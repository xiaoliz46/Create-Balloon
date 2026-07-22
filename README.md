# Create Balloon (机械动力：气球)

> All content in this repository is AI-generated. | 本仓库所有内容为AI制作。

[English](#english) | [中文](#中文)

---

## English

Adds inflatable hot air balloons to Create: Aeronautics. One block = one balloon envelope.

### Blocks

| Block | Description |
|-------|-------------|
| **Inflatable Hot Air Balloon** | 100 kpg lift each, dyeable in 16 colors. Aeronautics envelope compatible. Recipe: 1× Aeronautics Envelope |
| **Balloon Controller** | Right-click to pilot. Recipe: Sail + Brass Casing |

### Controls

| Action | Effect |
|--------|--------|
| Right-click controller (empty hand) | Lock controls |
| Space | Ascend (only when total lift > weight) |
| V | Descend |
| Release all keys | Auto-hover (PI controller) |
| Shift | Exit control (hover maintained) |

### Redstone

| Signal | Effect |
|--------|--------|
| West side powered | Ascend |
| East side powered | Descend |
| No signal | Hover |
| Both sides powered | No effect |

### Dyeing

Craft balloon + any dye → colored balloon (16 colors). All textures referenced from Aeronautics, no custom assets.

### Config

`liftForce` in `config/create_balloon-common.toml` — lift capacity per balloon in kpg (default 100, range 1~10000). Change in-game via Mods menu.

### Dependencies

- Minecraft 1.21.1
- NeoForge 21.1.226+
- Create 6.0.10+
- Create: Aeronautics Bundled 1.1.3+
- Sable 1.1.3+

### License

MIT

---

## 中文

为机械动力航空学添加充气热气球。一个方块就是一个气囊。

### 内容

| 方块 | 说明 |
|------|------|
| **充气热气球** | 100 kpg 升力/个，16色可染色，航空学气囊兼容。合成：1×航空学信封 |
| **热气球控制器** | 右键锁定操控。合成：风帆 + 黄铜机壳 |

### 操控

| 操作 | 效果 |
|------|------|
| 右键控制器（空手） | 锁定操控 |
| 空格 | 上升（总升力 > 自重时生效） |
| V | 下降 |
| 松开所有按键 | 自动悬停（PI 控制器） |
| Shift | 退出操控（保持悬停） |

### 红石控制

| 信号 | 效果 |
|------|------|
| 西侧通入 | 上升 |
| 东侧通入 | 下降 |
| 无信号 | 悬停 |
| 两侧同时通入 | 无效 |

### 染色

气球 + 任意染料 → 对应颜色（16色）。全部贴图引用航空学材质，无自定义贴图。

### 配置

`config/create_balloon-common.toml` 中 `liftForce` — 每气球升力（kpg），默认 100，范围 1~10000。游戏中 Mods 菜单可调。

### 依赖

- Minecraft 1.21.1
- NeoForge 21.1.226+
- Create 6.0.10+
- Create: Aeronautics Bundled 1.1.3+
- Sable 1.1.3+

### 许可

MIT
