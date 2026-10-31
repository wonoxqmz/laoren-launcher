package com.lcj.oldfriend;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/** 微信通话入口：检查微信/无障碍，然后交给 WeChatService 自动化。 */
public class WxCaller {

    public static final String WX_PKG = "com.tencent.mm";

    public static boolean wxInstalled(Context c) {
        try {
            return c.getPackageManager().getLaunchIntentForPackage(WX_PKG) != null;
        } catch (Exception e) { return false; }
    }

    public static boolean openWeChat(Context c) {
        try {
            Intent i = c.getPackageManager().getLaunchIntentForPackage(WX_PKG);
            if (i == null) return false;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            c.startActivity(i);
            return true;
        } catch (Exception e) { return false; }
    }

    /** 跳到系统无障碍设置（不同 ROM 页面不同，逐级兜底）。 */
    public static void openAccessibilitySettings(Context c) {
        try {
            Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            android.widget.Toast.makeText(c,
                    "请在列表里找到「老友桌面微信通话」并开启", android.widget.Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            try {
                Intent i = new Intent(Settings.ACTION_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
            } catch (Exception ignored) {}
        }
    }

    /** 无障碍服务是否已在系统里勾选（不一定已连接，服务连上后 isReady 才为 true）。 */
    public static boolean accessibilityEnabled(Context c) {
        try {
            String flat = Settings.Secure.getString(c.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (flat == null) return false;
            String me = new ComponentName(c, WeChatService.class).flattenToString();
            return flat.contains(me) || flat.contains(c.getPackageName() + "/.WeChatService");
        } catch (Exception e) { return false; }
    }

    public static void call(Context c, Contact ct) {
        WeChatService.call(c, ct);
    }
}
