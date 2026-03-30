package com.xinyi.wifikit.scanner;

import android.net.wifi.ScanResult;

import androidx.annotation.NonNull;

/**
 * 扫描结果过滤器接口，用于按条件过滤 {@link ScanResult}。
 *
 * @author 新一
 * @date 2025/5/29 10:36
 */
public interface ResultFilter {

    /**
     * 判断指定扫描结果是否被拒绝
     *
     * @param result 当前扫描结果
     * @return true 表示拒绝（过滤掉），false 表示接受（保留）
     */
    boolean reject(@NonNull ScanResult result);
}