package com.lcj.oldfriend;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

/**
 * 头像视图，两种形态：
 * - 铺满模式（联系人卡片用）：整块圆角矩形铺照片，没照片就画色块 + 姓氏首字，和参考版一致
 * - 圆形模式（编辑对话框用）：圆形裁切
 */
public class AvatarView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private Contact contact;
    private Bitmap cache;       // 解码结果缓存，避免每帧 decodeFile
    private String cachePath;
    private boolean rect = true;
    private int corner = 0;     // px

    public AvatarView(Context c) { super(c); }

    public void setContact(Contact c) {
        this.contact = c;
        String p = (c == null || c.photo == null) ? null : c.photo;
        if (p == null ? cachePath != null : !p.equals(cachePath)) {
            cache = null;
            cachePath = null;
        }
        setContentDescription(c == null ? "" : c.name);
        invalidate();
    }

    /** true = 圆角矩形铺满（默认），false = 圆形。 */
    public void setRectMode(boolean rect) { this.rect = rect; invalidate(); }

    /** 圆角半径（px），仅铺满模式有效。 */
    public void setCorner(int px) { this.corner = px; invalidate(); }

    @Override protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        String name = contact == null ? "" : contact.name;
        int color = contact == null ? 0xFF1E90FF : contact.color;
        Bitmap bmp = loadPhoto();

        int save = cv.save();
        clip.reset();
        if (rect) {
            clip.addRoundRect(new RectF(0, 0, w, h), corner, corner, Path.Direction.CW);
        } else {
            float r = Math.min(w, h) / 2f;
            clip.addCircle(w / 2f, h / 2f, r, Path.Direction.CW);
        }
        cv.clipPath(clip);

        if (bmp != null) {
            // centerCrop 铺满
            float bw = bmp.getWidth(), bh = bmp.getHeight();
            float scale = Math.max(w / bw, h / bh);
            float dw = bw * scale, dh = bh * scale;
            float left = (w - dw) / 2f, top = (h - dh) / 2f;
            cv.drawBitmap(bmp, new Rect(0, 0, (int) bw, (int) bh),
                    new RectF(left, top, left + dw, top + dh), p);
        } else {
            // 没有照片：色块 + 首字（和参考版的纯色占位一致）
            p.setColor(color);
            cv.drawRect(0, 0, w, h, p);

            tp.setColor(0xFFFFFFFF);
            tp.setFakeBoldText(true);
            tp.setTextSize(h * 0.36f);
            tp.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = tp.getFontMetrics();
            float base = h / 2f - (fm.ascent + fm.descent) / 2f;
            cv.drawText(U.firstChar(name), w / 2f, base, tp);
        }
        cv.restoreToCount(save);
    }

    private Bitmap loadPhoto() {
        if (contact == null || contact.photo == null || contact.photo.length() == 0) return null;
        if (cache != null && contact.photo.equals(cachePath)) return cache;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(contact.photo, o);
            int sample = 1;
            while (o.outWidth / sample > 1024) sample *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            Bitmap b = BitmapFactory.decodeFile(contact.photo, o);
            if (b != null) {
                cache = b;
                cachePath = contact.photo;
            }
            return b;
        } catch (Exception e) { return null; }
    }
}
