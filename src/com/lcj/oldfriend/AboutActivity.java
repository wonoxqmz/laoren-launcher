package com.lcj.oldfriend;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 关于页。 */
public class AboutActivity extends Activity {

    private int dp(int v) { return U.dp(this, v); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Theme th = Theme.get(Prefs.theme(this));

        LinearLayout root = U.v(this);
        root.setBackgroundColor(th.bg);
        int pad = dp(20);

        TextView back = U.tv(this, "＜ 返回", 18, th.sub, true);
        back.setPadding(dp(8), dp(12), dp(8), dp(12));
        back.setOnClickListener(new android.view.View.OnClickListener() {
            @Override public void onClick(android.view.View v) { finish(); }
        });
        root.addView(back, U.lp(-2, -2, 0));

        LinearLayout col = U.v(this);
        col.setPadding(pad, dp(30), pad, pad);
        col.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView logo = U.tv(this, "老友桌面", 34, th.accent, true);
        col.addView(logo, U.lp(-2, -2, 0));

        TextView ver = U.tv(this, "版本 1.0.0 · 开源免费", 16, th.sub, false);
        LinearLayout.LayoutParams vlp = U.lp(-2, -2, 0);
        vlp.topMargin = dp(8);
        col.addView(ver, vlp);

        LinearLayout card = U.v(this);
        card.setBackground(U.solid(th.card, dp(20)));
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        LinearLayout.LayoutParams clp = U.lp(-1, -2, 0);
        clp.topMargin = dp(24);
        col.addView(card, clp);

        addPara(card, th, "这是一个为长辈做的极简桌面：大字、大图标、大按钮，"
                + "常用联系人和应用一眼就能找到，点一下就能打电话。");
        addPara(card, th, "功能：大字时钟、联系人一键拨号、适老拨号盘、"
                + "应用大图标页、语音播报、整点报时、桌面锁定、多套适老配色。");
        addPara(card, th, "本项目完全开源、免费、无广告、不联网、不上传任何数据，"
                + "联系人只保存在本机。");
        addPara(card, th, "声明：本项目为独立开发的开源作品，与任何商业「老人桌面」"
                + "应用无关；自动接听微信、远程协助等需要无障碍权限的功能不在此范围内。");

        setContentView(root, new ViewGroup.LayoutParams(-1, -1));
    }

    private void addPara(LinearLayout card, Theme th, String s) {
        TextView t = U.tv(this, s, 17, th.text, false);
        t.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams lp = U.lp(-1, -2, 0);
        lp.bottomMargin = dp(12);
        card.addView(t, lp);
    }
}
