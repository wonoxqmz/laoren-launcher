package com.lcj.oldfriend;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** 底栏大图标：实心填充，压在半透明色块上也看得清（参考版底栏就是大图标）。 */
public class DockIcon extends View {
    public static final int CONTACTS = 0, APPS = 1, WECHAT = 2, PHONE = 3;

    private final int type;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int color = Color.WHITE;
    private boolean active = true;

    public DockIcon(Context c, int type) {
        super(c);
        this.type = type;
    }

    public void setColor(int c) { this.color = c; invalidate(); }

    /** 未选中的页：淡一点。 */
    public void setActive(boolean a) { this.active = a; invalidate(); }

    @Override protected void onDraw(Canvas cv) {
        float s = Math.min(getWidth(), getHeight()) / 48f;
        p.setColor(color);
        p.setAlpha(active ? 255 : 165);
        p.setStyle(Paint.Style.FILL);

        if (type == CONTACTS) {
            cv.drawCircle(24 * s, 15 * s, 8.6f * s, p);
            path.reset();
            path.moveTo(24 * s, 26 * s);
            path.addArc(new RectF(5 * s, 26 * s, 43 * s, 62 * s), 180, 180);
            path.close();
            cv.drawPath(path, p);

        } else if (type == APPS) {
            float r = 4.5f * s, side = 17 * s, gap = 4 * s;
            float x0 = 24 * s - (side + gap / 2f);
            float y0 = 24 * s - (side + gap / 2f);
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) {
                    float l = x0 + col * (side + gap);
                    float t = y0 + row * (side + gap);
                    cv.drawRoundRect(new RectF(l, t, l + side, t + side), r, r, p);
                }
            }

        } else if (type == WECHAT) {
            // 两个实心气泡，带小尾巴
            cv.drawOval(new RectF(3 * s, 8 * s, 32 * s, 31 * s), p);
            path.reset();
            path.moveTo(9 * s, 26 * s);
            path.lineTo(6 * s, 33 * s);
            path.lineTo(16 * s, 28 * s);
            path.close();
            cv.drawPath(path, p);

            cv.drawOval(new RectF(20 * s, 22 * s, 45 * s, 41 * s), p);
            path.reset();
            path.moveTo(39 * s, 37 * s);
            path.lineTo(43 * s, 43 * s);
            path.lineTo(32 * s, 39 * s);
            path.close();
            cv.drawPath(path, p);

            // 挖两个眼睛出来（用底色挖不了，改用半透明小孔）
            p.setColor(0x33000000);
            cv.drawCircle(13 * s, 19 * s, 2.0f * s, p);
            cv.drawCircle(23 * s, 19 * s, 2.0f * s, p);
            cv.drawCircle(29 * s, 31 * s, 1.8f * s, p);
            cv.drawCircle(37 * s, 31 * s, 1.8f * s, p);

        } else if (type == PHONE) {
            // 听筒
            path.reset();
            path.addArc(new RectF(6 * s, 6 * s, 22 * s, 26 * s), 150, 200);
            path.close();
            cv.drawPath(path, p);
            cv.drawRoundRect(new RectF(28 * s, 28 * s, 42 * s, 42 * s), 5 * s, 5 * s, p);
        }
    }
}
