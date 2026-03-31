# WiFiKit - Wi-Fi 开发套件库

<div align="center">
  <img src="wifi-kit-logo.svg" width="500" alt="wifi-kit-logo">
</div>

![API](https://img.shields.io/badge/API-19%2B-brightgreen)
![Version](https://img.shields.io/badge/version-1.2.0-blue)
![License](https://img.shields.io/badge/license-Apache%202.0-green)

> 📡 A lightweight Android Wi-Fi toolkit  
> 轻量级 Android Wi-Fi 管理库，适用于 Wi-Fi 扫描、设备配网、网络诊断等场景。

## 一、模块简介

WiFiKit 主要对常见的 Wi-Fi 操作做了一层封装，比如扫描附近网络、连接指定 Wi-Fi、获取信号强度、识别安全类型等，同时也支持监听 Wi-Fi 状态变化。

整体用起来比较简单，接口也相对清晰，适合在 Android 项目中直接集成使用，省去自己从底层一点点实现的时间。

---

## 框架特点
- 扫描附近 Wi-Fi，支持结果过滤
- 连接 / 断开指定 Wi-Fi（自动适配系统版本）
- 获取连接信息（SSID、BSSID、IP、网关、DNS、频率、信号强度、安全类型等）
- 监听 Wi-Fi 状态变化（开关 / 连接 / 断开）
- 封装定位权限检查与申请

---

## 二、SDK 适用范围

- Android SDK 版本：Min SDK 19（Android 4.4）及以上

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
dependencies {
    implementation 'com.github.starseaway:wifikit:1.2.0'
}
```

```kotlin
dependencies {
    implementation("com.github.starseaway:wifikit:1.2.0")
}
```

### 3. 初始化模块

> 请在 Application 中初始化 WiFiKit，否则部分功能无法使用

```kotlin
class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        WiFiKit.init(this)
    }
}
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

### 6. 监听 Wi-Fi 状态变化

```kotlin
val observer = WifiStateObserver()
observer.start(object : WifiStateCallback {
    override fun onWifiStateChanged(state: Int) {
        // Wi-Fi 开关状态变化
    }

    override fun onWifiConnected() {
        // 已连接 Wi-Fi
    }

    override fun onWifiDisconnected() {
        // 已断开 Wi-Fi
    }
})
```

页面销毁时记得停止监听：

```kotlin
observer.stop()
```

---

## 五、目录结构

```text
com.xinyi.wifikit
│
├── WiFiKit.java                     # WiFiKit 核心入口（初始化与系统服务获取）
├── connect                     
│   ├── WifiConnector.java           # Wi-Fi连接相关，用于连接指定的Wi-Fi网络
│   ├── WifiNetworkCallback.java     # Android 10+ 的网络回调实现
│   └── ConnectCallback.java         # 连接结果回调接口
├── info                        
│   └── WifiInfoHelper.java          # Wi-Fi 信息获取工具，获取当前Wi-Fi名称、BSSID等信息
├── permission
│   └── LocationPermission.java      # 权限处理，检查/申请定位权限
├── scanner                    
│   ├── WifiScanner.java             # Wi-Fi 扫描相关，用于扫描附近 Wi-Fi
│   ├── ResultFilter.java            # 自定义扫描结果过滤器
│   └── ScannerCallback.java         # 扫描结果回调接口
├── state                            # 状态与分析工具
│   ├── WifiStateObserver.java       # 监听 Wi-Fi 状态变化（连接/断开/切换等）
│   ├── WifiSignalStrength.java      # Wi-Fi 信号强度计算与等级判定
│   └── WifiSecurityAnalyzer.java    # 安全类型判断（WEP/WPA/WPA2/WPA3等）
```

## 六、版本变更记录

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