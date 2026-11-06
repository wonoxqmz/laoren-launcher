package com.lcj.oldfriend;

/**
 * 配色主题。
 * 默认「经典蓝」完全对齐参考版的视觉规格：主色 #1E90FF、卡片纯白、圆角 15dp。
 */
public class Theme {
    public final String name;
    public final int bg;      // 页面底色（渐变的下半段）
    public final int card;    // 卡片背景
    public final int accent;  // 主色（时间数字、按钮、底栏）
    public final int text;    // 主文字
    public final int sub;     // 次文字
    public final boolean dark;

    public Theme(String name, int bg, int card, int accent, int text, int sub, boolean dark) {
        this.name = name; this.bg = bg; this.card = card;
        this.accent = accent; this.text = text; this.sub = sub; this.dark = dark;
    }

    public static final Theme[] ALL = {
        new Theme("经典蓝",   0xFFEDEFF2, 0xFFFFFFFF, 0xFF1E90FF, 0xFF16202C, 0xFF5B6B7C, false),
        new Theme("暖杏晨光", 0xFFFDF3E7, 0xFFFFFFFF, 0xFFD98324, 0xFF3A2A18, 0xFF7A6650, false),
        new Theme("静谧森林", 0xFFEAF3EC, 0xFFFFFFFF, 0xFF2E7D52, 0xFF1C2B22, 0xFF4E6A58, false),
        new Theme("玫瑰奶茶", 0xFFFCEFF1, 0xFFFFFFFF, 0xFFC2687A, 0xFF33232A, 0xFF7A5C63, false),
        new Theme("雾湖晨光", 0xFFE9F1F6, 0xFFFFFFFF, 0xFF3C7A99, 0xFF1B2A33, 0xFF5A7180, false),
        new Theme("极简深色", 0xFF14161A, 0xFF1F232A, 0xFF4FA3FF, 0xFFF2F4F8, 0xFF9AA6B4, true),
    };

    public static Theme get(int i) {
        if (i < 0 || i >= ALL.length) i = 0;
        return ALL[i];
    }

    /** 底栏底色：参考版是 34% 不透明的主色（#561E90FF 就是 accent 带 0x56 透明度）。 */
    public int dock() {
        return ((dark ? 0xCC : 0x66) << 24) | (accent & 0x00FFFFFF);
    }

    /** 底栏上的图标颜色。 */
    public int onDock() {
        return 0xFFFFFFFF;
    }

    /** 卡片描边：参考版是 1dp 的 #4D414345。 */
    public int cardStroke() {
        return dark ? 0x33FFFFFF : 0x24000000;
    }
}
