package com.lcj.oldfriend;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 适老化拨号盘：大按钮、按键播报、大字号码显示。 */
public class DialerActivity extends Activity {

    private final StringBuffer num = new StringBuffer();
    private TextView display;
    private String pendingCall;
    private Theme th;

    private static final String[][] KEYS = {
            {"1", ""}, {"2", "ABC"}, {"3", "DEF"},
            {"4", "GHI"}, {"5", "JKL"}, {"6", "MNO"},
            {"7", "PQRS"}, {"8", "TUV"}, {"9", "WXYZ"},
            {"*", ""}, {"0", "+"}, {"#", ""},
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        th = Theme.get(Prefs.theme(this));
        build();
    }

    private int dp(int v) { return U.dp(this, v); }

    private void build() {
        LinearLayout root = U.v(this);
        root.setBackgroundColor(th.bg);
        int pad = dp(14);
        root.setPadding(pad, pad, pad, pad);

        // 顶栏：返回 + 标题
        TextView back = U.tv(this, "＜ 返回", 18, th.sub, true);
        back.setPadding(dp(6), dp(6), dp(6), dp(6));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        root.addView(back, U.lp(-2, -2, 0));

        // 号码显示
        display = U.tv(this, "", 40, th.text, true);
        display.setGravity(Gravity.CENTER);
        display.setBackgroundResource(0);
        display.setBackground(U.solid(th.card, dp(20)));
        display.setPadding(dp(16), dp(22), dp(16), dp(22));
        display.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { backspace(); }
        });
        LinearLayout.LayoutParams dlp = U.lp(-1, -2, 0);
        dlp.topMargin = dp(8);
        root.addView(display, dlp);

        TextView hint = U.tv(this, "点显示区可删除末位", 14, th.sub, false);
        hint.setGravity(Gravity.CENTER);
        root.addView(hint, U.lp(-1, -2, 0));

        // 键盘 4 行
        for (int r = 0; r < 4; r++) {
            LinearLayout row = U.h(this);
            for (int c = 0; c < 3; c++) {
                final String[] k = KEYS[r * 3 + c];
                LinearLayout btn = U.v(this);
                btn.setGravity(Gravity.CENTER);
                btn.setBackground(U.solid(th.card, dp(18)));
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        num.append(k[0]);
                        Tts.speak(DialerActivity.this, cnDigit(k[0]));
                        update();
                    }
                });
                TextView big = U.tv(this, k[0], 32, th.text, true);
                big.setGravity(Gravity.CENTER);
                btn.addView(big, U.lp(-2, -2, 0));
                if (k[1].length() > 0) {
                    TextView small = U.tv(this, k[1], 12, th.sub, false);
                    small.setGravity(Gravity.CENTER);
                    btn.addView(small, U.lp(-2, -2, 0));
                }
                LinearLayout.LayoutParams blp = U.lp(0, dp(74), 1f);
                row.addView(btn, blp);
            }
            LinearLayout.LayoutParams rlp = U.lp(-1, -2, 0);
            rlp.topMargin = dp(10);
            root.addView(row, rlp);
        }

        // 操作行：清空 / 拨打
        LinearLayout ops = U.h(this);
        Button clear = new Button(this, null, 0);
        clear.setText("清空");
        clear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        clear.setTypeface(Typeface.DEFAULT_BOLD);
        clear.setTextColor(th.text);
        clear.setBackground(U.solid(th.card, dp(18)));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { num.setLength(0); update(); }
        });
        ops.addView(clear, U.lp(0, dp(76), 0.4f));

        Button call = new Button(this, null, 0);
        call.setText("拨 打");
        call.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        call.setTypeface(Typeface.DEFAULT_BOLD);
        call.setTextColor(Color.WHITE);
        call.setBackground(U.solid(0xFF2E9E6B, dp(18)));
        call.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { doCall(); }
        });
        LinearLayout.LayoutParams clp = U.lp(0, dp(76), 0.6f);
        clp.leftMargin = dp(12);
        ops.addView(call, clp);

        LinearLayout.LayoutParams olp = U.lp(-1, -2, 0);
        olp.topMargin = dp(12);
        root.addView(ops, olp);

        setContentView(root, new ViewGroup.LayoutParams(-1, -1));
    }

    private String cnDigit(String s) {
        if ("0123456789".contains(s)) {
            String[] cn = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
            return cn[Integer.parseInt(s)];
        }
        if ("*".equals(s)) return "星号";
        if ("#".equals(s)) return "井号";
        return s;
    }

    private void backspace() {
        if (num.length() > 0) {
            num.setLength(num.length() - 1);
            update();
        }
    }

    private void update() {
        display.setText(num.length() == 0 ? "输 入 号 码" : num.toString());
    }

    private void doCall() {
        if (num.length() == 0) {
            toast("请先输入号码");
            return;
        }
        String n = num.toString();
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED) {
            pendingCall = n;
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 31);
            return;
        }
        place(n);
    }

    private void place(String n) {
        try {
            startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + n)));
        } catch (Exception e) {
            try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + n))); }
            catch (Exception ignored) {}
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        super.onRequestPermissionsResult(code, p, r);
        if (code == 31 && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED
                && pendingCall != null) {
            place(pendingCall);
        } else if (code == 31) {
            toast("没有拨号权限，改为跳转系统拨号确认");
            if (pendingCall != null) {
                try {
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + pendingCall)));
                } catch (Exception ignored) {}
            }
        }
        pendingCall = null;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
