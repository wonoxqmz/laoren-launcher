# 老友桌面 OldFriend Launcher

一款**开源免费**的 Android 适老化桌面（Launcher），大字、大图标、大按钮，
把「打电话」和「常用应用」做到一眼可见、一点直达。

> 起因：市面上的「老人桌面」App 高级功能动辄收费，而核心体验其实不复杂。
> 于是用纯 Java + 原生 API（无第三方依赖、无 Gradle）自己实现了一个基本相同的开源版。

![icon](res/drawable-xxxhdpi/ic_launcher.png)

## 功能

- **大字时钟卡**：时间 / 日期 / 星期 / 电量，字号随卡片自适应
- **联系人一键拨号**：大头像 + 24sp 名字卡片，点一下直接拨号
  - 点头像循环换颜色，支持从手机通讯录导入
  - 长按卡片：编辑 / 上移 / 下移 / 删除
- **适老拨号盘**：全屏大按钮，按键语音播报数字，点显示区删末位
- **应用页**：全部已安装应用大图标（左右滑动或点底栏切换）
- **语音播报**（系统 TTS）：点按播报名字、整点报时
- **桌面锁定**：锁定后长按不再弹出编辑菜单，防止长辈误删误改
- **6 套适老配色**：经典蓝 / 暖杏晨光 / 静谧森林 / 玫瑰奶茶 / 燕麦青松 / 极简深色
- **可调外观**：字号 4 档、联系人每行 2/3 个、卡片圆角 3 档
- **设为默认桌面**：设置页一键跳转系统默认桌面选择

## 不包含的功能（与商业版差异）

自动接听微信语音/视频、微信辅助操作、远程协助 / 子女端、短信转发、
跌落检测等需要**无障碍权限 + 后台常驻 + 云账号**的功能，本项目不做——
这些既是原版的付费点，也存在权限滥用的风险。本项目**不联网、不上传任何数据**，
联系人只保存在本机 SharedPreferences。

## 构建

无需 Gradle、无需 Android Studio，只要 JDK + Android SDK 命令行工具：

```bash
bash build.sh   # aapt2 → javac → d8 → zipalign → apksigner，约 30 秒
```

产物：`OldFriendLauncher-v1.0.0.apk`（约 65 KB）

支持 Android 4.4（API 19）~ Android 14+，v1+v2 签名。

## 安装

直接安装 APK 即可。按 Home 键选择「老友桌面」并设为始终，它就成为默认桌面；
想退出，到 系统设置 → 默认桌面 / 默认应用 里换回系统桌面。

## 项目结构

```
├── AndroidManifest.xml
├── build.sh              # 无 Gradle 构建脚本
├── res/                  # 图标与主题资源
└── src/com/lcj/oldfriend/
    ├── LauncherActivity.java   # 主桌面（HOME）
    ├── DialerActivity.java     # 适老拨号盘
    ├── SettingsActivity.java   # 设置
    ├── AboutActivity.java      # 关于
    ├── ContactEditDialog.java  # 联系人添加/编辑
    ├── TimeView.java           # 大字时钟卡（自绘）
    ├── AvatarView.java         # 圆形头像（自绘）
    ├── IconView.java           # 自绘矢量图标
    ├── Store.java / Contact.java / Prefs.java / Theme.java
    └── U.java / Tts.java / AppRepo.java
```

## 免责声明

本项目为独立开发的开源学习作品，与任何商业「老人桌面」应用无关；
界面与交互为通用适老化设计，不包含任何原应用的代码或资源。

## 更新日志

### v1.0.0（2026-10-31）

- 首个版本：主桌面、联系人一键拨号、适老拨号盘、应用页、
  大字时钟、语音播报、整点报时、桌面锁定、6 套主题、外观设置
