# EFMapArt

EFMapArt 是一个适用于 Minecraft Bukkit / Spigot / Paper 服务端的地图画插件。

插件可以将服务器文件夹中的图片转换成 Minecraft 地图画。玩家可以通过 GUI 选择图片和尺寸，普通玩家领取地图后自行摆放，管理员可以一键自动生成完整地图画墙。

## 插件特点

- 全部 GUI 化操作
- 支持普通玩家领取地图画物品
- 支持 OP / 管理员全自动生成地图画墙
- 支持普通玩家最大尺寸限制
- 支持按地图大小收费
- 支持 Vault 经济余额检测
- 支持背包空间检测
- 支持相同图片 + 相同尺寸复用地图缓存，避免重复创建 mapId
- 支持自动生成墙体、物品展示框和地图
- 支持自定义墙体方块
- 支持权限节点管理

## 适用版本

主要开发和测试版本：

- Paper 1.8.8
- Spigot 1.8.8
- Java 8

其他版本未完全测试，不保证兼容。

## 依赖

如果启用普通玩家花费游戏币领取地图画，需要安装：

- Vault
- 经济插件，例如 EssentialsX

如果没有安装 Vault 或经济插件，普通玩家花费游戏币功能将无法正常使用。

## 说明

插件首次启动后会生成：

```text
plugins/EFMapArt/
├─ config.yml
├─ mapdata.yml
└─ images/

请将图片放入：plugins/EFMapArt/images/

| 指令                                 | 说明              |
| ---------------------------------- | --------------- |
| `/efmapart`                        | 打开地图画 GUI       |
| `/efmapart gui`                    | 打开地图画 GUI       |
| `/efmapart help`                   | 查看插件帮助          |
| `/efmapart list`                   | 查看图片列表          |
| `/efmapart recommend <图片文件名>`      | 自动推荐地图画尺寸       |
| `/efmapart get <图片文件名> <宽> <高>`    | 普通玩家领取地图画物品     |
| `/efmapart wall <图片文件名> <宽> <高>`   | 管理员自动生成地图画墙     |
| `/efmapart wall cancel`            | 取消地图墙放置         |
| `/efmapart give <图片文件名>`           | 管理员生成 1×1 地图画   |
| `/efmapart create <图片文件名> <宽> <高>` | 管理员生成指定尺寸地图画到背包 |

| 权限                | 默认  | 说明          |
| ----------------- | --- | ----------- |
| `efmapart.gui`    | 所有人 | 允许打开 GUI    |
| `efmapart.get`    | 所有人 | 允许领取地图画物品   |
| `efmapart.wall`   | OP  | 允许自动生成地图画墙  |
| `efmapart.create` | OP  | 允许管理员直接生成地图 |
| `efmapart.admin`  | OP  | 管理员权限       |
| `efmapart.bypass` | OP  | 绕过尺寸限制和费用限制 |
