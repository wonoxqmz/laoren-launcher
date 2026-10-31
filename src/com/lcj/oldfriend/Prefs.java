package com.lcj.oldfriend;

import android.content.Context;
import android.content.SharedPreferences;

/** 全局配置。随时写盘（apply 异步），不怕被杀。 */
public class Prefs {
    private static SharedPreferences sp;

    private static SharedPreferences sp(Context c) {
        if (sp == null) sp = c.getApplicationContext().getSharedPreferences("cfg", 0);
        return sp;
    }

    // ---------- 键 ----------
    private static final String K_FONT   = "font";    // 0标准 1大 2更大 3超大
    private static final String K_COLS   = "cols";    // 联系人列数 2 / 应用列数 4 或 3
    private static final String K_RADIUS = "radius";  // 0小 1中 2大
    private static final String K_THEME  = "theme";
    private static final String K_LOCKED = "locked";
    private static final String K_TTS_CLICK = "tts_click"; // 点按播报
    private static final String K_TTS_HOUR  = "tts_hour";  // 整点报时
    private static final String K_SHOW_APPS = "show_apps"; // 底栏显示应用页
    private static final String K_CALL_CONFIRM = "call_confirm"; // 拨号前确认
    private static final String K_AUTO_UPDATE = "auto_update";   // 自动检查更新
    private static final String K_LAST_CHECK = "last_check";     // 上次检查更新时间

    public static int font(Context c)        { return sp(c).getInt(K_FONT, 1); }
    public static void font(Context c, int v){ sp(c).edit().putInt(K_FONT, v).apply(); }

    public static int cols(Context c)         { return sp(c).getInt(K_COLS, 2); }
    public static void cols(Context c, int v) { sp(c).edit().putInt(K_COLS, v).apply(); }

    public static int radius(Context c)         { return sp(c).getInt(K_RADIUS, 1); }
    public static void radius(Context c, int v) { sp(c).edit().putInt(K_RADIUS, v).apply(); }

    public static int theme(Context c)         { return sp(c).getInt(K_THEME, 0); }
    public static void theme(Context c, int v) { sp(c).edit().putInt(K_THEME, v).apply(); }

    public static boolean locked(Context c)          { return sp(c).getBoolean(K_LOCKED, false); }
    public static void locked(Context c, boolean v)  { sp(c).edit().putBoolean(K_LOCKED, v).apply(); }

    public static boolean ttsClick(Context c)         { return sp(c).getBoolean(K_TTS_CLICK, true); }
    public static void ttsClick(Context c, boolean v) { sp(c).edit().putBoolean(K_TTS_CLICK, v).apply(); }

    public static boolean ttsHour(Context c)          { return sp(c).getBoolean(K_TTS_HOUR, false); }
    public static void ttsHour(Context c, boolean v)  { sp(c).edit().putBoolean(K_TTS_HOUR, v).apply(); }

    public static boolean showApps(Context c)         { return sp(c).getBoolean(K_SHOW_APPS, true); }
    public static void showApps(Context c, boolean v) { sp(c).edit().putBoolean(K_SHOW_APPS, v).apply(); }

    public static boolean callConfirm(Context c)         { return sp(c).getBoolean(K_CALL_CONFIRM, false); }
    public static void callConfirm(Context c, boolean v) { sp(c).edit().putBoolean(K_CALL_CONFIRM, v).apply(); }

    public static boolean autoUpdate(Context c)         { return sp(c).getBoolean(K_AUTO_UPDATE, true); }
    public static void autoUpdate(Context c, boolean v) { sp(c).edit().putBoolean(K_AUTO_UPDATE, v).apply(); }

    public static long lastCheck(Context c)         { return sp(c).getLong(K_LAST_CHECK, 0); }
    public static void lastCheck(Context c, long v) { sp(c).edit().putLong(K_LAST_CHECK, v).apply(); }

    /** 联系人名字字号（跟随字号档）。标准档 24sp，对应原版。 */
    public static float nameSp(Context c) {
        float[] arr = {20f, 24f, 28f, 34f};
        return arr[font(c)];
    }

    public static float appSp(Context c) {
        float[] arr = {13f, 15f, 17f, 20f};
        return arr[font(c)];
    }

    public static float radiusPx(Context c) {
        float[] arr = {10f, 20f, 30f};
        return U.dp(c, arr[radius(c)]);
    }
}
