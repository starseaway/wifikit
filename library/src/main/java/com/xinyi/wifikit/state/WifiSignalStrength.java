package com.xinyi.wifikit.state;

import android.net.wifi.ScanResult;

import com.xinyi.wifikit.R;

/**
 * Wi-Fi 信号强度获取工具
 *
 * <p> 用于将 RSSI 信号强度转换为便于人类理解的等级（如：满格、较强、中等、较弱） </p>
 *
 * <p>
 *    Android 中 Wi-Fi 信号强度以 RSSI（Received Signal Strength Indicator）为单位，值越小信号越弱。
 * <p>
 *
 * <a href="https://developer.android.com/reference/android/net/wifi/ScanResult#level">官方文档参考链接</a>
 * <a href="https://www.netspotapp.com/wifi-signal-strength/">NetSpot 的官方指南</a>
 * <a href="https://www.metageek.com/training/resources/understanding-rssi/">MetaGeek 对 RSSI 和 dBm 的详细解释</a>
 * 一般范围约为 -30 dBm（极强）到 -100 dBm（极弱）。
 * </p>
 *
 * @author 新一
 * @date 2025/5/29 15:14
 */
public class WifiSignalStrength {

    /**
     * 信号等级的枚举数组
     */
    public static final SignalLevel[] SIGNAL_LEVELS = {
        SignalLevel.EXCELLENT,
        SignalLevel.GOOD,
        SignalLevel.FAIR,
        SignalLevel.WEAK,
        SignalLevel.VERY_WEAK
    };

    /**
     * 将 RSSI 转换为信号等级, 并允许误差
     *
     * @param rssi RSSI 信号强度值
     * @param tolerance 允许的误差
     * @return 信号等级
     */
    public static SignalLevel getSignalLevel(int rssi, int tolerance) {
        // 调整 RSSI 值，加入误差范围
        int adjustedRssi = rssi + tolerance;
        // 根据调整后的 RSSI 值判断信号等级
        return getSignalLevel(adjustedRssi);
    }

    /**
     * 将 RSSI 转换为信号等级
     *
     * @param rssi RSSI 信号强度值（如 -55）
     * @return 信号等级
     */
    public static SignalLevel getSignalLevel(int rssi) {
        if (rssi >= -50) return SignalLevel.EXCELLENT;
        if (rssi >= -60) return SignalLevel.GOOD;
        if (rssi >= -70) return SignalLevel.FAIR;
        if (rssi >= -80) return SignalLevel.WEAK;
        return SignalLevel.VERY_WEAK;
    }

    /**
     * 从扫描结果中提取并计算信号等级
     *
     * @param result Wi-Fi 扫描结果
     * @return 信号等级
     */
    public static SignalLevel getSignalLevel(ScanResult result) {
        return getSignalLevel(result.level);
    }

    /**
     * 信号等级枚举
     */
    public enum SignalLevel {

        /**
         * 极强（满格）
         */
        EXCELLENT,

        /**
         * 较强
         */
        GOOD,

        /**
         * 中等
         */
        FAIR,

        /**
         * 较弱
         */
        WEAK,

        /**
         * 极弱
         */
        VERY_WEAK;

        /**
         * 将信号等级转换为数字等级（0~4）
         *
         * @return 数字等级值（0：最差，4：最好）
         */
        public int toNumberLevel() {
            switch (this) {
                case EXCELLENT:
                    return 4;
                case GOOD:
                    return 3;
                case FAIR:
                    return 2;
                case WEAK:
                    return 1;
                default:
                    return 0;
            }
        }

        /**
         * 将信号等级转换为对应的图标资源地址
         *
         * @return 等级图标资源地址
         */
        public int toIconResources() {
            switch (this) {
                case EXCELLENT:
                    return R.mipmap.wifi_excellent;
                case GOOD:
                    return R.mipmap.wifi_good;
                case FAIR:
                    return R.mipmap.wifi_fair;
                case WEAK:
                    return R.mipmap.wifi_weak;
                default:
                    return R.mipmap.wifi_very_weak;
            }
        }
    }
}