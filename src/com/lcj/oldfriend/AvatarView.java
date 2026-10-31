package com.lcj.oldfriend;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** 圆形头像：有照片显示照片，没有就显示姓氏首字 + 彩色底。 */
public class AvatarView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Contact contact;
    private Bitmap cache;       // 解码结果缓存，避免每帧 decodeFile
    private String cachePath;

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

    @Override protected void onDraw(Canvas cv) {
        float s = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = s / 2f;
        String name = contact == null ? "" : contact.name;

        Bitmap bmp = loadPhoto();
        if (bmp != null) {
            float bs = bmp.getWidth() < bmp.getHeight() ? bmp.getWidth() : bmp.getHeight();
            RectF srcR = new RectF((bmp.getWidth() - bs) / 2f, (bmp.getHeight() - bs) / 2f,
                    (bmp.getWidth() + bs) / 2f, (bmp.getHeight() + bs) / 2f);
            RectF dstR = new RectF(cx - r, cy - r, cx + r, cy + r);
            int save = cv.save();
            cv.clipPath(makeCircle(cx, cy, r));
            cv.drawBitmap(bmp, toRect(srcR), dstR, p);
            cv.restoreToCount(save);
            return;
        }

        int color = contact == null ? 0xFF1E90FF : contact.color;
        p.setColor(color);
        cv.drawCircle(cx, cy, r, p);

        tp.setColor(0xFFFFFFFF);
        tp.setFakeBoldText(true);
        tp.setTextSize(s * 0.42f);
        tp.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = tp.getFontMetrics();
        float base = cy - (fm.ascent + fm.descent) / 2f;
        cv.drawText(U.firstChar(name), cx, base, tp);
    }

    private android.graphics.Path makeCircle(float cx, float cy, float r) {
        android.graphics.Path path = new android.graphics.Path();
        path.addCircle(cx, cy, r, android.graphics.Path.Direction.CW);
        return path;
    }

    private static android.graphics.Rect toRect(RectF f) {
        return new android.graphics.Rect((int) f.left, (int) f.top,
                (int) f.right, (int) f.bottom);
    }

    private Bitmap loadPhoto() {
        if (contact == null || contact.photo == null || contact.photo.length() == 0) return null;
        if (cache != null && contact.photo.equals(cachePath)) return cache;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(contact.photo, o);
            int sample = 1;
            while (o.outWidth / sample > 512) sample *= 2;
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
