package com.lcj.oldfriend;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 检查更新：读 Gitee 最新 release，比对版本号。
 * 只做「提示 + 打开发布页」，不在后台偷偷下 APK —— 老人机流量和权限都省着点。
 */
public class UpdateChecker {

    private static final String API =
            "https://gitee.com/api/v5/repos/i-dont-want-to-pick-a-name/laoren-launcher/releases/latest";
    private static final String PAGE =
            "https://gitee.com/i-dont-want-to-pick-a-name/laoren-launcher/releases";

    public static class Info {
        public String version;   // 2.1.0
        public String notes;     // 更新说明
        public String pageUrl;   // 发布页
        public String apkUrl;    // 直接下载 APK
    }

    public interface Callback {
        /** newer == null 表示已是最新。 */
        void onResult(Info newer);
        /** 网络/解析失败。 */
        void onError();
    }

    public static String currentVersion(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return pi.versionName;
        } catch (Exception e) { return "0"; }
    }

    /** 后台线程请求，结果回主线程。 */
    public static void check(final Context ctx, final Callback cb) {
        final Context c = ctx.getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                Info info = null;
                boolean ok;
                try {
                    info = fetch(c);
                    ok = true;
                } catch (Throwable t) {
                    ok = false;
                }
                final Info fi = info;
                final boolean fok = ok;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override public void run() {
                        if (fok) cb.onResult(fi);
                        else cb.onError();
                    }
                });
            }
        }).start();
    }

    private static Info fetch(Context c) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(API);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "OldFriendLauncher");
            if (conn.getResponseCode() != 200) throw new Exception("http " + conn.getResponseCode());

            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();

            JSONObject o = new JSONObject(sb.toString());
            String tag = o.optString("tag_name", "");
            String latest = tag.startsWith("v") ? tag.substring(1) : tag;
            if (latest.length() == 0) return null;

            if (compare(latest, currentVersion(c)) <= 0) return null; // 已是最新

            Info info = new Info();
            info.version = latest;
            info.notes = o.optString("body", "");
            info.pageUrl = o.optString("html_url", PAGE);

            JSONArray assets = o.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject a = assets.getJSONObject(i);
                    String name = a.optString("name", "");
                    if (name.toLowerCase().endsWith(".apk")) {
                        info.apkUrl = a.optString("browser_download_url", "");
                        break;
                    }
                }
            }
            return info;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** 1.2.10 > 1.2.9；返回 >0 表示 a 新。 */
    public static int compare(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        int n = Math.max(x.length, y.length);
        for (int i = 0; i < n; i++) {
            int xi = i < x.length ? toInt(x[i]) : 0;
            int yi = i < y.length ? toInt(y[i]) : 0;
            if (xi != yi) return xi > yi ? 1 : -1;
        }
        return 0;
    }

    private static int toInt(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return 0; }
    }

    /** 打开浏览器下载（有直链优先直链，否则打开发布页）。 */
    public static void openDownload(Context c, Info info) {
        String url = (info.apkUrl != null && info.apkUrl.length() > 0) ? info.apkUrl : info.pageUrl;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(PAGE));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
            } catch (Exception ignored) {}
        }
    }
}
