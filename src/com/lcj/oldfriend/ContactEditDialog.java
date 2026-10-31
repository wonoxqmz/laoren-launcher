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
 * 联系人添加 / 编辑对话框（全代码构建，大输入框适老）。
 * 点头像循环换颜色。
 */
public class ContactEditDialog extends Dialog {

    public interface Callback {
        void onSaved(Contact c);
        void onDelete();
    }

    private final Theme th;
    private final Contact c;
    private final int colorSeed;
    private Callback cb;
    private Runnable pickPhone;

    private final AvatarView avatar;
    private final EditText name;
    private final EditText number;
    private final TextView deleteBtn;
    private int colorIdx;

    public ContactEditDialog(Context ctx, Theme th, Contact origin, int colorSeed) {
        super(ctx);
        this.th = th;
        this.colorSeed = colorSeed;
        this.c = origin == null ? new Contact() : origin;
        if (origin == null) {
            this.colorIdx = colorSeed;
            this.c.color = Store.pickColor(colorSeed);
        } else {
            this.colorIdx = -1;
            for (int i = 0; i < Store.PALETTE.length; i++)
                if (Store.PALETTE[i] == origin.color) { this.colorIdx = i; break; }
            if (this.colorIdx < 0) this.colorIdx = colorSeed;
        }

        int dp = (int) getContext().getResources().getDisplayMetrics().density;
        LinearLayout root = U.v(getContext());
        root.setBackground(U.solid(th.card, dp * 24));
        int pad = dp * 22;
        root.setPadding(pad, pad, pad, pad);

        TextView title = U.tv(getContext(), origin == null ? "添加联系人" : "编辑联系人", 24, th.text, true);
        root.addView(title, U.lp(-2, -2, 0));

        // 头像预览（点击换色）
        avatar = new AvatarView(getContext());
        avatar.setContact(c);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                colorIdx = (colorIdx + 1) % Store.PALETTE.length;
                c.color = Store.PALETTE[colorIdx];
                avatar.setContact(c);
            }
        });
        LinearLayout avRow = U.h(getContext());
        avRow.setGravity(Gravity.CENTER);
        avRow.addView(avatar, U.lp(dp * 96, dp * 96, 0));
        LinearLayout.LayoutParams avRowLp = U.lp(-1, -2, 0);
        avRowLp.topMargin = dp * 16;
        root.addView(avRow, avRowLp);

        TextView colorHint = U.tv(getContext(), "点头像换颜色", 14, th.sub, false);
        colorHint.setGravity(Gravity.CENTER);
        root.addView(colorHint, U.lp(-1, -2, 0));

        // 姓名
        name = makeInput("姓名", false);
        name.setText(c.name);
        root.addView(name, inputLp(dp));

        // 号码
        number = makeInput("电话号码", true);
        number.setText(c.number);
        root.addView(number, inputLp(dp));

        // 从通讯录导入
        TextView fromPhone = U.tv(getContext(), "从手机通讯录选择", 18, th.accent, true);
        fromPhone.setGravity(Gravity.CENTER);
        fromPhone.setBackground(U.solid(U.lighten(th.accent, 0.85f), dp * 12));
        fromPhone.setPadding(0, dp * 12, 0, dp * 12);
        fromPhone.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (pickPhone != null) pickPhone.run();
            }
        });
        LinearLayout.LayoutParams fpLp = U.lp(-1, -2, 0);
        fpLp.topMargin = dp * 16;
        root.addView(fromPhone, fpLp);

        // 按钮
        LinearLayout btns = U.h(getContext());
        int btnPad = dp * 14;
        Button cancel = bigBtn("取消");
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dismiss(); }
        });
        btns.addView(cancel, U.lp(0, -2, 1f));

        if (origin != null) {
            deleteBtn = bigBtn("删除");
            deleteBtn.setTextColor(0xFFE5484D);
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (cb != null) cb.onDelete();
                    dismiss();
                }
            });
            btns.addView(deleteBtn, U.lp(0, -2, 1f));
        } else {
            deleteBtn = null;
        }

        Button save = bigBtn("保 存");
        save.setTextColor(Color.WHITE);
        save.setBackground(U.solid(th.accent, dp * 12));
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String n = name.getText().toString().trim();
                String num = number.getText().toString().trim();
                if (n.length() == 0) { toast("请填写姓名"); return; }
                if (num.length() == 0) { toast("请填写电话号码"); return; }
                c.name = n;
                c.number = num;
                if (cb != null) cb.onSaved(c);
                dismiss();
            }
        });
        btns.addView(save, U.lp(0, -2, 1f));

        LinearLayout.LayoutParams btnsLp = U.lp(-1, -2, 0);
        btnsLp.topMargin = dp * 20;
        root.addView(btns, btnsLp);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(root, new android.view.ViewGroup.LayoutParams(-2, -2));
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams wl = w.getAttributes();
            int screenW = getContext().getResources().getDisplayMetrics().widthPixels;
            wl.width = Math.min(screenW - dpv(24), dpv(340)); // 小屏不溢出
            w.setAttributes(wl);
        }
        setCanceledOnTouchOutside(false);
    }

    private LinearLayout.LayoutParams inputLp(int dp) {
        LinearLayout.LayoutParams p = U.lp(-1, -2, 0);
        p.topMargin = dp * 14;
        return p;
    }

    private EditText makeInput(String hint, boolean numeric) {
        EditText e = new EditText(getContext());
        e.setHint(hint);
        e.setTextSize(22);
        e.setTextColor(th.text);
        e.setHintTextColor(th.sub);
        e.setSingleLine(true);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_PHONE);
        int dp = (int) getContext().getResources().getDisplayMetrics().density;
        e.setBackground(U.stroke(th.dark ? U.darken(th.card, 0.3f) : U.lighten(th.accent, 0.8f),
                dp * 12, dp * 2, U.lighten(th.accent, 0.5f)));
        e.setPadding(dp * 14, dp * 12, dp * 14, dp * 12);
        return e;
    }

    private Button bigBtn(String s) {
        Button b = new Button(getContext(), null, 0);
        b.setText(s);
        b.setTextSize(18);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(th.text);
        int dp = (int) getContext().getResources().getDisplayMetrics().density;
        b.setBackground(U.solid(th.dark ? U.darken(th.card, 0.3f) : U.lighten(th.accent, 0.8f), dp * 12));
        b.setPadding(0, btnPad(), 0, btnPad());
        return b;
    }

    private int btnPad() {
        return dpv(12);
    }

    private int dpv(int v) {
        return (int) getContext().getResources().getDisplayMetrics().density * v;
    }

    private void toast(String s) {
        android.widget.Toast.makeText(getContext(), s, android.widget.Toast.LENGTH_SHORT).show();
    }

    public void setCallback(Callback cb) { this.cb = cb; }
    public void setOnPickFromPhone(Runnable r) { this.pickPhone = r; }

    /** 从系统通讯录选完后，把结果填回当前输入框。 */
    public void fill(String n, String num) {
        if (n != null) name.setText(n);
        if (num != null && num.length() > 0) number.setText(num);
    }
}
