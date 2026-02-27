package com.xabber.android.data.database.repositories;

import android.os.Looper;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.DeviceRealmObject;
import com.xabber.android.data.log.LogManager;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.realm.Realm;

public class DeviceRepository {

    private static final String LOG_TAG = DeviceRepository.class.getSimpleName();

    public static boolean hasDeviceInRealm() throws ExecutionException, InterruptedException {
        return Application.getInstance().runInBackground(() -> {
            Boolean result = null;
            try (Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance()) {
                result = !realm.where(DeviceRealmObject.class).findAll().isEmpty();
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
            return result;
        }, Boolean.class);
    }

    public static void saveOrUpdateDeviceToRealm(DeviceRealmObject device) {
        Application.getInstance().runInBackground(() -> {
            try (Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance()) {
                realm.executeTransaction(realm1 -> realm1.copyToRealmOrUpdate(device));
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    public static void removeDeviceFromRealm(String deviceId) {
        Application.getInstance().runInBackground(() -> {
            try (Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance()) {
                realm.executeTransaction(realm1 -> {
                    realm1.where(DeviceRealmObject.class)
                            .equalTo(DeviceRealmObject.Fields.ID, deviceId)
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
        });
    }

    public static void removeAllOtherDevices(String currentId) {
        Application.getInstance().runInBackground(() -> {
            try (Realm realm = DatabaseManager.getInstance().getDefaultRealmInstance()) {
                realm.executeTransaction(realm1 -> {
                    realm1.where(DeviceRealmObject.class)
                            .notEqualTo(DeviceRealmObject.Fields.ID, currentId)
                            .findAll()
                            .deleteAllFromRealm();
                });
            } catch (Exception e) {
                LogManager.exception(LOG_TAG, e);
            }
        });
    }
}
