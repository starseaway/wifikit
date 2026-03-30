package com.xinyi.wifikit.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于检测并申请运行时的定位权限
 *
 * @author 新一
 * @date 2025/5/29 10:40
 */
public class LocationPermission {

    /**
     * 定位权限
     */
    private static final String[] PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };

    /**
     * 权限申请请求码
     */
    private static final int REQUEST_CODE = 1001;

    /**
     * 检查并申请定位相关权限
     *
     * @param activity 当前 Activity
     * @param callback 全部权限授予后的回调
     */
    public static void checkAndRequestPermissions(Activity activity, PermissionCallback callback) {
        List<String> denied = new ArrayList<>();
        for (String perm : PERMISSIONS) {
            // 未授予的权限
            if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                denied.add(perm);
            }
        }
        if (denied.isEmpty()) {
            callback.onGranted();
        } else {
            ActivityCompat.requestPermissions(activity,
                denied.toArray(new String[0]),
                REQUEST_CODE
            );
        }
    }

    /**
     * 判断位置服务（GPS）是否开启
     *
     * @param context 上下文
     * @return true 已开启，false 未开启
     */
    public static boolean isLocationEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return (locationManager != null && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)));
    }

    /**
     * 权限申请回调接口
     */
    public interface PermissionCallback {

        /**
         * 全部权限授予后的回调
         */
        void onGranted();
    }
}