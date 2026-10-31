package com.lcj.oldfriend;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

/** 联系人数据：姓名、号码、头像颜色，JSON 存 SharedPreferences。 */
public class Contact {
    public String name;
    public String number;
    public int color;   // 头像底色
    public String photo; // 可选，本地图片路径

    public Contact() {}

    public Contact(String name, String number, int color) {
        this.name = name; this.number = number; this.color = color;
    }

    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("number", number);
            o.put("color", color);
            if (photo != null) o.put("photo", photo);
            return o;
        } catch (Exception e) { return new JSONObject(); }
    }

    public static Contact from(JSONObject o) {
        Contact c = new Contact();
        c.name = o.optString("name", "");
        c.number = o.optString("number", "");
        c.color = o.optInt("color", 0xFF1E90FF);
        c.photo = o.has("photo") ? o.optString("photo") : null;
        return c;
    }
}
