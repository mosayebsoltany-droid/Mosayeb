package com.mose.assistant;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class MoseAccessibilityService extends AccessibilityService {
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // نسخه نخست فقط وضعیت صفحه را مشاهده می‌کند؛ اجرای هر اقدام در نسخه بعد
        // با فرمان روشن کاربر و کنترل‌های ایمنی اضافه می‌شود.
    }
    @Override public void onInterrupt() {}
}
