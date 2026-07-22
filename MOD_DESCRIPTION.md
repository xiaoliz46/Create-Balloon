# Create Balloon
### Hot Air Balloons for Create × Create: Aeronautics

Take to the skies with fully functional hot air balloons built on the Sable physics engine. Inflate them, dye them any of 16 colors, pilot them with WASD controls, or automate them with ComputerCraft.

---

## 基本特色

**真实物理升力** — 每个气球为 Sable 结构提供升力，升力随海拔指数衰减（Y=62 以上），模拟大气压力变化。多个气球共享负载，首个放置的气球作为主控，负责悬停、阻力和陀螺仪计算。

**WASD 操控** — Space 上升、V 下降、W 前进、S 后退、A 左转、D 右转、Shift 退出。控制器落地后 1 秒自动解除控制。

**16 色染色** — 右键染料单气球染色，双击周围染色，三击 17×17×17 范围批量染色。合成台用染料直接合成彩色气球，Create 压榨机可把彩色气球压回白色信封。

**PD 陀螺仪稳定** — 飞行中自动保持结构水平。大力结构运行平稳；小质量结构角冲量自动上限（mass × 0.5），防止数值正反馈炸飞。如果结构上装有 Aeroworks 陀螺仪方块，本模组自动让位。

**CC:Tweaked 全功能** — 原生全局 API（`balloon.ascend()` 直接调用）和外设 API（`peripheral.find("balloon")`）同时支持。11 个 Lua 函数覆盖升降、移动、转向、停止、计数、状态查询和高度读取。配合 GPS 主机可实现全自动导航。

**Debug 日志** — 可配置的调试日志，记录物理数据、控制器输入、气球状态变化和 CC API 调用。Time 提供完整版（保留 5000 条的 latest 尾视图）与节流版（PHY 每秒 1 次）。完整会话日志用可读时间戳归档且永不删除。

**红石与 Drive-By-Wire** — 控制器左右两侧接收红石信号触发升降。Drive-By-Wire 安装时自动读取其 ascend/descend 信号。

**12 种语言** — 英语（US/UK）、俄语、印地语、德语、法语、西班牙语、葡萄牙语（巴西）、日语、韩语、简体中文、繁体中文。部分为机器翻译，欢迎贡献。

---

## 新增方块及配方

### 热气球方块
- 16 种染料颜色变体，默认白色。
- 放置后成为 Sable 升力提供者。
- 与 Aeronautics 信封方块功能兼容（标记为 `aeronautics:envelope` 和 `aeronautics:airtight`）。
- 合成方式：
  - **基础**：Aeronautics 信封 → 白色气球（合成台）
  - **染色**：白色气球 + 任意染料 → 对应颜色气球（合成台）
  - **批量染色**：手持染料右键气球（单击单球、双击相邻、三击 17³ 范围）
  - **Create 压榨**：彩色气球 → 白色信封（压榨机）
  - **Create 洗涤**：彩色气球 + 水 → 白色气球（溅水/洗涤）
  - **Create 部署**：水桶/水瓶 + 信封 → 白色气球（部署器）
  - **循环**：共 97 个配方覆盖所有颜色和机械处理

### 操控台方块
- 放置于 Sable 结构上用于驾驶。
- 右键接管控制，再次右键或 Shift 退出。
- 接收红石信号：左侧下降、右侧上升（相对于放置朝向）。
- 合成：白色船帆 + 传动杆 + 黄铜板（合成台）

### 彩色气球物品
- 创造模式物品栏中所有 16 种颜色单独列出，堆叠独立。
- 放置时自动设置对应颜色方块状态。
- 语言文件按颜色生成独立翻译键。

---

## 新增配置

配置文件 `config/create_balloon-common.toml`，修改即时生效，无需重启。

### 升力
| 配置项 | 默认值 | 范围 | 说明 |
|--------|--------|------|------|
| `liftForce` | 100.0 | 1~10000 | 每气球升力容量（kpg） |
| `baseKpg` | 0.2597 | 0~100 | 重力补偿值，针对 Sable 物理实证调校 |
| `ascendSpeed` | 1.0 | 0.1~5.0 | 上升速度加成 |
| `descendSpeed` | 1.0 | 0.1~5.0 | 下降速度加成 |

### 陀螺仪
| 配置项 | 默认值 | 范围 | 说明 |
|--------|--------|------|------|
| `gyroEnabled` | true | — | 启用陀螺仪稳定 |
| `gyroStrength` | 3.0 | 0.1~5.0 | 稳定力度，>2.0 可能导致震荡 |

### 移动
| 配置项 | 默认值 | 范围 | 说明 |
|--------|--------|------|------|
| `moveSpeed` | 1.0 | 0~5.0 | 水平移动速度 |
| `turnSpeed` | 0.5 | 0~5.0 | 转向速度，>1.0 可能导致不可控旋转 |

### 调试
| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `logLevel` | SIMPLE | OFF=关闭 / SIMPLE=简要 / DETAILED=完整物理数据 |

---

## 可联动模组

| 模组 | 版本 | 联动内容 |
|------|------|----------|
| **CC:Tweaked** | 1.0+ | 原生 API + 外设 API，Lua 脚本全自动控制气球 |
| **Drive-By-Wire** | 0.2.9+ | 无线信号直接触发 ascend/descend |
| **Aeroworks** | 1.2.10+ | 结构上有 Aeroworks 陀螺仪时本模组陀螺仪自动停用，避免冲突 |

---

## 日志格式

日志目录 `create-balloon-logs/`。

### 文件说明
- `latest-debug.log` — 当前会话尾视图，保留最近 5000 条
- `latest-debug-throttled.log` — 同上，PHY 每秒 1 条
- `debug-YYYY-MM-DD_HH-MM-SS.log` — 完整会话归档（永不删除）

### 条目格式
```
[HH:MM:SS.mmm] #序号 类型 数据...
```

### 条目类型
- `PHY` — 物理 tick 数据（质量、速度、力、位置）
- `CTRL` — 控制器输入事件（升/降/前/后/左/右）
- `BALLOON` — 气球状态变化（悬停开关）
- `[CC:ID]` — CC:Tweaked API 调用日志

---

## 快速开始

1. 用白色船帆 + 传动杆 + 黄铜板合成操控台
2. 将 Aeronautics 信封放入合成台获得白色气球方块（或用 Create 部署器）
3. 用染料给气球染色（可选）
4. 用 Create: Aeronautics 将方块组装为 Sable 物理结构
5. 把操控台放在结构上
6. 右键操控台开始驾驶：Space 上升、V 下降、WASD 移动转向、Shift 退出
7. 可选：在结构上放置 CC:Tweaked 电脑，运行 Lua 脚本实现自动化

---

## 致谢

- **mred231** — Create: Aeroworks 陀螺仪代码（MIT 许可，本模组陀螺仪适配自 Aeroworks v1.2.10）
- **Create-Balloon Team** — 模组开发
- 所有测试者和语言贡献者

---

## 示例代码

### 基本飞行
```lua
balloon.ascend()
sleep(10)
balloon.hover()
sleep(5)
balloon.descend()
sleep(10)
balloon.stop()
```

### 自动悬停（指定高度）
```lua
local targetY = 80
while true do
    local x, y, z = gps.locate(5)
    if y == nil then
        balloon.hover()
    elseif y < targetY - 3 then
        balloon.ascend()
    elseif y > targetY + 3 then
        balloon.descend()
    else
        balloon.hover()
    end
    sleep(1)
end
```

### GPS 自动导航
```lua
local tx, ty, tz = 1000, 80, 2000  -- 目标坐标
while true do
    local x, y, z = gps.locate(3)
    if not x then
        balloon.hover()
        sleep(1)
    else
        local dy = ty - y
        local dist = math.sqrt((tx - x)^2 + (tz - z)^2)
        if math.abs(dy) < 2 and dist < 2 then
            balloon.stop()
            break
        end
        if dy > 3 then balloon.ascend()
        elseif dy < -3 then balloon.descend()
        else balloon.forward() end
    end
    sleep(0.5)
end
```

### API 全功能测试
```lua
print("Balloons: " .. balloon.count() .. "  Height: " .. balloon.getHeight())
balloon.ascend();  sleep(3)
balloon.hover();   sleep(3)
balloon.descend(); sleep(3)
balloon.forward(); sleep(2); balloon.hover()
balloon.back();    sleep(2); balloon.hover()
balloon.turnLeft();  sleep(2); balloon.hover()
balloon.turnRight(); sleep(2); balloon.hover()
balloon.stop()
print("Done. Active: " .. tostring(balloon.isActive()))
```
