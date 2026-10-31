package com.lcj.oldfriend;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 小工具：尺寸换算、圆角背景、快捷建控件。 */
public final class U {
    private U() {}

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable solid(int color, int radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static GradientDrawable stroke(int color, int radiusPx, int strokePx, int strokeColor) {
        GradientDrawable d = solid(color, radiusPx);
        d.setStroke(strokePx, strokeColor);
        return d;
    }

    public static GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    public static TextView tv(Context c, CharSequence text, float sizeSp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        if (weight != 0) p.weight = weight;
        return p;
    }

    /** 带外边距的 LayoutParams —— 直接用于 addView，避免对未加入父容器的 View 取 getLayoutParams()（会 null）。 */
    public static LinearLayout.LayoutParams lpM(int w, int h, float weight, int top, int bottom) {
        LinearLayout.LayoutParams p = lp(w, h, weight);
        p.topMargin = top;
        p.bottomMargin = bottom;
        return p;
    }

    public static LinearLayout v(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout h(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    /** 把颜色变淡（往白靠）。a: 0~1，越大越淡。 */
    public static int lighten(int color, float a) {
        int r = Color.red(color), g = Color.green(color), b = Color.blue(color);
        r = Math.round(r + (255 - r) * a);
        g = Math.round(g + (255 - g) * a);
        b = Math.round(b + (255 - b) * a);
        return Color.rgb(r, g, b);
    }

    /** 把颜色压深（往黑靠）。a: 0~1。 */
    public static int darken(int color, float a) {
        return Color.rgb(Math.round(Color.red(color) * (1 - a)),
                Math.round(Color.green(color) * (1 - a)),
                Math.round(Color.blue(color) * (1 - a)));
    }

    /** 名字首字，用于无头像时的头像占位。 */
    public static String firstChar(String name) {
        if (name == null || name.length() == 0) return "?";
        return name.substring(0, 1).toUpperCase();
    }
}
