package com.createballoon.client;

import com.createballoon.ModConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class GuiConfigScreen extends Screen {

    private static final int SIDEBAR_W = 90, GAP = 6, ROW_H = 22, SPACE = 4;
    private static final int BG = 0x660A0A0A, SIDEBAR_BG = 0x66121212;
    private static final int ACCENT = 0xFF4A90D9, HOVER_ROW = 0x1AFFFFFF;
    private static final int BTN_W = 60, BTN_H = 18;

    private static final String[] CATEGORIES = {"升降", "移动", "陀螺仪", "其他"};
    private int selected, hoveredRow = -1;
    private boolean dragging, dirty, editing;
    private String editBuf = "";
    private int editIdx = -1;
    private int logScroll, logHScroll;
    private double animHover = -1, animCat = -1;

    private final ConfigItem[] liftItems, moveItems, gyroItems, otherItems;

    public GuiConfigScreen() {
        super(Component.literal("配置"));
        liftItems = new ConfigItem[]{
            ci("升力容量", "结构总质量超出总升力时不可起飞。调大允许更重结构。", 0, 10000, 100, ModConfigs.LIFT_FORCE),
            ci("上升速度", "按住空格时控制器将结构推到此垂直速度。", 0, 100, 5, ModConfigs.ASCEND_SPEED),
            ci("下降速度", "按住下降键时控制器将结构压到此下沉速度。", 0, 100, 5, ModConfigs.DESCEND_SPEED),
        };
        moveItems = new ConfigItem[]{
            ci("前进速度", "按住 W 时结构在此方向加速到此水平速度。", 0, 100, 10, ModConfigs.FORWARD_SPEED),
            ci("后退速度", "按住 S 时结构在此方向加速到此水平速度。", 0, 100, 10, ModConfigs.BACKWARD_SPEED),
            ci("转向速度", "按住 A/D 时结构绕世界Y轴旋转到此角速度(°/s)。", 0, 360, 30, ModConfigs.TURN_SPEED),
        };
        var gyroOn = new ConfigItem("陀螺仪开关", "关闭后陀螺不运行，结构不自动回正。", 0, 1, 1, true) {
            public double getRaw() { return ModConfigs.GYRO_ENABLED.get() ? 1 : 0; }
            public void setRaw(double v) { ModConfigs.GYRO_ENABLED.set(v > 0.5); }
        };
        gyroItems = new ConfigItem[]{
            gyroOn,
            ci("整体强度", "≤10 倍乘增益；>10 启用多虚拟陀螺仪舰队模式。", 0, 10010, 10, ModConfigs.GYRO_STRENGTH),
            ci("自然频率 ω", "陀螺响应速度(rad/s)。越大修正越快但可能振荡。", 0.1, 10, 3.0, ModConfigs.GYRO_OMEGA),
            ci("阻尼比 ζ", "0.6=弹性, 1.0=临界阻尼。值小回正猛，值大更平稳。", 0.1, 2, 0.6, ModConfigs.GYRO_DAMPING),
            ci("陀螺容量", "每个陀螺的角动量容量(kg·m²)。容量÷惯量=有效增益。", 10, 100000, 5000, ModConfigs.GYRO_AUTHORITY),
            ci("偏航阻尼", "抑制结构自发的绕Y轴旋转。玩家A/D转向时自动暂停。0=关闭。", 0, 50, 0.8, ModConfigs.GYRO_YAW_DAMPING),
        };
        otherItems = new ConfigItem[]{
            new ConfigItem("日志级别", "0=关闭日志 1=简要 (GYRO+LIFT) 2=详细 3=诊断 (含DIAG朝向)", 0, 3, 1, false) {
                public double getRaw() { return ModConfigs.LOG_LEVEL.get().ordinal(); }
                public void setRaw(double v) { var vals = ModConfigs.LogLevel.values(); ModConfigs.LOG_LEVEL.set(vals[Math.max(0, Math.min(vals.length - 1, (int)v))]); }
            },
        };
    }

    @Override protected void init() { for (var a : allArrays()) for (var it : a) it.snapshot(); }
    private ConfigItem[][] allArrays() { return new ConfigItem[][]{liftItems, moveItems, gyroItems, otherItems}; }

    @Override public void onClose() { if (dirty) minecraft.setScreen(new ConfirmScreen(this)); else doExit(); }
    @Override public boolean isPauseScreen() { return false; }

    private void doSave() { for (var a : allArrays()) for (var it : a) it.apply(); ModConfigs.SPEC.save(); dirty = false; }
    private void doExit() { minecraft.setScreen(null); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int sx = 20, cx = sx + SIDEBAR_W + GAP;
        int top = 20, bottom = height - 10, cr = width - 30;

        // Smooth category animation
        double targetCat = selected;
        animCat += (targetCat - animCat) * 0.25;

        // Backdrop
        g.fill(0, 0, width, height, 0x66000000);
        g.fill(sx, top, width - 20, bottom, BG);
        g.fill(sx, top, sx + SIDEBAR_W, bottom, SIDEBAR_BG);

        // Sidebar
        for (int i = 0; i < CATEGORIES.length; i++) {
            int sy = top + 16 + i * (ROW_H + SPACE + 4);
            int col = i == selected ? ACCENT : 0xFF888888;
            g.drawString(font, CATEGORIES[i], sx + 10, sy + 4, col);
        }
        g.fill(sx + SIDEBAR_W, top, sx + SIDEBAR_W + 1, bottom, 0x33FFFFFF);

        // Content
        ConfigItem[] items = currentItems();
        hoveredRow = -1;
        if (animHover < 0) animHover = -1;
        double targetHover = -1;

        for (int i = 0; i < items.length; i++) {
            int rowY = top + 16 + i * (ROW_H + SPACE);
            boolean hover = mx >= cx && mx <= cr && my >= rowY && my <= rowY + ROW_H;
            if (hover) { hoveredRow = i; targetHover = i; }

            double dist = Math.abs(i - animHover);
            int alpha = hover ? (int)(0x22 + (1.0 - Math.min(1, Math.abs(i - animHover))) * 0x11) : 0;
            if (alpha > 0) g.fill(cx, rowY, cr, rowY + ROW_H, alpha << 24 | 0xFFFFFF);
            g.drawString(font, items[i].name, cx + 8, rowY + 5, 0xFFCCCCCC);

            double val = items[i].get();
            int sliderX = cx + 150, sliderW = cr - sliderX - 74;
            int resetX = cr - 20;

            // Reset button "↺"
            boolean overReset = mx >= resetX && mx <= cr - 2 && my >= rowY && my <= rowY + ROW_H;
            int rc = overReset ? ACCENT : 0xFF666666;
            g.drawString(font, "↺", resetX, rowY + 5, rc);

            if (items[i].isBool) {
                g.drawString(font, val > 0.5 ? "■ 开" : "□ 关", sliderX, rowY + 5, val > 0.5 ? 0xFF55FF55 : 0xFFFF5555);
            } else {
                String vs = editing && editIdx == i ? editBuf + "▊" :
                        (items[i].min == 0 && items[i].max <= 10 && (items[i].max - (int)items[i].max) < 0.001) ? String.format("%.0f", val)
                        : String.format("%.1f", val);
                int vx = sliderX + sliderW + 4;
                g.drawString(font, vs, vx, rowY + 5, 0xFFEEEEEE);
                int trackY = rowY + ROW_H / 2;
                g.fill(sliderX, trackY, sliderX + sliderW, trackY + 1, 0x44FFFFFF);
                double frac = (val - items[i].min) / (items[i].max - items[i].min);
                int fillX = sliderX + (int)(sliderW * frac);
                g.fill(sliderX, trackY, fillX, trackY + 1, ACCENT);
                int r = 3;
                g.fill(fillX - r, trackY - r, fillX + r, trackY + r, ACCENT);
                if (editing && editIdx == i)
                    g.fill(vx - 2, rowY, vx + font.width(vs) + 2, rowY + ROW_H, 0x33FFFFFF);
            }

            if (hover && items[i].desc != null) {
                int tw = font.width(items[i].desc);
                g.fill(mx + 10, my - 18, mx + tw + 22, my, 0xEE181818);
                g.drawString(font, items[i].desc, mx + 16, my - 15, 0xFFDDDDDD);
            }
        }
        animHover += (targetHover - animHover) * 0.25;

        // Log viewer ("其他" tab)
        if (selected == 3) {
            int logX = cx, logTop = top + 16 + (otherItems.length + 1) * (ROW_H + SPACE);
            int logH = bottom - logTop - 30;
            if (logH > 40) {
                g.fill(logX, logTop, cr, logTop + logH, 0xAA0E0E0E);
                g.drawString(font, "实时日志", logX + 6, logTop + 4, 0xFF666666);
                String path = com.createballoon.DebugLog.getLogPath();
                if (path != null) try {
                    var lines = java.nio.file.Files.readAllLines(java.nio.file.Path.of(path));
                    int totalLines = Math.min(1000, lines.size());
                    int maxVis = (logH - 28) / 12;
                    int start = Math.max(0, totalLines - maxVis - logScroll);
                    start = Math.min(start, Math.max(0, totalLines - maxVis));
                    for (int j = 0; j < Math.min(maxVis, totalLines - start); j++) {
                        String line = lines.get(lines.size() - 1 - start - j);
                        if (logHScroll > 0 && line.length() > logHScroll) line = line.substring(logHScroll);
                        g.drawString(font, line, logX + 6, logTop + 20 + j * 12, 0xFF777777);
                    }
                    if (totalLines > maxVis) {
                        int sbX = cr - 8, sbH = logH - 8;
                        g.fill(sbX, logTop + 3, sbX + 3, logTop + sbH, 0x33FFFFFF);
                        int thumbH = Math.max(8, sbH * maxVis / totalLines);
                        int thumbY = logTop + 3 + Math.max(0, Math.min(sbH - thumbH, (sbH - thumbH) * logScroll / Math.max(1, totalLines - maxVis)));
                        g.fill(sbX, thumbY, sbX + 3, thumbY + thumbH, 0x88FFFFFF);
                    }
                    // Horizontal scrollbar
                    int hW = cr - logX - 12;
                    g.fill(logX, logTop + logH - 6, cr - 8, logTop + logH - 3, 0x33FFFFFF);
                    if (logHScroll > 0) {
                        int thumbW = Math.max(20, hW * hW / (hW + logHScroll * 6));
                        g.fill(logX + Math.min(hW - thumbW, logHScroll * hW / Math.max(1, logHScroll + hW)),
                               logTop + logH - 6, logX + Math.min(hW, logHScroll * hW / Math.max(1, logHScroll + hW) + thumbW),
                               logTop + logH - 3, 0x88FFFFFF);
                    }
                } catch (Exception ignored) {}
            }
        }

        // Save / Exit buttons (inside panel)
        int btnY = bottom - 24;
        g.fill(cx, btnY - 2, cr, bottom, 0x66181818);
        boolean overSave = mx >= cr - BTN_W * 2 - 20 && mx <= cr - BTN_W - 20 && my >= btnY && my <= btnY + BTN_H;
        boolean overExit = mx >= cr - BTN_W - 8 && mx <= cr - 8 && my >= btnY && my <= btnY + BTN_H;
        g.drawString(font, "保存", cr - BTN_W * 2 - 16, btnY + 3, overSave ? ACCENT : 0xFF888888);
        g.drawString(font, "退出", cr - BTN_W - 4, btnY + 3, overExit ? ACCENT : 0xFF888888);
        if (dirty) g.drawString(font, "●", cr - BTN_W * 2 - 28, btnY + 3, 0xFFFFAA00);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            int sx = 20, top = 20, cr = width - 30;
            for (int i = 0; i < CATEGORIES.length; i++) {
                int ry = top + 16 + i * (ROW_H + SPACE + 4);
                if (mx >= sx && mx <= sx + SIDEBAR_W && my >= ry && my <= ry + ROW_H) { selected = i; return true; }
            }
            // Save/Exit
            int btnY = height - 10 - 24;
            if (my >= btnY && my <= btnY + BTN_H) {
                if (mx >= cr - BTN_W * 2 - 20 && mx <= cr - BTN_W - 20) {
                    if (editing) { editing = false; editIdx = -1; }
                    doSave(); return true;
                }
                if (mx >= cr - BTN_W - 8 && mx <= cr - 8) {
                    if (editing) { editing = false; editIdx = -1; }
                    onClose(); return true;
                }
            }
            // Value edit
            int cx = sx + SIDEBAR_W + GAP, sliderX = cx + 150;
            int sliderW = cr - sliderX - 74, vx = sliderX + sliderW + 4, resetX = cr - 20;
            if (editing && editIdx >= 0) { editing = false; editIdx = -1; }
            if (hoveredRow >= 0) {
                ConfigItem[] items = currentItems(); ConfigItem item = items[hoveredRow];
                // Reset button -> restore mod default
                if (mx >= resetX && mx <= cr - 2) { item.resetToDefault(); dirty = true; return true; }
                if (!item.isBool && mx >= vx && mx <= vx + 60) {
                    editing = true; editIdx = hoveredRow;
                    editBuf = item.min >= 10 ? String.format("%.0f", item.get()) : String.format("%.1f", item.get());
                    return true;
                }
                if (item.isBool) { item.set(item.get() > 0.5 ? 0 : 1); dirty = true; return true; }
                if (mx >= sliderX && mx <= sliderX + sliderW) { dragging = true; applySlider(mx); return true; }
            }
            // Log scrollbar
            if (selected == 3) {
                int sbX = cr - 4, logTop = top + 16 + (otherItems.length + 1) * (ROW_H + SPACE);
                int logH = (height - 25) - logTop - 8;
                if (mx >= sbX && mx <= sbX + 6 && my >= logTop && my <= logTop + logH) {
                    String path = com.createballoon.DebugLog.getLogPath();
                    if (path != null) try {
                        int totalLines = Math.min(200, java.nio.file.Files.readAllLines(java.nio.file.Path.of(path)).size());
                        int maxVis = (logH - 20) / 12;
                        double frac = (my - logTop) / logH;
                        logScroll = Math.max(0, Math.min(totalLines - maxVis, (int)(frac * (totalLines - maxVis))));
                    } catch (Exception ignored) {}
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging && hoveredRow >= 0) { applySlider(mx); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int cx = 20 + SIDEBAR_W + GAP, sliderX = cx + 150, cr = width - 30;
        int sliderW = cr - sliderX - 74;
        if (selected == 3) {
            int logTop = 20 + 16 + (otherItems.length + 1) * (ROW_H + SPACE);
            int logH = (height - 10) - logTop - 30;
            if (mx >= cx && mx <= cr - 8 && my >= logTop && my <= logTop + logH) {
                if (org.lwjgl.glfw.GLFW.glfwGetKey(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
                    logHScroll = Math.max(0, logHScroll - (int)sy * 10);
                } else {
                    logScroll = Math.max(0, logScroll - (int)sy);
                }
                return true;
            }
        }
        if (mx >= sliderX && mx <= sliderX + sliderW && hoveredRow >= 0) {
            ConfigItem[] items = currentItems();
            if (hoveredRow < items.length && !items[hoveredRow].isBool) {
                double step = (items[hoveredRow].max - items[hoveredRow].min) / 100.0;
                double v = Math.max(items[hoveredRow].min, Math.min(items[hoveredRow].max, items[hoveredRow].get() + step * sy));
                items[hoveredRow].set(v); dirty = true;
            }
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override public boolean mouseReleased(double mx, double my, int btn) { dragging = false; return super.mouseReleased(mx, my, btn); }

    private void applySlider(double mx) {
        ConfigItem[] items = currentItems();
        if (hoveredRow < 0 || hoveredRow >= items.length || items[hoveredRow].isBool) return;
        int cx = 20 + SIDEBAR_W + GAP, sliderX = cx + 150;
        int sliderW = (width - 30) - sliderX - 74;
        double frac = Math.max(0, Math.min(1, (mx - sliderX) / sliderW));
        items[hoveredRow].set(items[hoveredRow].min + frac * (items[hoveredRow].max - items[hoveredRow].min));
        dirty = true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (editing && editIdx >= 0) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                if (key == GLFW.GLFW_KEY_ENTER) try {
                    double v = Double.parseDouble(editBuf);
                    ConfigItem[] it = currentItems();
                    v = Math.max(it[editIdx].min, Math.min(it[editIdx].max, v));
                    it[editIdx].set(v); dirty = true;
                } catch (NumberFormatException ignored) {}
                editing = false; editIdx = -1; return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !editBuf.isEmpty()) { editBuf = editBuf.substring(0, editBuf.length() - 1); return true; }
            if (key == GLFW.GLFW_KEY_MINUS) { editBuf = "-" + editBuf; return true; }
            if (key == GLFW.GLFW_KEY_PERIOD && !editBuf.contains(".")) { editBuf += "."; return true; }
            if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) { editBuf += (char)(key - GLFW.GLFW_KEY_0 + '0'); return true; }
            return false;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        // Block E when V is held
        if (key == GLFW.GLFW_KEY_E) {
            long w = minecraft.getWindow().getWindow();
            if (GLFW.glfwGetKey(w, GLFW.GLFW_KEY_V) == GLFW.GLFW_PRESS) return true;
        }
        return super.keyPressed(key, scan, mod);
    }

    private ConfigItem[] currentItems() { return switch (selected) { case 0 -> liftItems; case 1 -> moveItems; case 2 -> gyroItems; default -> otherItems; }; }

    private static ConfigItem ci(String n, String d, double min, double max, double def, net.neoforged.neoforge.common.ModConfigSpec.DoubleValue dv) {
        return new ConfigItem(n, d, min, max, def, false) {
            public double getRaw() { return dv.get(); } public void setRaw(double v) { dv.set(v); }
        };
    }

    private abstract static class ConfigItem {
        final String name, desc; final double min, max, defaultVal; boolean isBool; private double original;
        ConfigItem(String n, String d, double min, double max, double def, boolean b) { name = n; desc = d; this.min = min; this.max = max; this.defaultVal = def; isBool = b; }
        abstract double getRaw(); abstract void setRaw(double v);
        double get() { return original; }
        void set(double v) { original = Math.max(min, Math.min(max, v)); }
        void snapshot() { original = getRaw(); }
        void apply() { setRaw(original); }
        void resetToDefault() { original = defaultVal; }
    }

    static class ConfirmScreen extends Screen {
        private final Screen parent;
        ConfirmScreen(Screen p) { super(p.getTitle()); this.parent = p; }
        @Override public void render(GuiGraphics g, int mx, int my, float pt) {
            parent.render(g, -1, -1, pt);
            g.fill(0, 0, width, height, 0x44000000);
            int cx = width / 2, cy = height / 2;
            g.fill(cx - 110, cy - 28, cx + 110, cy + 28, 0xCC181818);
            g.drawCenteredString(font, "是否保存配置？", cx, cy - 12, 0xFFFFFFFF);
            int sc = mx >= cx - 55 && mx <= cx - 5 && my >= cy + 4 && my <= cy + 20 ? ACCENT : 0xFF888888;
            int ec = mx >= cx + 5 && mx <= cx + 55 && my >= cy + 4 && my <= cy + 20 ? ACCENT : 0xFF888888;
            g.drawString(font, "保存", cx - 52, cy + 8, sc);
            g.drawString(font, "不保存", cx + 15, cy + 8, ec);
        }
        @Override public boolean mouseClicked(double mx, double my, int btn) {
            if (btn == 0) {
                int cx = width / 2, cy = height / 2;
                if (mx >= cx - 55 && mx <= cx - 5 && my >= cy + 4 && my <= cy + 20) {
                    ((GuiConfigScreen)parent).doSave(); ((GuiConfigScreen)parent).doExit(); return true;
                }
                if (mx >= cx + 5 && mx <= cx + 55 && my >= cy + 4 && my <= cy + 20) {
                    ((GuiConfigScreen)parent).doExit(); return true;
                }
                minecraft.setScreen(parent); return true;
            }
            return super.mouseClicked(mx, my, btn);
        }
        @Override public boolean keyPressed(int key, int scan, int mod) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { ((GuiConfigScreen)parent).doExit(); return true; }
            return super.keyPressed(key, scan, mod);
        }
    }
}
