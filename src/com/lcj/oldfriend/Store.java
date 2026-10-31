package com.lcj.oldfriend;

import org.json.JSONArray;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 存储：联系人 + 桌面常用应用。 */
public class Store {
    private static final String KEY = "contacts";
    private static final String KEY_APPS = "home_apps";

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("contacts", 0);
    }

    public static List<Contact> load(Context c) {
        List<Contact> out = new ArrayList<Contact>();
        try {
            JSONArray a = new JSONArray(sp(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(Contact.from(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        return out;
    }

    public static void save(Context c, List<Contact> list) {
        try {
            JSONArray a = new JSONArray();
            for (Contact ct : list) a.put(ct.toJson());
            sp(c).edit().putString(KEY, a.toString()).apply();
        } catch (Exception ignored) {}
    }

    // ---------- 桌面常用应用（只放用户自己挑的） ----------

    public static Set<String> loadHomeApps(Context c) {
        Set<String> out = new HashSet<String>();
        try {
            JSONArray a = new JSONArray(sp(c).getString(KEY_APPS, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
        } catch (Exception ignored) {}
        return out;
    }

    public static void saveHomeApps(Context c, Set<String> pkgs) {
        JSONArray a = new JSONArray();
        for (String p : pkgs) a.put(p);
        sp(c).edit().putString(KEY_APPS, a.toString()).apply();
    }

    /** 头像底色盘：高辨识度、彼此区分明显。 */
    public static final int[] PALETTE = {
            0xFF1E90FF, 0xFFE5734A, 0xFF2E9E6B, 0xFF9B6BD6,
            0xFFD6527C, 0xFF3BA3C9, 0xFFC9A227, 0xFF6B7FD6,
    };

    public static int pickColor(int index) {
        return PALETTE[Math.abs(index) % PALETTE.length];
    }
}
