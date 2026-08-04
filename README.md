# WiFiBridge - Android Wi-Fi 桥接库

<div align="center">
  <img src="wifi-bridge-logo.svg" width="500" alt="wifi-kit-logo">
</div>

![Version](https://img.shields.io/badge/version-2.1.0-blue)
![License](https://img.shields.io/badge/license-Apache%202.0-green)
![API](https://img.shields.io/badge/API-19%2B-brightgreen)

> Making Android Wi-Fi easier to work with.
> 让 Android Wi-Fi 开发更简单。

## 一、模块简介

WiFiBridge 对 Android 原生 Wi-Fi 能力进行了统一桥接与封装；
包含扫描附近网络、连接指定 Wi-Fi、获取连接信息、监听 Wi-Fi 状态以及分析信号强度、安全类型等。

库内部对不同 Android 版本进行了兼容处理，直接统一了常用 Wi-Fi API 的使用方式，极大的减少了直接调用系统接口带来的繁琐操作。

整体接口简单清晰，适合在 Android 项目中直接集成使用，省去自己从底层一点点实现的时间。

---

## 特点

- 扫描附近 Wi-Fi，支持结果过滤
- 连接 / 断开指定 Wi-Fi（自动适配系统版本）
- 获取连接信息（SSID、BSSID、IP、网关、DNS、频率、信号强度、安全类型等）
- 监听 Wi-Fi 状态变化（开关 / 连接 / 断开，回调携带连接信息）
- 封装定位权限检查与申请

---

## 二、SDK 适用范围

| 项目         | 要求                 |
|------------|--------------------|
| Min SDK    | 19（Android 4.4）及以上 |
| JVM Target | 1.8                |
| Kotlin     | 1.6+               |

---

## 三、集成方式

### 1. 根据 Gradle 版本或项目配置自行选择在合适的位置添加仓库地址
```groovy
maven {
    // jitpack仓库
    url 'https://jitpack.io' 
}
```

### 2. 在 `build.gradle` (Module 级) 中添加依赖：

```groovy
implementation 'com.github.starseaway:wifi-bridge:2.1.0'
```

```kotlin
implementation("com.github.starseaway:wifi-bridge:2.1.0")
```

---

## 四、快速开始

### 1. 检查权限

> 如果你要在 Android 10+ 连接 Wi-Fi，通常还需要结合运行时定位权限和系统定位开关一起处理
> 若全部已授权，会直接回调 onGranted() 方法，否则内部会调用 requestPermissions 弹出系统授权框。

```kotlin
// 检查定位权限，未授权则发起请求
LocationPermission.checkAndRequestPermissions(activity, object : PermissionCallback {
    override fun onGranted() {
        // 权限已就绪，可以继续操作 Wi-Fi
    }
})
```

**注意事项：**
- 该方法只负责发起权限请求，不处理用户拒绝或 “永久拒绝” 的情况。
- 调用方需在 activity 里重写 onRequestPermissionsResult() 中自行处理结果并决定是否再次请求或引导用户到设置页

```kotlin
override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_CODE) {
        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        if (allGranted) {
            // 全部权限通过
            mPermissionCallback.onGranted()
        } else {
            // 是否存在“永久拒绝”
            val permanentlyDenied = permissions.any { perm ->
                ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED &&
                !ActivityCompat.shouldShowRequestPermissionRationale(this, perm)
            }

            if (permanentlyDenied) {
                // 引导去设置页
            } else {
                // 普通拒绝
            }
        }
    }
}
```

也可以先判断定位服务是否开启：

```kotlin
val enabled = LocationPermission.isLocationEnabled(context)
```

### 2. 扫描附近 Wi-Fi

```kotlin
val scanner = WifiScanner()
scanner.scanWifi(object : ScannerCallback {

    /**
     * 扫描成功时的回调
     *
     * @param results 扫描到的 Wi-Fi 列表
     */
    override fun onResult(results: MutableList<ScanResult>) {
        for (result in results) {
            val ssid: String? = result.SSID
            val rssi: Int = result.level
        }
    }

    override fun onTimeout() {
        // 扫描超时
    }

    override fun onFailure(reason: String?) {
        // 扫描失败
    }
})
```

如果你想过滤某些结果，也可以加过滤器：

```kotlin
scanner.setFilter { result: ScanResult ->
    result.SSID == null || result.SSID.trim { it <= ' ' }.isEmpty()
}
```

### 3. 连接指定 Wi-Fi

```kotlin
val connector = WifiConnector()
connector.connect("WifiName", "WifiPassword", object : ConnectCallback {
    override fun onSuccess() {
        // 连接成功
    }

    override fun onFailure(reason: String?) {
        // 连接失败
    }
})
```

说明一下（库内部已经做了版本适配）：
- Android 10 及以上：基于 NetworkSpecifier + NetworkRequest 实现
- Android 9 及以下：基于 WifiConfiguration 实现

### 4. 获取 Wi-Fi 信息

_未连接时返回 null_

```kotlin
// 当前连接的 Wi-Fi 名称（SSID）
val ssid = WifiInfoHelper.getCurrentSsid()

// 当前连接的接入点 BSSID（路由器 MAC 地址）
val bssid = WifiInfoHelper.getCurrentBssid()

// 当前设备在局域网中的 IP 地址
val ip = WifiInfoHelper.getCurrentIpAddress()

// 当前 Wi-Fi 信号强度（RSSI，单位 dBm，值越接近 0 表示信号越强）
val rssi = WifiInfoHelper.getCurrentSignalLevel()

// 当前链路速率（单位 Mbps，表示理论传输速率）
val speed = WifiInfoHelper.getLinkSpeed()

// 是否已连接到 Wi-Fi（基于 networkId 判断）
val connected = WifiInfoHelper.isWifiConnected()
```

也可以读取更详细的信息：

```kotlin
// 网关地址（通常是路由器地址）
val gateway = WifiInfoHelper.getGatewayAddress()

// DNS 服务器地址
val dns = WifiInfoHelper.getDnsAddress()

// 子网掩码
val mask = WifiInfoHelper.getSubnetMask()

// DHCP 信息汇总（包含 ip/gateway/dns/lease 等，适合调试输出）
val dhcp = WifiInfoHelper.getDhcpInfoSummary()
```

### 5. 判断信号等级和安全类型

- 信号等级：

> RSSI 一般范围约为 -30 dBm（极强）到 -100 dBm（极弱），实际表现会受设备天线与环境干扰影响。

```kotlin
// 将 RSSI 转换为更直观的信号等级
val level = WifiSignalStrength.getSignalLevel(-58)

// 转换为当前等级对应的 UI 图标的资源地址
val wifiIcon = level.toIconResources()
```

- 安全类型：

```kotlin
// 根据扫描结果分析 Wi-Fi 安全类型（如 OPEN / WEP / WPA_WPA2 / WPA3）
val type = WifiSecurityAnalyzer.analyze(scanResult)
```

### 6. 注册 Wi-Fi 状态变化监听

```kotlin
val observer = WifiStateObserver()
observer.register(object : WifiStateCallback {
    override fun onWifiStateChanged(state: Int) {
        // Wi-Fi 开关状态变化
    }

    override fun onWifiConnected(info: WifiConnectionInfo?) {
        // 已连接 Wi-Fi，可直接使用 ssid / bssid / rssi 等信息
        // info?.ssid
    }

    override fun onWifiDisconnected(info: WifiConnectionInfo?) {
        // 已断开 Wi-Fi，info 为断开前的连接快照
    }
})
```

页面销毁时记得注销监听：

```kotlin
observer.unregister()
```

---

## 五、版本变更记录

### V2.1.0 (2026-08-04)
- 📦 deps: 升级安卓设备状态核心库版本至 V3.0.0
- ✨ feat: 状态监听回调补充当前连接信息
- 🦄 refactor: 优化 Wi-Fi 连接状态监听实现
- 🦄 refactor: 优化 Wi-Fi 扫描任务调度方式

### V2.0.1 (2026-07-02)
- 🦄 refactor: 调整 Wi-Fi 状态监听方法命名

### V2.0.0 (2026-07-02)
- 修改库的名称与包名
- 更新 device 依赖版本

### V1.2.1 (2026-03-30)
- build: 修改 agp 构建版本

### V1.2.0 (2026-03-30)
- 从该版本开始，正式在 GitHub 开源

### V1.1.0 (2025-08-04)
- 优化 Wi-Fi 连接状态判断逻辑，提升准确性
- 信号等级计算增加误差容错机制

### V1.0.6 (2025-05-30)
- 修复 R8 优化枚举类导致的编译期崩溃或运行时问题

### V1.0.4 (2025-05-30)
- 优化循环扫描 Wi-Fi 的任务调度逻辑

### V1.0.3 (2025-05-30)
- 去除 Wi-Fi 扫描类中的匿名内部类
- 全面优化 Wi-Fi 扫描流程

### V1.0.2 (2025-05-30)
- 更新 device 工具库依赖版本
- 优化 Wi-Fi 连接流程

### V1.0.1 (2025-05-29)
- 更新 device 工具库依赖版本

### V1.0.0 (2025-05-29)
- 初始版本，支持 Wi-Fi 扫描、连接、权限处理、信号强度分析、安全等级判断等功能