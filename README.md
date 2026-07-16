# EhViewer 自动翻译（自用）

本仓库仅用于为 EhViewer 增加“图片内文本翻译”能力，聚焦翻译相关逻辑与使用说明。内容仅供个人自用与研究，不提供技术支持，接口与行为可能随时变更。

Fork 来源：基于 `Ehviewer_CN_SXJ` 仓库二次开发并聚焦翻译功能，上游仓库 `https://github.com/xiaojieonly/Ehviewer_CN_SXJ`；原始项目 `EhViewer` 由 `seven332` 开发（`https://github.com/seven332/EhViewer`）。

## 功能概览
- 大模型单页翻译：在阅读页对当前图片进行遮罩与生成式翻译，生成译后图片并保存到 `translated` 子目录，页面会自动刷新显示。
- 普通批量翻译：将整本画廊打包为 zip 上传至本地服务，由服务端执行检测、OCR、擦写/重绘、翻译与导出，客户端轮询进度并下载结果。
- 使用定位：仅用于方便一键机翻，效果可能差强人意，适用于懒得手动修的，可在手机上直接翻译。
- 交互能力：支持在阅读页进行“译文/原文”切换，便于比对与回退。
- 支持范围翻译：可在普通批量翻译中选择翻译范围（例如2-5，代表第2页到第5页），默认全部。

## 演示

翻译配置（1–2）：在设置中填写普通服务主机/端口并测试连接；配置大模型的基地址、模型、通用提示词与 `API Key`。点击图片可查看原图。

<table>
  <tr>
    <td align="center" valign="top">
      <a href="doc/1.png"><img src="doc/1.png" alt="演示1：普通服务配置" width="360"></a>
      <div>演示1：设置阅读是否优先显示翻译版本</div>
    </td>
    <td align="center" valign="top">
      <a href="doc/2.png"><img src="doc/2.png" alt="演示2：大模型配置" width="360"></a>
      <div>演示2：翻译服务配置</div>
    </td>
  </tr>
  <tr><td colspan="2" style="height:12px"></td></tr>
</table>

## 下载地址

[//]: # (- [Appteka]&#40;https://appteka.store/app/acdr168648&#41;)
- [百度云](https://pan.baidu.com/s/1rh-lvEc-QjiMtfPi6T1BKA) 提取码：7p6z
- [夸克网盘](https://pan.quark.cn/s/0de32f69e12a) 提取码：SnVD
- [蓝奏云](https://wwbfg.lanzouu.com/iWAFL3uefh8d)，电脑端可正常下载 提取码：3txx
- [GitHub](https://github.com/xiaojieonly/Ehviewer_CN_SXJ/releases)
- Torrent链接:magnet:?xt=urn:btih:ec1403341edaba853b0836d2d3c5976498572a3c&xt=urn:btmh:1220453fdf4272b27c81d88a4ddae25a64ca524a13e53f835430b4acacb2e7ae3bcb&dn=EhViewer-2.0.2.2.apk&xl=27747451

普通翻译流程（3–6）：在下载页选择“上传翻译”（支持范围选择），上传打包 zip；客户端轮询服务进度；结果下载到 `translated` 目录并自动刷新，阅读页可在“译文/原文”间切换。

<table>
  <tr>
    <td align="center" valign="top">
      <a href="doc/3.png"><img src="doc/3.png" alt="演示3：选择范围" width="300"></a>
      <div>演示3：在下载列表选择需要翻译的作品</div>
    </td>
    <td align="center" valign="top">
      <a href="doc/4.png"><img src="doc/4.png" alt="演示4：上传打包" width="300"></a>
      <div>演示4：范围选择</div>
    </td>
  </tr>
  <tr>
    <td align="center" valign="top">
      <a href="doc/5.png"><img src="doc/5.png" alt="演示5：进度轮询" width="300"></a>
      <div>演示5：翻译队列进度显示</div>
    </td>
    <td align="center" valign="top">
      <a href="doc/6.png"><img src="doc/6.png" alt="演示6：译后展示与切换" width="300"></a>
      <div>演示6：译后展示</div>
    </td>
  </tr>
</table>

大模型单页翻译（7–9）：在阅读页菜单选择“大模型翻译本页”（可先遮罩文本区域）；展示生成式翻译效果；对提示词增加“上色”等样式后展示改进示例。

<table>
  <tr>
    <td align="center" valign="top">
      <a href="doc/7.png"><img src="doc/7.png" alt="演示7：选择大模型翻译本页" width="320"></a>
      <div>演示7：选择大模型翻译本页</div>
    </td>
    <td align="center" valign="top">
      <a href="doc/8.png"><img src="doc/8.png" alt="演示8：大模型翻译效果" width="320"></a>
      <div>演示8：大模型翻译效果</div>
    </td>
    <td align="center" valign="top">
      <a href="doc/9.png"><img src="doc/9.png" alt="演示9：提示词上色后的效果" width="320"></a>
      <div>演示9：提示词上色后的效果</div>
    </td>
  </tr>
</table>

## 社区与更新日志

唯一X账号：https://x.com/Sherloc21784244    
Telegram群: https://telegram.me/+WyclP8pPlk-JfbwS   
Telegram通知群: https://telegram.me/Ehviewer_xiaojieonly_channel   


# Changelog
## 2026/07/01
### 新版发布2.0.2.2

- 修复了浏览历史未正确保存的问题
- 修复评论时输入框被键盘遮挡的问题


## 2026/07/01 
### 新版发布2.0.2.1

- 修复底部导航栏导致的页面遮蔽问题
- 修复浏览画廊时图片会自动下载到下载目录的问题
- 实现WiFi下载迁移功能：将数据传世方式从json变更为二进制帧，新增了下载目录的迁移项
- 添加多语言支持的下载目录迁移字符串资源
- 添加“阅读时同步下载”功能：设置-下载-观看时同步下载
- 增强下载恢复功能：添加了在从层次结构中分离时取消任务的逻辑
- 更新 Gradle 版本和插件
- [百度云](https://pan.baidu.com/s/19ZEEdF3waR3hkMghbmu7rw) 提取码：gxsj
- [夸克网盘](https://pan.quark.cn/s/bffd976d75c4) 提取码：TJDR
- [蓝奏云](https://wwbfg.lanzouu.com/iZPct3u2hxef)，电脑端可正常下载 提取码：1obh
- [GitHub](https://github.com/xiaojieonly/Ehviewer_CN_SXJ/releases)
- Torrent链接:magnet:?xt=urn:btih:76cf0c1951465d0ab5e4e6fc0b4f371aceec8f9c&xt=urn:btmh:12201dbee30e3448b93d0b725ba19c5fe6a19b042dd662fe9ef898d92c3e67ca6c80&dn=EhViewer-2.0.2.1.apk&xl=27747322



## 2026/06/01 祝大家六一儿童节快乐~
### 新版发布2.0.1.8

- 将 jsoup 从 1.18.1 降级到 1.15.4，以避免在某些 Android 环境中出现 NoClassDefFoundError
- 在 EhDB 中添加了空检查，以防止快速搜索操作期间潜在的 NullPointerExceptions
- 增强了 EhEngine 中 TopListParser 的错误处理，以在运行时错误上引发更具描述性的 ParseException
- 重构SpiderDen，确保访问下载目录时的线程安全
- 改进了 ArchiverDownloadDialog 中的文件名处理，以防止非法路径和长文件名
- 添加了 ArchiverDownloadCompleter 来处理存档器任务的下载完成和状态检查
- 在 EhApplication 中集成挂起的下载恢复
- 增强设置，提供管理待处理存档下载的方法
- 更新了 ArchiverDownloadDialog 以利用 ArchiverDownloadCompleter 进行下载处理
- 改进了 ArchiverDownloadProgress 中的下载进度跟踪
- 确保在 SpiderDen 中创建下载目录
- 猫尾草：添加小米系统优化助手，优化后台下载和通知管理
- 猫尾草：add gradle wrapper jar and properties for CI build
- Cololi：沉浸式底部导航栏 (#2597)
- [百度云](https://pan.baidu.com/s/1hFLjNrU-_c1u8iugt82d6g) 提取码：wz2h
- [夸克网盘](https://pan.quark.cn/s/133080ed0571) 提取码：ekzT
- [蓝奏云](https://wwbfg.lanzouu.com/i1hv53qtvjba)，电脑端可正常下载 提取码：eg80
- [GitHub](https://github.com/xiaojieonly/Ehviewer_CN_SXJ/releases)
- Torrent链接:magnet:?xt=urn:btih:a14acab7edec4b1c5f10d291296fda3e19449a0d&xt=urn:btmh:1220f25dba401d5db2cfb2d864114728418c1e7bf746679182a3155ef3e1714539cb&dn=EhViewer-2.0.1.8.apk&xl=27739161


## 2026/05/01 祝大家五一劳动节快乐~
### 新版发布2.0.1.7

- 修复SpiderInfo读取时的OOM风险并升级JDK至21
- 修复图片搜索无法使用的问题
- orbisai0security：the vendored giflib library performs multiple m... in gifalloc.c
- 增加对 WebView/CookieManager 初始化失败的异常处理
- En：修复了已下载项目的按标签搜索功能
- 猫尾草：restore gradle wrapper jar and properties
- 升级Gradle至9.3.1及Android插件至9.1.1
- [百度云](https://pan.baidu.com/s/17a5zwo0HeTp_Iqh9P2QwXQ) 提取码：7y92
- [夸克网盘](https://pan.quark.cn/s/036dd4d5f09d) 提取码：B6J6
- [蓝奏云](https://wwbfg.lanzouu.com/iFc783oecgmh)，电脑端可正常下载 提取码：dfg8
- [GitHub](https://github.com/xiaojieonly/Ehviewer_CN_SXJ/releases)
- Torrent链接:magnet:?xt=urn:btih:c3aab1194eb843bac7274b87873dc94041310e52&xt=urn:btmh:1220c3640ed2ef7f588376f7dadeffc99463d764608b577beb038378788996fc1ad5&dn=EhViewer-2.0.1.7.apk&xl=27705830


## 2026/04/01 祝大家愚人节伤心
### 新版发布2.0.1.6

- 搜索时过滤文本中的换行符
- 修复下载列表排序奔溃的问题
- 优化 EGL 初始化逻辑并增加 OpenGL 渲染故障时的回退机制
- 排行榜中，画廊排行从原先的跳转画廊搜索，改为直接跳转对应画廊
- 优化归档下载逻辑与文件名生成
- 优化解析错误日志清理逻辑并增加异常处理
- 优化搜索文本过滤，直接移除换行符而非替换为空格
- 修正登录WebView客户端设置及资料获取逻辑
- 升级SDK版本并启用coreLibraryDesugaring
- miki sayaga：新增一个多标签搜索组合页面（未完成）
- 修复部分多标签搜索组合页面bug
- 将部分代码从java迁移到kotlin
- [百度云](https://pan.baidu.com/s/1koygBtTteJtDHZTQYL8wXQ) 提取码：iqev
- [夸克网盘](https://pan.quark.cn/s/b41421a61e70) 提取码：MrnK
- [蓝奏云](https://wwbfg.lanzouu.com/iNSBF3m1jveb)，电脑端可正常下载 提取码：i4f8
- [GitHub](https://github.com/xiaojieonly/Ehviewer_CN_SXJ/releases)
- Torrent链接:magnet:?xt=urn:btih:8488a933608f5b3901de8a2bedc669e20ff94839&xt=urn:btmh:1220dbe6fffcb6aff255e089e3bb0cefeb33940c27d82e37163252abe4813c987e33&dn=EhViewer-2.0.1.6.apk&xl=27702785


## 2026/03/01 提前祝大家元宵节快乐
### 新版发布2.0.1.5

- 调整 Analytics上报字段
- 将部分代码从java迁移到kotlin
- 修复 `BitmapUtils` 中的潜在整数溢出问题
- 优化VPN检测逻辑，增加权限检查和异常处理
- 优化登录流程异常处理和进度显示
- 清理请求头中的换行符避免崩溃
- 调整下载列表页面的标题格式
- zyl-hub：修复了在搜索框不为空时的搜索历史补全
- [百度云](https://pan.baidu.com/s/1_rbxH65GXWjx_pxYIf0Pug) 提取码：wzv4
- [夸克网盘](https://pan.quark.cn/s/95915acfe88b) 提取码：HmUu
- [蓝奏云](https://wwbfg.lanzouu.com/iYopw3jiyizi)，电脑端可正常下载 提取码：fhbq
- [GitHub](https://github.com/xiaojieonly/Ehviewer_CN_SXJ/releases)
- Torrent链接:magnet:?xt=urn:btih:4869fe5d6cebba6e1f2b672e3523cca83838b466&xt=urn:btmh:1220df01410d828d8544b9efe8e15fe3e7323eab74d2ae962947f324dd36e0a99b74&dn=EhViewer-2.0.1.5.apk&xl=27934862


## 2026/02/01 给大家提前拜个早年，祝大家新春快乐~
### 新版发布2.0.1.4

- 更新通过webview 通过cloudflare验证获取用户名称的功能（裸连目前无法使用此功能）
- 修复略缩图因状态共用导致的重复显示同一张图片
- 登录时，修复用户名称获取逻辑，由于技术限制，sni开启的情况下无法通过机器人验证
  网页登录只能使用VPN进行
- 在裸连使用cookie登录时，现在会跳过获取用户详情步骤
- 更新安卓targetSdkVersion到30，所有用户需要授予管理文件权限，否则将无法读取旧画廊
- [百度云](https://pan.baidu.com/s/1VD8NwRZTUVkO5hsTCPd95A) 提取码：hfms
- [夸克网盘](https://pan.quark.cn/s/685e409e6164) 提取码：yiie
- [蓝奏云](https://wwbfg.lanzouu.com/iM6GO3hj88cd)，电脑端可正常下载 提取码：cjgb
- [GitHub](https://github.com/xiaojieonly/Ehviewer_CN_SXJ/releases)
- Torrent链接: magnet:?xt=urn:btih:fd7fb29419c9b6f7a22bbbd899500fb828e2e5ce&xt=urn:btmh:1220aa0bf5802d34adeb7e60b17f0466f951a4d96dd4ab5b06158af2690bee2cb073&dn=EhViewer-2.0.1.4.apk&xl=27623297


## 2026/01/05 紧急修复
### 新版发布2.0.1.2

- 暂时回滚图片解码方式，等后续优化好了再上

## 2026/01/04 紧急修复
### 新版发布2.0.1.1

- 限制了详情页初始加载的预览图数量为40张，以减少初次创建视图的开销。
- 修复了 `DownloadFragment` 中因 Activity 销毁后关闭对话框可能导致的崩溃问题
- 修复了下载列表画廊的删除和拖拽排序无法及时生效的问题
- [百度云](https://pan.baidu.com/s/1EPEqfeklH0Pdk_mEiuJ8rQ) 提取码：9cas
- [夸克网盘](https://pan.quark.cn/s/cb19c11bcb6d) 提取码：WJWs
- [蓝奏云](https://wwbfg.lanzouu.com/ipmeo3fa1umh)，电脑端可正常下载 提取码：4jop
- [GitHub](https://github.com/xiaojieonly/Ehviewer_CN_SXJ/releases)
- Torrent链接: magnet:?xt=urn:btih:a547ca192aada5109bbf891bc5ea21b04d50972e&xt=urn:btmh:12209bc1fe78c6f1b431a120f2a24dc89556f5a8da767f710d8b6840a461146fc434&dn=EhViewer-2.0.1.1.apk&xl=27606911


## 2026/01/01 祝大家新年快乐~
### 新版发布2.0.1.0

- 新增下载页拖拽排序设置项，允许用户启用或禁用该功能
- 该设置现在会被保存，以便在应用重启后保持不变
- 回归到旧版图片解码代码，引入libwebp插件，并添加webp图片格式的处理方法
- 更新依赖项并为 16KB 页面大小设备添加适配
- 调整下载场景 FAB 图标
- HaYaShi: 下载画廊添加文件大小排序 (#2321)
- 为下载分类添加“全部”选项并优化布局
- 当解析 URL 失败时，会通过 `FirebaseCrashlytics` 记录异常，以防止应用崩溃并帮助调试。
- 同步德语、西班牙语、法语、韩语、泰语、日语和繁体中文翻译
- 猫尾草：新增按分类筛选下载内容的功能
- 猫尾草：给恢复下载项、清空下载冗余新增进度条，免得等的有问题
- 猫尾草：在设置-EH选项卡新增当前系统主题显示，方便查看bug（这样容易分辨是否是系统造成的问题）
- [百度云](https://pan.baidu.com/s/1ZOzR9W24cDRVYtiR_msOoQ) 提取码：2rsb
- [夸克网盘](https://pan.quark.cn/s/b023fa0249dd) 提取码：iKSY
- [蓝奏云](https://wwbfg.lanzouu.com/iSJdX3eyu95g)，电脑端可正常下载 提取码：92ad
- [GitHub](https://github.com/xiaojieonly/Ehviewer_CN_SXJ/releases)
- Torrent链接: magnet:?xt=urn:btih:241667f787c7f5d62e393d2404d2f9e2280d9cfb&xt=urn:btmh:122067d50a27f6b620b065a961d4cb2ad048e470b1e3b416ced2468c19d3b0d0cf61&dn=EhViewer-2.0.1.0.apk&xl=27606133

## 自用声明

- 本项目仅满足作者个人使用场景，不保证稳定性与兼容性，不保证与上游仓库的同步更新。
- 不提供任何账号、密钥或云资源，也不保证第三方服务可用性。
- 使用造成的风险与后果由使用者自行承担，请勿用于任何违规用途。
 - 软件包分离与数据隔离：本软件与原始 EhViewer 的安装包分离，数据默认隔离。可使用“导出软件数据”功能进行同步与迁移（导出为数据库文件后在另一侧导入）。

## 大模型的局限与策略

- NSFW 限制：生成式模型可能拒绝或弱化成人内容，或输出不完整。策略为仅遮罩气泡/文本区域、弱化提示词、必要时降级为普通服务。
- 批量限制：成本与速率限制，以及额外的内容审查导致批量不太理想，仅在必要时使用大模型，故“不支持使用大模型进行批量翻译”。批量需求请使用下述普通服务方案。
- 质量与排版：生成图可能尺寸不一致或出现边缘瑕疵，客户端会缩放并做透明背景处理，但仍可能存在错位或风格不一致。
- API 依赖：需自备 `API Key`，模型与基地址可在应用设置内配置通用提示词与模型参数。

## 普通翻译需要自建服务

- 需要本地或内网运行一个 HTTP 服务（默认 `http://0.0.0.0:8000`）。
- 客户端调用的接口约定：
  - `GET /`：连通性测试。
  - `POST /translate`：表单上传 `zip`（整本）或 `file`（单张），返回 `{"job_id": "..."}`。
  - `GET /status?job_id=...`：查询任务进度与状态。
  - `GET /current_job`：获取当前任务信息。
  - `POST /cancel_current`：取消当前任务。
  - `GET /result?job_id=...`：下载翻译结果（打包输出）。
- 端口与主机在应用“设置-翻译设置”中配置：`translation_base_host/服务器主机`、`translation_base_port/服务器端口`。
- 输出格式建议 `webp`, 保持与输入相同的尺寸与质量。

- 快速部署示例（基于 BallonsTranslator）：
  - 仓库地址：`https://github.com/dmMaze/BallonsTranslator`（部署与依赖说明见其 README）。
  - 环境要求：`Python <= 3.12`，建议安装 Git；确保可以访问所需模型文件。
  - 获取源码或按其说明下载打包版；随后将提供的 API 服务解压到项目根目录下即可。
  - 启动服务：`python -m uvicorn server.api:app --host 0.0.0.0 --port 8000`
  - 需要额外安装：Pillow
                fastapi
                uvicorn
                python-multipart
                psutil
                gradio
  - 在应用中配置“设置-翻译设置”：填入服务主机与端口，使用“测试连接”确认可用后即可开始上传 zip/单页进行翻译。
  - 使用的远端的配置，客户端不支持配置修改，若需修改请直接修改服务配置。

## 配置提示

- 大模型相关：可在设置中配置基地址、模型、通用提示词及 API Key。
- 普通服务相关：在设置中填入服务主机与端口后，可使用“测试连接”验证连通性。
- 模型支持范围：仅支持 Gemini API 的格式的模型。

## 目录与输出

- 译后图片保存在对应画廊下载目录的 `translated` 子目录下。

## 免责声明

- 以上功能与接口均可能调整，不保证长期兼容与可用。
- 本项目不承诺问题修复或用户支持，请谨慎评估并自行建设所需基础设施。
