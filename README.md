# 快对净化 KuaiDuiPure

针对 **快对AI**（`com.kuaiduizuoye.scan`，7.8.0）的 LSPosed 模块，基于 LibXposed API 102。

## 当前功能
- 跳过开屏广告（`rg.d` / `ResumeSplashActivity`）
- 拦截应用内广告配置（`ai.a` 的广告位配置）
- 拦截穿山甲、优量汇、百度、快手等广告 SDK 的加载/展示
- 拦截广告 View，避免广告卡片/横幅显示

## 安装
1. 安装 `KuaiDuiPure-v1.0.apk`。
2. 在 LSPosed 管理器启用「快对净化」。
3. 作用域只勾选 **快对AI**（`com.kuaiduizuoye.scan`）。
4. 强行停止快对AI 后重新打开。

## 注意
这是第一版，先以去广告和开屏为主；不同版本广告 SDK 可能有差异，遇到闪退/漏广告请记录版本和现象。
