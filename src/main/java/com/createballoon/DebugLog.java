package com.createballoon;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DebugLog {
    private static final int TAIL_LINES = 5000;

    private static PrintWriter fullWriter;

    public static String getLogPath() {
        return new File("create-balloon-logs", "latest-debug.log").getAbsolutePath();
    }
    private static int lineNum;

    private static final RingBuf latestBuf = new RingBuf("latest-debug.log");
    private static final RingBuf throttledBuf = new RingBuf("latest-debug-throttled.log");
    private static long lastPhyMs;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static class RingBuf {
        final String[] buf = new String[TAIL_LINES];
        final String filename;
        int idx;
        int count;
        int flushCounter;

        RingBuf(String filename) { this.filename = filename; }

        void add(String s) {
            buf[idx % TAIL_LINES] = s;
            idx++;
            if (count < TAIL_LINES) count++;
            if (++flushCounter >= 200) { flushCounter = 0; flush(); }
        }

        void flush() {
            File dir = new File("create-balloon-logs");
            dir.mkdirs();
            try (PrintWriter pw = new PrintWriter(new FileWriter(new File(dir, filename)), true)) {
                int start = count < TAIL_LINES ? 0 : idx % TAIL_LINES;
                for (int i = 0; i < count; i++) {
                    String s = buf[(start + i) % TAIL_LINES];
                    if (s != null) pw.print(s);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void init() {
        try {
            File dir = new File("create-balloon-logs");
            dir.mkdirs();
            fullWriter = new PrintWriter(new FileWriter(new File(dir, "debug-" + LocalDateTime.now().format(FILE_FMT) + ".log")), true);
            fullWriter.println("============================================================");
            fullWriter.println("  Create Balloon Debug Log  (full)");
            fullWriter.println("============================================================");
            fullWriter.println();
        } catch (Exception e) {
            System.err.println("CreateBalloon: Failed to init debug log: " + e.getMessage());
        }
    }

    public static synchronized void log(String format, Object... args) {
        if (fullWriter == null) return;
        if (ModConfigs.LOG_LEVEL.get() == ModConfigs.LogLevel.OFF) return;

        String entry = String.format("[%s] #%d %s%n",
                LocalDateTime.now().format(FMT), ++lineNum, String.format(format, args));

        fullWriter.print(entry);
        if (fullWriter.checkError()) fullWriter = null;

        latestBuf.add(entry);

        if (format.startsWith("PHY")) {
            long now = System.currentTimeMillis();
            if (now - lastPhyMs < 1000) return;
            lastPhyMs = now;
        }
        throttledBuf.add(entry);
    }

    public static synchronized void close() {
        if (fullWriter != null) { fullWriter.close(); fullWriter = null; }
        latestBuf.flush();
        throttledBuf.flush();
    }
}
