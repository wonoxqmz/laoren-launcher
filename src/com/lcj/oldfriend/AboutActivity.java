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

        addPara(card, th, "这是一个为长辈做的极简桌面：大字、大图标、大按钮。"
                + "把家人头像放上桌面，点一下就在微信里直接发起视频或语音通话。");
        addPara(card, th, "功能：大字时钟、联系人一键微信通话、自选常用应用、"
                + "语音播报名字、整点报时、桌面锁定、6 套适老配色、字号与圆角可调。");
        addPara(card, th, "微信通话靠系统「无障碍」服务完成：自动打开微信、搜索备注名、点进聊天、"
                + "点「+」、点视频或语音通话。不读取聊天内容，不联网，不上传任何数据。");
        addPara(card, th, "声明：本项目为独立开发的开源作品，与任何商业「老人桌面」应用无关；"
                + "微信界面升级可能导致自动点击失效，届时请反馈微信版本号。");

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
