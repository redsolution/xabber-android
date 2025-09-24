package com.xabber.android.ui.helper;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import com.xabber.android.data.Application;

public class PermissionsRequester {
    public static final int REQUEST_PERMISSION_GALLERY = 4;
    public static final int REQUEST_PERMISSION_CAMERA = 5;
    public static final int REQUEST_PERMISSION_DOWNLOAD_FILE = 24;
    public static final int REQUEST_PERMISSION_RECORD_AUDIO = 37;

    public static boolean requestFileReadPermissionIfNeeded(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
            }
            return checkAndRequestPermissions(permissions, activity, requestCode);
        } else {
            return checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, activity, requestCode);
        }
    }

    public static boolean requestFileReadPermissionIfNeeded(android.app.Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
            }
            return checkAndRequestPermissions(permissions, fragment, requestCode);
        } else {
            return checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, fragment, requestCode);
        }
    }

    public static boolean requestFileReadPermissionIfNeeded(Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
            }
            return checkAndRequestPermissions(permissions, fragment, requestCode);
        } else {
            return checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, fragment, requestCode);
        }
    }

    public static boolean requestFileWritePermissionIfNeeded(android.app.Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
            }
            return checkAndRequestPermissions(permissions, fragment, requestCode);
        } else {
            return checkAndRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, fragment, requestCode);
        }
    }

    public static boolean requestFileWritePermissionIfNeeded(Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
            }
            return checkAndRequestPermissions(permissions, fragment, requestCode);
        } else {
            return checkAndRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, fragment, requestCode);
        }
    }

    public static boolean requestFileWritePermissionIfNeeded(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
            }
            return checkAndRequestPermissions(permissions, activity, requestCode);
        } else {
            return checkAndRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, activity, requestCode);
        }
    }

    public static boolean requestCameraPermissionIfNeeded(android.app.Fragment fragment, int requestCode) {
        return checkAndRequestPermission(Manifest.permission.CAMERA, fragment, requestCode);
    }

    public static boolean requestCameraPermissionIfNeeded(Fragment fragment, int requestCode) {
        return checkAndRequestPermission(Manifest.permission.CAMERA, fragment, requestCode);
    }

    public static boolean requestCameraPermissionIfNeeded(Activity activity, int requestCode) {
        return checkAndRequestPermission(Manifest.permission.CAMERA, activity, requestCode);
    }

    public static boolean requestRecordAudioPermissionIfNeeded(android.app.Fragment fragment, int requestCode) {
        return checkAndRequestPermission(Manifest.permission.RECORD_AUDIO, fragment, requestCode);
    }

    public static boolean requestRecordAudioPermissionIfNeeded(Fragment fragment, int requestCode) {
        return checkAndRequestPermission(Manifest.permission.RECORD_AUDIO, fragment, requestCode);
    }

    public static boolean requestRecordAudioPermissionIfNeeded(Activity activity, int requestCode) {
        return checkAndRequestPermission(Manifest.permission.RECORD_AUDIO, activity, requestCode);
    }

    public static boolean requestLocationPermissionIfNeeded(Activity activity, int requestCode) {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        return checkAndRequestPermissions(permissions, activity, requestCode);
    }

    private static boolean checkAndRequestPermission(String permission, Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
        return false;
    }

    private static boolean checkAndRequestPermission(String permission, android.app.Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (ActivityCompat.checkSelfPermission(fragment.getActivity(), permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        fragment.requestPermissions(new String[]{permission}, requestCode);
        return false;
    }

    private static boolean checkAndRequestPermission(String permission, Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (ActivityCompat.checkSelfPermission(fragment.requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        fragment.requestPermissions(new String[]{permission}, requestCode);
        return false;
    }

    private static boolean checkAndRequestPermissions(String[] permissions, Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
            return false;
        }
        return true;
    }

    private static boolean checkAndRequestPermissions(String[] permissions, android.app.Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(fragment.getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            fragment.requestPermissions(permissions, requestCode);
            return false;
        }
        return true;
    }

    private static boolean checkAndRequestPermissions(String[] permissions, Fragment fragment, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(fragment.requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (!allGranted) {
            fragment.requestPermissions(permissions, requestCode);
            return false;
        }
        return true;
    }

    public static boolean hasFileReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkPermission(Manifest.permission.READ_MEDIA_IMAGES) ||
                    checkPermission(Manifest.permission.READ_MEDIA_VIDEO);
        } else {
            return checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    public static boolean hasCameraPermission() {
        return checkPermission(Manifest.permission.CAMERA);
    }

    public static boolean isPermissionGranted(int[] grantResults) {
        if (grantResults.length == 0) return false;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkPermission(String permission) {
        return ActivityCompat.checkSelfPermission(Application.getInstance(), permission) == PackageManager.PERMISSION_GRANTED;
    }
}