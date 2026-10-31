package com.lcj.oldfriend;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.Calendar;

/** 大字时钟卡：时间 + 日期 + 星期 + 电量，自绘，字号随卡片高度自适应。 */
public class TimeView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Theme th;
    private final float fontScale; // 与设置字号档联动的缩放
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            invalidate();
            h.postDelayed(this, 30 * 1000L);
        }
    };
    private final Runnable hourTick = new Runnable() {
        @Override public void run() {
            Calendar c = Calendar.getInstance();
            if (c.get(Calendar.MINUTE) == 0 && Prefs.ttsHour(getContext())) {
                Tts.speak(getContext(), "现在时间 " + c.get(Calendar.HOUR_OF_DAY) + " 点整");
            }
            h.postDelayed(this, 20 * 1000L);
        }
    };
    private static final String[] WEEK = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

    public TimeView(Context c, Theme th) {
        super(c);
        this.th = th;
        int[] fs = {96, 110, 122, 134};
        this.fontScale = fs[Prefs.font(c)] / 110f;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        h.post(tick);
        h.post(hourTick);
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        h.removeCallbacks(tick);
        h.removeCallbacks(hourTick);
    }

    @Override protected void onDraw(Canvas cv) {
        float w = getWidth(), hh = getHeight();
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        int week = c.get(Calendar.DAY_OF_WEEK) - 1;

        // 时间
        String time = String.format("%02d:%02d", hour, min);
        p.setColor(th.accent);
        p.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.BOLD));
        float tSize = Math.min(hh * 0.52f * fontScale, w * 0.24f);
        p.setTextSize(tSize);
        p.setTextAlign(Paint.Align.LEFT);
        float ty = hh * 0.60f;
        cv.drawText(time, w * 0.08f, ty, p);

        // 日期 + 星期
        p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        p.setColor(th.sub);
        float dSize = Math.min(hh * 0.13f, w * 0.055f);
        p.setTextSize(dSize);
        p.setTextAlign(Paint.Align.LEFT);
        cv.drawText(month + "月" + day + "日  " + WEEK[week], w * 0.085f, hh * 0.86f, p);

        // 电量（右上）
        int batt = battery();
        p.setTextAlign(Paint.Align.RIGHT);
        p.setTextSize(dSize * 0.92f);
        p.setColor(batt <= 20 ? 0xFFE5484D : th.sub);
        cv.drawText("电量 " + batt + "%", w * 0.92f, hh * 0.30f, p);
    }

    private int battery() {
        try {
            android.content.Intent i = getContext()
                    .registerReceiver(null, new android.content.IntentFilter(
                            android.content.Intent.ACTION_BATTERY_CHANGED));
            if (i != null) {
                int lv = i.getIntExtra("level", -1);
                int sc = i.getIntExtra("scale", 100);
                if (lv >= 0 && sc > 0) return lv * 100 / sc;
            }
        } catch (Exception ignored) {}
        return 100;
    }
}
