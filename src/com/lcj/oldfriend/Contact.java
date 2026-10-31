package com.lcj.oldfriend;

import org.json.JSONObject;

/** 联系人数据：姓名、手机号（可选）、微信备注名、头像颜色、默认通话方式。 */
public class Contact {
    public static final int CALL_VIDEO = 0;
    public static final int CALL_VOICE = 1;

    public String name;
    public String number;   // 手机号（可选，仅记录）
    public String wxName;   // 微信备注名（微信里显示的名字，用于搜索）
    public int color;
    public String photo;    // 可选，本地图片路径
    public int callType = CALL_VIDEO;

    public Contact() {}

    public Contact(String name, String wxName, int color) {
        this.name = name;
        this.wxName = wxName;
        this.color = color;
    }

    /** 微信里搜索用的名字：优先备注名，否则用姓名。 */
    public String searchName() {
        return (wxName != null && wxName.trim().length() > 0) ? wxName.trim() : name;
    }

    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("number", number == null ? "" : number);
            o.put("wxName", wxName == null ? "" : wxName);
            o.put("color", color);
            o.put("callType", callType);
            if (photo != null) o.put("photo", photo);
            return o;
        } catch (Exception e) { return new JSONObject(); }
    }

    public static Contact from(JSONObject o) {
        Contact c = new Contact();
        c.name = o.optString("name", "");
        c.number = o.optString("number", "");
        c.wxName = o.optString("wxName", "");
        c.color = o.optInt("color", 0xFF1E90FF);
        c.callType = o.optInt("callType", CALL_VIDEO);
        c.photo = o.has("photo") ? o.optString("photo") : null;
        return c;
    }
}
