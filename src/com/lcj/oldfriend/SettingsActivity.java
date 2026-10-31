package com.lcj.oldfriend;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** 设置页：字号 / 列数 / 圆角 / 主题 / 行为 / 安全 / 系统。 */
public class SettingsActivity extends Activity {

    private LinearLayout list;
    private Theme th;

    private int dp(int v) { return U.dp(this, v); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        th = Theme.get(Prefs.theme(this));
        build();
    }

    @Override protected void onResume() {
        super.onResume();
        th = Theme.get(Prefs.theme(this));
        build(); // 主题切换后重建
    }

    private void build() {
        LinearLayout root = U.v(this);
        root.setBackgroundColor(th.bg);

        // 顶栏
        TextView back = U.tv(this, "＜ 返回", 18, th.sub, true);
        back.setPadding(dp(8), dp(12), dp(8), dp(12));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        root.addView(back, U.lp(-2, -2, 0));

        TextView title = U.tv(this, "设 置", 28, th.text, true);
        title.setPadding(dp(18), dp(4), dp(18), dp(10));
        root.addView(title, U.lp(-2, -2, 0));

        android.widget.ScrollView sc = new android.widget.ScrollView(this);
        list = U.v(this);
        int pad = dp(14);
        list.setPadding(pad, pad, pad, dp(40));
        sc.addView(list, new ViewGroup.LayoutParams(-1, -2));
        root.addView(sc, U.lp(-1, 0, 1f));

        buildItems();
        setContentView(root, new ViewGroup.LayoutParams(-1, -1));
    }

    private LinearLayout card() {
        LinearLayout c = U.v(this);
        c.setBackground(U.solid(th.card, dp(20)));
        c.setPadding(dp(18), dp(6), dp(18), dp(6));
        LinearLayout.LayoutParams lp = U.lp(-1, -2, 0);
        lp.bottomMargin = dp(14);
        list.addView(c, lp);
        return c;
    }

    private void section(String t) {
        TextView tv = U.tv(this, t, 17, th.sub, true);
        LinearLayout.LayoutParams lp = U.lp(-2, -2, 0);
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(6);
        list.addView(tv, lp);
    }

    private LinearLayout row(String title) {
        LinearLayout r = U.h(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(14), 0, dp(14));
        TextView t = U.tv(this, title, 20, th.text, false);
        r.addView(t, U.lp(0, -2, 1f));
        return r;
    }

    private void divider(LinearLayout card) {
        View d = new View(this);
        d.setBackgroundColor(th.dark ? U.darken(th.card, 0.3f) : 0xFFE8ECF1);
        card.addView(d, U.lp(-1, dp(1), 0));
    }

    private TextView valueText(LinearLayout r, String v) {
        TextView t = U.tv(this, v, 17, th.sub, false);
        LinearLayout.LayoutParams lp = U.lp(-2, -2, 0);
        r.addView(t, lp);
        return t;
    }

    private void buildItems() {
        // ---------- 外观 ----------
        section("外 观");

        // 字号
        LinearLayout c1 = card();
        final LinearLayout rFont = row("字号大小");
        final TextView vFont = valueText(rFont, new String[]{"标准", "大", "更大", "超大"}[Prefs.font(this)]);
        rFont.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.font(SettingsActivity.this, (Prefs.font(SettingsActivity.this) + 1) % 4);
                vFont.setText(new String[]{"标准", "大", "更大", "超大"}[Prefs.font(SettingsActivity.this)]);
                build(); // 联动圆角等显示
            }
        });
        c1.addView(rFont);
        divider(c1);

        // 列数
        final LinearLayout rCols = row("联系人每行个数");
        final TextView vCols = valueText(rCols, Prefs.cols(this) == 2 ? "2 个（大图标）" : "3 个（更多）");
        rCols.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.cols(SettingsActivity.this, Prefs.cols(SettingsActivity.this) == 2 ? 3 : 2);
                vCols.setText(Prefs.cols(SettingsActivity.this) == 2 ? "2 个（大图标）" : "3 个（更多）");
            }
        });
        c1.addView(rCols);
        divider(c1);

        // 圆角
        final LinearLayout rRad = row("卡片圆角");
        final TextView vRad = valueText(rRad, new String[]{"小", "中", "大"}[Prefs.radius(this)]);
        rRad.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.radius(SettingsActivity.this, (Prefs.radius(SettingsActivity.this) + 1) % 3);
                vRad.setText(new String[]{"小", "中", "大"}[Prefs.radius(SettingsActivity.this)]);
            }
        });
        c1.addView(rRad);

        // 主题
        LinearLayout cTheme = card();
        cTheme.addView(row("配色主题"));
        LinearLayout themesRow = U.h(this);
        for (int i = 0; i < Theme.ALL.length; i++) {
            final int idx = i;
            View sw = new View(this);
            sw.setBackground(U.solid(Theme.ALL[i].accent, dp(14)));
            if (i == Prefs.theme(this)) {
                sw.setBackground(U.stroke(Theme.ALL[i].accent, dp(14), dp(3),
                        th.dark ? 0xFFFFFFFF : 0xFF333333));
            }
            sw.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Prefs.theme(SettingsActivity.this, idx);
                    build();
                    Toast.makeText(SettingsActivity.this,
                            "已选择「" + Theme.ALL[idx].name + "」", Toast.LENGTH_SHORT).show();
                }
            });
            LinearLayout.LayoutParams lp = U.lp(0, dp(44), 1f);
            lp.setMargins(dp(5), dp(10), dp(5), dp(10));
            themesRow.addView(sw, lp);
        }
        cTheme.addView(themesRow, U.lp(-1, -2, 0));

        // ---------- 桌面 ----------
        section("桌 面");
        LinearLayout c2 = card();

        LinearLayout rApps = row("显示应用页");
        Switch swApps = makeSwitch(Prefs.showApps(this));
        swApps.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                Prefs.showApps(SettingsActivity.this, v);
            }
        });
        rApps.addView(swApps, U.lp(-2, -2, 0));
        c2.addView(rApps);
        divider(c2);

        LinearLayout rTts = row("点按播报名字");
        TextView hintTts = U.tv(this, "点击应用或联系人时语音提醒", 14, th.sub, false);
        hintTts.setPadding(0, 0, 0, dp(10));
        Switch swTts = makeSwitch(Prefs.ttsClick(this));
        swTts.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                Prefs.ttsClick(SettingsActivity.this, v);
            }
        });
        rTts.addView(swTts, U.lp(-2, -2, 0));
        c2.addView(rTts);
        c2.addView(hintTts);
        divider(c2);

        LinearLayout rHour = row("整点报时");
        Switch swHour = makeSwitch(Prefs.ttsHour(this));
        swHour.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                Prefs.ttsHour(SettingsActivity.this, v);
            }
        });
        rHour.addView(swHour, U.lp(-2, -2, 0));
        c2.addView(rHour);
        divider(c2);

        LinearLayout rConfirm = row("拨号前确认");
        Switch swConfirm = makeSwitch(Prefs.callConfirm(this));
        swConfirm.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                Prefs.callConfirm(SettingsActivity.this, v);
            }
        });
        rConfirm.addView(swConfirm, U.lp(-2, -2, 0));
        c2.addView(rConfirm);

        // ---------- 安全 ----------
        section("安 全");
        LinearLayout c3 = card();
        LinearLayout rLock = row("锁定桌面");
        TextView hintLock = U.tv(this, "锁定后长按不再弹出编辑菜单", 14, th.sub, false);
        hintLock.setPadding(0, 0, 0, dp(10));
        Switch swLock = makeSwitch(Prefs.locked(this));
        swLock.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                Prefs.locked(SettingsActivity.this, v);
                Toast.makeText(SettingsActivity.this, v ? "桌面已锁定" : "桌面已解锁",
                        Toast.LENGTH_SHORT).show();
            }
        });
        rLock.addView(swLock, U.lp(-2, -2, 0));
        c3.addView(rLock);
        c3.addView(hintLock);

        // ---------- 系统 ----------
        section("系 统");
        LinearLayout c4 = card();
        LinearLayout rHome = row("设为默认桌面");
        valueText(rHome, "去设置 ＞");
        rHome.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                } catch (Exception e) {
                    try {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    } catch (Exception ignored) {}
                }
            }
        });
        c4.addView(rHome);
        divider(c4);

        LinearLayout rAbout = row("关于老友桌面");
        valueText(rAbout, "＞");
        rAbout.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, AboutActivity.class));
            }
        });
        c4.addView(rAbout);
    }

    private Switch makeSwitch(boolean checked) {
        Switch s = new Switch(this);
        s.setChecked(checked);
        return s;
    }
}
