package com.lcj.oldfriend;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** 自绘矢量图标：联系人 / 应用 / 电话 / 设置，粗线条高辨识，适老。 */
public class IconView extends View {
    public static final int CONTACTS = 0, APPS = 1, PHONE = 2, SETTINGS = 3, SOS = 4;

    private final int type;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color = Color.WHITE;

    public IconView(Context c, int type) {
        super(c);
        this.type = type;
    }

    public void setColor(int color) { this.color = color; invalidate(); }

    @Override protected void onDraw(Canvas cv) {
        float s = Math.min(getWidth(), getHeight()) / 48f; // 设计坐标 48x48
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4f * s);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(color);

        if (type == CONTACTS) {
            // 圆头 + 肩
            cv.drawCircle(24 * s, 16 * s, 9 * s, p);
            cv.drawArc(8 * s, 30 * s, 40 * s, 60 * s, 180, 180, false, p);
        } else if (type == APPS) {
            // 3x3 圆点
            p.setStyle(Paint.Style.FILL);
            for (int r = 0; r < 3; r++)
                for (int col = 0; col < 3; col++)
                    cv.drawCircle((12 + col * 12) * s, (12 + r * 12) * s, 4.2f * s, p);
        } else if (type == PHONE) {
            // 听筒话机
            cv.drawRoundRect(6 * s, 12 * s, 42 * s, 40 * s, 6 * s, 6 * s, p);
            p.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 3; i++)
                cv.drawCircle((13 + i * 11) * s, 26 * s, 2.6f * s, p);
            p.setStyle(Paint.Style.STROKE);
            cv.drawLine(15 * s, 12 * s, 22 * s, 4 * s, p);
            cv.drawLine(33 * s, 12 * s, 26 * s, 4 * s, p);
        } else if (type == SETTINGS) {
            // 齿轮
            p.setStyle(Paint.Style.STROKE);
            cv.drawCircle(24 * s, 24 * s, 10 * s, p);
            p.setStrokeWidth(5f * s);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                float x1 = 24 * s + (float) Math.cos(a) * 11 * s;
                float y1 = 24 * s + (float) Math.sin(a) * 11 * s;
                float x2 = 24 * s + (float) Math.cos(a) * 17 * s;
                float y2 = 24 * s + (float) Math.sin(a) * 17 * s;
                cv.drawLine(x1, y1, x2, y2, p);
            }
        } else if (type == SOS) {
            p.setStyle(Paint.Style.STROKE);
            cv.drawRoundRect(4 * s, 8 * s, 44 * s, 40 * s, 8 * s, 8 * s, p);
            p.setStyle(Paint.Style.FILL);
            p.setTextSize(18f * s);
            p.setTextAlign(Paint.Align.CENTER);
            cv.drawText("SOS", 24 * s, 30 * s, p);
        }
    }
}
