package com.lcj.oldfriend;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 联系人添加 / 编辑。
 * 核心是「微信备注名」—— 微信里显示的名字，用来在微信里搜索到这个人。
 */
public class ContactEditDialog extends Dialog {

    public interface Callback {
        void onSaved(Contact c);
        void onDelete();
    }

    private final Theme th;
    private final Contact c;
    private Callback cb;
    private Runnable pickPhone;

    private final AvatarView avatar;
    private final EditText name;
    private final EditText wxName;
    private final EditText number;
    private int colorIdx;
    private int callType;

    public ContactEditDialog(Context ctx, Theme th, Contact origin, int colorSeed) {
        super(ctx);
        this.th = th;
        this.c = origin == null ? new Contact() : origin;
        if (origin == null) {
            this.colorIdx = colorSeed;
            this.c.color = Store.pickColor(colorSeed);
            this.callType = Contact.CALL_VIDEO;
        } else {
            this.colorIdx = 0;
            for (int i = 0; i < Store.PALETTE.length; i++)
                if (Store.PALETTE[i] == origin.color) { this.colorIdx = i; break; }
            this.callType = origin.callType;
        }

        int dp = (int) ctx.getResources().getDisplayMetrics().density;
        LinearLayout root = U.v(ctx);
        root.setBackground(U.solid(th.card, dp * 22));
        root.setPadding(dp * 20, dp * 18, dp * 20, dp * 18);

        TextView title = U.tv(ctx, origin == null ? "添加联系人" : "编辑联系人", 22, th.text, true);
        root.addView(title, U.lp(-2, -2, 0));

        avatar = new AvatarView(ctx);
        avatar.setContact(c);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                colorIdx = (colorIdx + 1) % Store.PALETTE.length;
                c.color = Store.PALETTE[colorIdx];
                avatar.setContact(c);
            }
        });
        LinearLayout avRow = U.h(ctx);
        avRow.setGravity(Gravity.CENTER);
        avRow.addView(avatar, U.lp(dp * 84, dp * 84, 0));
        root.addView(avRow, U.lpM(-1, -2, 0, dp * 12, 0));

        TextView colorHint = U.tv(ctx, "点头像换颜色", 13, th.sub, false);
        colorHint.setGravity(Gravity.CENTER);
        root.addView(colorHint, U.lp(-1, -2, 0));

        name = input(ctx, "姓名（如：女儿）", false, dp);
        name.setText(c.name);
        root.addView(name, U.lpM(-1, -2, 0, dp * 14, 0));

        wxName = input(ctx, "微信备注名（微信里显示的名字）", false, dp);
        wxName.setText(c.wxName);
        root.addView(wxName, U.lpM(-1, -2, 0, dp * 10, 0));

        TextView wxHint = U.tv(ctx, "填了才能在微信里自动找到 TA；留空就用姓名", 13, th.sub, false);
        root.addView(wxHint, U.lpM(-1, -2, 0, dp * 4, 0));

        number = input(ctx, "手机号（可不填）", true, dp);
        number.setText(c.number);
        root.addView(number, U.lpM(-1, -2, 0, dp * 10, 0));

        TextView ctLabel = U.tv(ctx, "点这个人时：", 15, th.sub, true);
        root.addView(ctLabel, U.lpM(-1, -2, 0, dp * 12, dp * 6));

        LinearLayout seg = U.h(ctx);
        final TextView segVideo = segBtn(ctx, "视频通话", dp);
        final TextView segVoice = segBtn(ctx, "语音通话", dp);
        segVideo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { callType = Contact.CALL_VIDEO; paintSeg(segVideo, segVoice); }
        });
        segVoice.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { callType = Contact.CALL_VOICE; paintSeg(segVideo, segVoice); }
        });
        seg.addView(segVideo, U.lp(0, -2, 1f));
        LinearLayout.LayoutParams slp2 = U.lp(0, -2, 1f);
        slp2.leftMargin = dp * 10;
        seg.addView(segVoice, slp2);
        root.addView(seg, U.lp(-1, -2, 0));
        paintSeg(segVideo, segVoice);

        TextView fromPhone = U.tv(ctx, "从手机通讯录找", 16, th.accent, true);
        fromPhone.setGravity(Gravity.CENTER);
        fromPhone.setBackground(U.solid(U.lighten(th.accent, 0.87f), dp * 12));
        fromPhone.setPadding(0, dp * 11, 0, dp * 11);
        fromPhone.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (pickPhone != null) pickPhone.run();
            }
        });
        root.addView(fromPhone, U.lpM(-1, -2, 0, dp * 14, 0));

        LinearLayout btns = U.h(ctx);
        Button cancel = btn(ctx, "取消", th.text, U.lighten(th.accent, 0.82f), dp);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dismiss(); }
        });
        btns.addView(cancel, U.lp(0, -2, 1f));

        if (origin != null) {
            Button del = btn(ctx, "删除", 0xFFE5484D, U.lighten(th.accent, 0.82f), dp);
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (cb != null) cb.onDelete();
                    dismiss();
                }
            });
            LinearLayout.LayoutParams lp = U.lp(0, -2, 1f);
            lp.leftMargin = dp * 10;
            btns.addView(del, lp);
        }

        Button save = btn(ctx, "保 存", Color.WHITE, th.accent, dp);
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String n = name.getText().toString().trim();
                if (n.length() == 0) { toast("请填写姓名"); return; }
                c.name = n;
                c.wxName = wxName.getText().toString().trim();
                c.number = number.getText().toString().trim();
                c.callType = callType;
                if (cb != null) cb.onSaved(c);
                dismiss();
            }
        });
        LinearLayout.LayoutParams slp = U.lp(0, -2, 1f);
        slp.leftMargin = dp * 10;
        btns.addView(save, slp);
        root.addView(btns, U.lpM(-1, -2, 0, dp * 16, 0));

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(root, new android.view.ViewGroup.LayoutParams(-2, -2));
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams wl = w.getAttributes();
            int screenW = ctx.getResources().getDisplayMetrics().widthPixels;
            wl.width = Math.min(screenW - dp * 24, dp * 340);
            w.setAttributes(wl);
        }
        setCanceledOnTouchOutside(false);
    }

    private void paintSeg(TextView video, TextView voice) {
        boolean v = callType == Contact.CALL_VIDEO;
        int dp = (int) getContext().getResources().getDisplayMetrics().density;
        video.setBackground(U.solid(v ? th.accent : U.lighten(th.accent, 0.88f), dp * 12));
        video.setTextColor(v ? Color.WHITE : th.sub);
        voice.setBackground(U.solid(!v ? th.accent : U.lighten(th.accent, 0.88f), dp * 12));
        voice.setTextColor(!v ? Color.WHITE : th.sub);
    }

    private TextView segBtn(Context ctx, String s, int dp) {
        TextView t = U.tv(ctx, s, 16, th.sub, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp * 12, 0, dp * 12);
        return t;
    }

    private EditText input(Context ctx, String hint, boolean numeric, int dp) {
        EditText e = new EditText(ctx);
        e.setHint(hint);
        e.setTextSize(19);
        e.setTextColor(th.text);
        e.setHintTextColor(th.sub);
        e.setSingleLine(true);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_PHONE);
        e.setBackground(U.stroke(U.lighten(th.accent, 0.78f), dp * 12, dp * 2, U.lighten(th.accent, 0.5f)));
        e.setPadding(dp * 14, dp * 11, dp * 14, dp * 11);
        return e;
    }

    private Button btn(Context ctx, String s, int textColor, int bg, int dp) {
        Button b = new Button(ctx, null, 0);
        b.setText(s);
        b.setTextSize(17);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(textColor);
        b.setBackground(U.solid(bg, dp * 12));
        b.setPadding(0, dp * 11, 0, dp * 11);
        return b;
    }

    private void toast(String s) {
        android.widget.Toast.makeText(getContext(), s, android.widget.Toast.LENGTH_SHORT).show();
    }

    public void setCallback(Callback cb) { this.cb = cb; }
    public void setOnPickFromPhone(Runnable r) { this.pickPhone = r; }

    /** 从系统通讯录选完后填回输入框。 */
    public void fill(String n, String num) {
        if (n != null) {
            name.setText(n);
            if (wxName.getText().toString().trim().length() == 0) wxName.setText(n);
        }
        if (num != null && num.length() > 0) number.setText(num);
    }
}
