package com.lcj.oldfriend;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 联系人存储。 */
public class Store {
    private static final String KEY = "contacts";

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

    /** 头像底色盘：高辨识度、彼此区分明显。 */
    public static final int[] PALETTE = {
            0xFF1E90FF, 0xFFE5734A, 0xFF2E9E6B, 0xFF9B6BD6,
            0xFFD6527C, 0xFF3BA3C9, 0xFFC9A227, 0xFF6B7FD6,
    };

    public static int pickColor(int index) {
        return PALETTE[Math.abs(index) % PALETTE.length];
    }
}
