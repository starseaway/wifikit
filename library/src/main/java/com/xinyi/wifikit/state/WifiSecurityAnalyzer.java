package com.xinyi.wifikit.state;

import android.net.wifi.ScanResult;
import android.text.TextUtils;

/**
 * Wi-Fi 安全类型分析器，用于分析 Wi-Fi 热点的加密方式（WEP、WPA/WPA2/WPA3、开放等）。
 *
 * @author 新一
 * @date 2025/5/29 14:46
 */
public class WifiSecurityAnalyzer {

    /**
     * 分析指定 Wi-Fi 的安全类型
     *
     * @param scanResult 扫描结果
     * @return 安全类型
     */
    public static SecurityType analyze(ScanResult scanResult) {
        if (scanResult == null || TextUtils.isEmpty(scanResult.capabilities)) {
            return SecurityType.UNKNOWN;
        }

        final String cap = scanResult.capabilities.toUpperCase();
        if (cap.contains("WPA3")) {
            return SecurityType.WPA3;
        } else if (cap.contains("WPA") || cap.contains("WPA2")) {
            return SecurityType.WPA_WPA2;
        } else if (cap.contains("WEP")) {
            return SecurityType.WEP;
        } else if (cap.contains("ESS")) {
            return SecurityType.OPEN;
        } else {
            return SecurityType.UNKNOWN;
        }
    }

    /**
     * Wi-Fi 安全类型
     */
    public enum SecurityType {

        /**
         * 未加密（开放网络）
         */
        OPEN,

        /**
         * 使用 WEP 加密
         */
        WEP,

        /**
         * 使用 WPA/WPA2 加密
         */
        WPA_WPA2,

        /**
         * 使用 WPA3 加密
         */
        WPA3,

        /**
         * 未知类型
         */
        UNKNOWN
    }
}