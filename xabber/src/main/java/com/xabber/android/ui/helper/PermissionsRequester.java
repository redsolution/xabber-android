package com.xabber.android.ui.helper;

import android.Manifest;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import com.xabber.android.data.Application;

public class PermissionsRequester {

    public static boolean requestFileReadPermissionIfNeeded(Fragment fragment, int requestCode) {
        return checkAndRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, fragment, requestCode);
    }

    public static boolean requestFileWritePermissionIfNeeded(Fragment fragment, int requestCode) {
        return checkAndRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, fragment, requestCode);
    }

    public static boolean requestCameraPermissionIfNeeded(Fragment fragment, int requestCode) {
        return checkAndRequestPermission(Manifest.permission.CAMERA, fragment, requestCode);
    }

    private static boolean checkAndRequestPermission(String permission, Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (checkPermission(permission)) {
            return true;
        } else {
            fragment.requestPermissions(new String[]{permission}, requestCode);
        }
        return false;
    }

    public static boolean isPermissionGranted(int[] grantResults) {
        return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasFileReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            return true;
        }
    }

    public static boolean hasFileWritePermission() {
        return checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private static boolean checkPermission(String permission) {
        final int permissionCheck = ContextCompat.checkSelfPermission(Application.getInstance(), permission);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

}
