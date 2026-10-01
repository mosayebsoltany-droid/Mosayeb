package com.mose.assistant;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class MoseNotificationService extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        // اعلان‌ها فعلاً محلی می‌مانند و بدون اجازه کاربر ارسال نمی‌شوند.
    }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) {}
}
