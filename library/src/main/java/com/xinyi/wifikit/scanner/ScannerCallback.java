package com.xinyi.wifikit.scanner;

import android.net.wifi.ScanResult;

import java.util.List;

/**
 * Wi-Fi 扫描的回调接口
 *
 * @author 新一
 * @date 2025/5/29 10:32
 */
public interface ScannerCallback {

    /**
     * 扫描成功时的回调
     *
     * @param results 扫描到的 Wi-Fi 列表
     */
    void onResult(List<ScanResult> results);

    /**
     * 扫描超时时的回调（默认空实现）
     */
    default void onTimeout() {
    }

    /**
     * 扫描失败时的回调（默认空实现）
     *
     * @param reason 失败原因描述
     */
    default void onFailure(String reason) {
    }
}