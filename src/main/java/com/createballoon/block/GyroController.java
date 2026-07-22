package com.createballoon.block;

import com.createballoon.DebugLog;
import com.createballoon.ModConfigs;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Matrix3dc;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import java.util.Map;

public final class GyroController {

    private static final Vector3d REFERENCE_UP = new Vector3d(0, 1, 0);
    private static final double FRESH_MARKER = -1.0;
    private static final Map<ServerSubLevel, GyroController> POOL = new java.util.WeakHashMap<>();

    static GyroController of(ServerSubLevel ssl) { return POOL.computeIfAbsent(ssl, GyroController::new); }
    static void detach(ServerSubLevel ssl) { POOL.remove(ssl); }
    public static void clearAll() { POOL.clear(); }

    private final ServerSubLevel subLevel;

    private final Vector3d bodyUp         = new Vector3d();
    private final Vector3d errorGlobal    = new Vector3d();
    private final Vector3d errorBody      = new Vector3d();
    private final Vector3d omegaWorld     = new Vector3d();
    private final Vector3d omegaBody      = new Vector3d();
    private final Vector3d upInBody       = new Vector3d();
    private final Vector3d torque         = new Vector3d();
    private final Vector3d dampTerm       = new Vector3d();
    private final Vector3d dOmegaBody     = new Vector3d();
    private final Vector3d omegaPrev      = new Vector3d();
    private final Vector3d torquePrev     = new Vector3d();
    private final Vector3d noiseRaw       = new Vector3d();
    private final Vector3d noiseFilt      = new Vector3d();
    private final Vector3d noiseDelta     = new Vector3d();
    private final Vector3d invUpGlobal    = new Vector3d();

    private final double[] tiltRing    = new double[6];
    private final double[] speedRing   = new double[6];
    private double ringPrevP = Double.NaN;
    private double ringPrevD = Double.NaN;
    private long ringTick;

    private double mark = FRESH_MARKER;
    private boolean warm;

    private double cachedAuthority, cachedFreq, cachedDamping;

    GyroController(ServerSubLevel ssl) { this.subLevel = ssl; }

    void tick(double partialTick, RigidBodyHandle handle, ServerSubLevel ssl, double dt) {
        if (partialTick == mark) return;
        mark = partialTick;

        MassData mass = ssl.getMassTracker();
        if (mass == null || mass.isInvalid()) { halt(); return; }

        int n = countGyroUnits();
        double s = ModConfigs.GYRO_STRENGTH.get();
        if (n > 0 && s > 1.0) n = (int)Math.min(10000, n * s);
        if (n <= 0 || !ModConfigs.GYRO_ENABLED.get()) { halt(); return; }

        double authority = n * ModConfigs.GYRO_AUTHORITY.get();
        double inertiaAvg = traceAvg(mass);
        double margin = inertiaAvg > 0 ? Math.min(1.0, authority / inertiaAvg) : 0;
        if (margin <= 0) { halt(); return; }

        cachedAuthority = authority;
        cachedFreq = ModConfigs.GYRO_OMEGA.get();
        cachedDamping = ModConfigs.GYRO_DAMPING.get();

        Matrix3dc J = mass.getInertiaTensor();
        Matrix3dc Jinv = mass.getInverseInertiaTensor();
        Quaterniondc q = ssl.logicalPose().orientation();

        // ---- snapshot body state ----
        handle.getAngularVelocity(omegaWorld);
        q.transformInverse(omegaWorld, omegaBody);
        double yawRate = omegaBody.y;
        omegaBody.y = 0;
        q.transformInverse(REFERENCE_UP, upInBody);

        // ---- disturbance estimation ----
        noiseRaw.zero();
        if (warm) {
            // α_observed = ω_current - ω_previous
            noiseDelta.set(omegaBody).sub(omegaPrev);
            // α_ours = J⁻¹ × τ_previous
            Jinv.transform(torquePrev, dOmegaBody);
            noiseDelta.sub(dOmegaBody);
            double mag = noiseDelta.length();
            if (mag > 1.0) noiseDelta.mul(1.0 / mag);
            // τ_noise = J × α_noise, then project off up axis
            J.transform(noiseDelta, noiseRaw);
            noiseRaw.fma(-noiseRaw.dot(upInBody), upInBody);
        }
        noiseFilt.lerp(noiseRaw, 0.2);

        // ---- tilt error ----
        q.transform(REFERENCE_UP, bodyUp);
        double cosA = bodyUp.dot(REFERENCE_UP);
        if (bodyUp.cross(REFERENCE_UP, errorGlobal).length() > 1e-9)
            errorGlobal.mul(clippedAcos(cosA) / errorGlobal.length());
        q.transformInverse(errorGlobal, errorBody);

        // ---- compute correction torque ----
        double angle = errorBody.length();
        double rate = omegaBody.length();
        torque.zero();

        if (s > 10) {
            int count = (int)Math.min(s, 10000);
            double[] pGains = new double[count];
            double[] dGains = new double[count];
            for (int i = 0; i < count; i++) {
                double var = 0.9 + 0.2 * ((i * 0.618034) % 1.0);
                pGains[i] = margin * cachedFreq * cachedFreq * var * var;
                dGains[i] = margin * 2.0 * cachedDamping * cachedFreq * var;
            }
            for (int i = 0; i < count; i++) {
                Vector3d p = new Vector3d(errorBody).mul(pGains[i] * dt);
                J.transform(p, p);
                Vector3d d = new Vector3d(omegaBody).mul(-dGains[i] * dt);
                J.transform(d, d);
                Jinv.transform(d, dOmegaBody);
                d.mul(saturation(omegaBody, dOmegaBody));
                torque.add(p).add(d);
            }
            torque.mul(1.0 / count);
        } else {
            double P = s * margin * cachedFreq * cachedFreq;
            double D = s * margin * 2.0 * cachedDamping * cachedFreq;

            torque.set(errorBody).mul(P * dt);
            J.transform(torque, torque);

            dampTerm.set(omegaBody).mul(-D * dt);
            J.transform(dampTerm, dampTerm);
            Jinv.transform(dampTerm, dOmegaBody);
            dampTerm.mul(saturation(omegaBody, dOmegaBody));
            torque.add(dampTerm);
        }

        // ---- nonlinear boost ----
        double scale = 1.0 + 3.0 * angle * angle / (Math.PI * Math.PI);
        torque.mul(scale);

        // ---- disturbance feedforward ----
        if (warm) torque.fma(-margin * 0.3, noiseFilt);

        // ---- torque alignment (ensure no yaw injection from inertia coupling) ----
        Jinv.transform(REFERENCE_UP, invUpGlobal);
        if (Math.abs(invUpGlobal.y) > 0.001)
            torque.y = -(torque.x * invUpGlobal.x + torque.z * invUpGlobal.z) / invUpGlobal.y;

        handle.applyTorqueImpulse(torque);

        // ---- yaw kill ----
        if (Math.abs(yawRate) > 0.01 && !pilotTurning()) {
            double damp = s * margin * 2.0 * cachedDamping * cachedFreq * ModConfigs.GYRO_YAW_DAMPING.get() * yawRate * dt;
            double maxDamp = inertiaAvg * Math.abs(yawRate) * 0.3;
            if (Math.abs(damp) > maxDamp) damp = Math.signum(yawRate) * maxDamp;
            handle.applyTorqueImpulse(new Vector3d(0, -damp, 0));
        }
        omegaPrev.set(omegaBody);
        torquePrev.set(torque);
        warm = true;

        // ---- logging ----
        var lvl = ModConfigs.LOG_LEVEL.get();
        if (lvl != ModConfigs.LogLevel.SIMPLE && lvl != ModConfigs.LogLevel.DETAILED && lvl != ModConfigs.LogLevel.DIAGNOSTIC) return;

        double tiltDeg = Math.toDegrees(clippedAcos(upInBody.y));
        if (lvl == ModConfigs.LogLevel.DIAGNOSTIC) {
            ringLog(tiltDeg, rate, q);
        } else {
            DebugLog.log("GYRO t=%.1f\u00b0 imp=%.1f |P|=%.3f |D|=%.3f |FF|=%.1f kp=%.0f kd=%.0f sc=%.2f I=%.0f n=%d cap=%.0f",
                    tiltDeg, torque.length(), angle, rate, noiseFilt.length(),
                    s * margin * cachedFreq * cachedFreq * inertiaAvg,
                    s * margin * 2.0 * cachedDamping * cachedFreq * inertiaAvg,
                    margin, inertiaAvg, n, authority);
        }
    }

    private void ringLog(double tiltDeg, double rate, Quaterniondc q) {
        tiltRing[(int)(ringTick % 6)] = tiltDeg;
        speedRing[(int)(ringTick % 6)] = rate;
        if (ringTick > 0 && ringTick % 10 == 0)
            DebugLog.log("DIAG tiltHist[%.0f %.0f %.0f %.0f %.0f %.0f]",
                tiltRing[0], tiltRing[1], tiltRing[2], tiltRing[3], tiltRing[4], tiltRing[5]);
        if (!Double.isNaN(ringPrevP)) {
            double yawDeg = Math.toDegrees(Math.atan2(
                2 * (q.w() * q.y() + q.x() * q.z()),
                1 - 2 * (q.y() * q.y() + q.z() * q.z())));
            DebugLog.log("DIAG tilt=%.1f yawD=%.1f pd=%.3f |P|=%.3f |D|=%.3f |FF|=%.1f yawRaw=%.2f",
                tiltDeg, yawDeg, rate > 1e-9 ? errorBody.length() / rate : 0,
                errorBody.length(), rate, noiseFilt.length(),
                noiseFilt.length() - (Double.isNaN(ringPrevD) ? 0 : ringPrevD), omegaWorld.y);
        }
        ringPrevP = errorBody.length(); ringPrevD = rate;
        ringTick++;
    }

    private int countGyroUnits() {
        try {
            var plot = subLevel.getPlot();
            if (plot == null) return 0;
            for (var a : plot.getBlockEntityActors()) {
                if (a instanceof GyroscopeBlockEntity || a instanceof ControlConsoleBlockEntity) return 1;
            }
            return 0;
        } catch (Exception e) { return 0; }
    }

    private boolean pilotTurning() {
        try {
            var plot = subLevel.getPlot();
            if (plot == null) return false;
            for (var a : plot.getBlockEntityActors()) {
                if (a instanceof LiftProviderBE lbe && (lbe.isTurnLeft() || lbe.isTurnRight())) return true;
            }
            return false;
        } catch (Exception e) { return false; }
    }

    private static double traceAvg(MassData m) {
        Matrix3dc J = m.getInertiaTensor();
        return (J.m00() + J.m11() + J.m22()) / 3.0;
    }

    private static double clippedAcos(double x) { return Math.acos(Math.max(-1.0, Math.min(1.0, x))); }

    private static double saturation(Vector3dc cur, Vector3dc expect) {
        double a = -cur.dot(expect);
        double b = cur.lengthSquared();
        if (a <= 0) return 0;
        if (10.0 * a < b) return 1.0 - a / (2.0 * b);
        if (b < 1e-10) return b / (a + 1e-10);
        return b * (1.0 - Math.exp(-a / b)) / a;
    }

    private void halt() { noiseFilt.zero(); warm = false; }
}
