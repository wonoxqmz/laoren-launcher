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

/** 设置页：微信通话 / 桌面 / 外观 / 系统。 */
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
        build(); // 无障碍状态/主题可能变了，重建
    }

    private void build() {
        LinearLayout root = U.v(this);
        root.setBackgroundColor(th.bg);

        TextView back = U.tv(this, "＜ 返回", 17, th.sub, true);
        back.setPadding(dp(10), dp(12), dp(10), dp(8));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        root.addView(back, U.lp(-2, -2, 0));

        TextView title = U.tv(this, "设 置", 26, th.text, true);
        title.setPadding(dp(16), 0, dp(16), dp(8));
        root.addView(title, U.lp(-2, -2, 0));

        android.widget.ScrollView sc = new android.widget.ScrollView(this);
        list = U.v(this);
        list.setPadding(dp(14), dp(10), dp(14), dp(40));
        sc.addView(list, new ViewGroup.LayoutParams(-1, -2));
        root.addView(sc, U.lp(-1, 0, 1f));

        buildItems();
        setContentView(root, new ViewGroup.LayoutParams(-1, -1));
    }

    private LinearLayout card() {
        LinearLayout c = U.v(this);
        c.setBackground(U.solid(th.card, dp(20)));
        c.setPadding(dp(16), dp(4), dp(16), dp(4));
        LinearLayout.LayoutParams lp = U.lp(-1, -2, 0);
        lp.bottomMargin = dp(14);
        list.addView(c, lp);
        return c;
    }

    private void section(String t) {
        TextView tv = U.tv(this, t, 15, th.sub, true);
        LinearLayout.LayoutParams lp = U.lp(-2, -2, 0);
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(6);
        list.addView(tv, lp);
    }

    private LinearLayout row(String title) {
        LinearLayout r = U.h(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(13), 0, dp(13));
        r.addView(U.tv(this, title, 18, th.text, false), U.lp(0, -2, 1f));
        return r;
    }

    private void divider(LinearLayout card) {
        View d = new View(this);
        d.setBackgroundColor(th.dark ? U.darken(th.card, 0.3f) : 0xFFE9EDF2);
        card.addView(d, U.lp(-1, dp(1), 0));
    }

    private TextView valueText(LinearLayout r, String v) {
        return valueText(r, v, th.sub);
    }

    private TextView valueText(LinearLayout r, String v, int color) {
        TextView t = U.tv(this, v, 15, color, false);
        r.addView(t, U.lp(-2, -2, 0));
        return t;
    }

    private void buildItems() {
        // ---------- 微信通话 ----------
        section("微 信 通 话");
        LinearLayout c0 = card();

        boolean acc = WxCaller.accessibilityEnabled(this);
        boolean ready = WeChatService.isReady();
        LinearLayout rAcc = row("无障碍服务");
        valueText(rAcc, ready ? "已连接" : (acc ? "需重连" : "未开启"),
                ready ? 0xFF2E9E6B : 0xFFE5484D);
        rAcc.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { WxCaller.openAccessibilitySettings(SettingsActivity.this); }
        });
        c0.addView(rAcc);
        divider(c0);

        LinearLayout rWx = row("微信");
        valueText(rWx, WxCaller.wxInstalled(this) ? "已安装" : "未安装",
                WxCaller.wxInstalled(this) ? 0xFF2E9E6B : 0xFFE5484D);
        c0.addView(rWx);

        TextView accHint = U.tv(this,
                "开启「老友桌面微信通话」后，点联系人卡片会自动帮你在微信里找到 TA 并发起通话。\n"
                        + "微信升级后如果点不通，请到项目主页反馈微信版本。",
                13, th.sub, false);
        accHint.setLineSpacing(0, 1.3f);
        accHint.setPadding(0, dp(8), 0, dp(14));
        list.addView(accHint, U.lp(-1, -2, 0));

        // ---------- 桌面 ----------
        section("桌 面");
        LinearLayout c2 = card();

        LinearLayout rTts = row("点按播报名字");
        Switch swTts = syncSwitch(Prefs.ttsClick(this));
        swTts.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                Prefs.ttsClick(SettingsActivity.this, v);
            }
        });
        rTts.addView(swTts, U.lp(-2, -2, 0));
        c2.addView(rTts);
        divider(c2);

        LinearLayout rHour = row("整点报时");
        Switch swHour = syncSwitch(Prefs.ttsHour(this));
        swHour.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                Prefs.ttsHour(SettingsActivity.this, v);
            }
        });
        rHour.addView(swHour, U.lp(-2, -2, 0));
        c2.addView(rHour);
        divider(c2);

        LinearLayout rLock = row("锁定桌面");
        Switch swLock = syncSwitch(Prefs.locked(this));
        swLock.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                Prefs.locked(SettingsActivity.this, v);
                Toast.makeText(SettingsActivity.this, v ? "桌面已锁定，长按不再弹出菜单" : "桌面已解锁",
                        Toast.LENGTH_SHORT).show();
            }
        });
        rLock.addView(swLock, U.lp(-2, -2, 0));
        c2.addView(rLock);

        // ---------- 外观 ----------
        section("外 观");
        LinearLayout c1 = card();

        final LinearLayout rFont = row("字号大小");
        valueText(rFont, new String[]{"标准", "大", "更大", "超大"}[Prefs.font(this)]);
        rFont.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.font(SettingsActivity.this, (Prefs.font(SettingsActivity.this) + 1) % 4);
                build();
            }
        });
        c1.addView(rFont);
        divider(c1);

        final LinearLayout rCols = row("联系人每行");
        valueText(rCols, Prefs.cols(this) == 2 ? "2 个（大）" : "3 个（多）");
        rCols.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.cols(SettingsActivity.this, Prefs.cols(SettingsActivity.this) == 2 ? 3 : 2);
                build();
            }
        });
        c1.addView(rCols);
        divider(c1);

        final LinearLayout rRad = row("卡片圆角");
        valueText(rRad, new String[]{"小", "中", "大"}[Prefs.radius(this)]);
        rRad.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.radius(SettingsActivity.this, (Prefs.radius(SettingsActivity.this) + 1) % 3);
                build();
            }
        });
        c1.addView(rRad);

        LinearLayout cTheme = card();
        cTheme.addView(row("配色主题"));
        LinearLayout themesRow = U.h(this);
        for (int i = 0; i < Theme.ALL.length; i++) {
            final int idx = i;
            View sw = new View(this);
            if (i == Prefs.theme(this)) {
                sw.setBackground(U.stroke(Theme.ALL[i].accent, dp(13), dp(3), 0xFF333333));
            } else {
                sw.setBackground(U.solid(Theme.ALL[i].accent, dp(13)));
            }
            sw.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Prefs.theme(SettingsActivity.this, idx);
                    build();
                    Toast.makeText(SettingsActivity.this, "已选择「" + Theme.ALL[idx].name + "」",
                            Toast.LENGTH_SHORT).show();
                }
            });
            LinearLayout.LayoutParams lp = U.lp(0, dp(42), 1f);
            lp.setMargins(dp(4), dp(8), dp(4), dp(10));
            themesRow.addView(sw, lp);
        }
        cTheme.addView(themesRow, U.lp(-1, -2, 0));

        // ---------- 系统 ----------
        section("系 统");
        LinearLayout c4 = card();

        LinearLayout rAuto = row("自动检查更新");
        Switch swAuto = syncSwitch(Prefs.autoUpdate(this));
        swAuto.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton b, boolean v) {
                Prefs.autoUpdate(SettingsActivity.this, v);
            }
        });
        rAuto.addView(swAuto, U.lp(-2, -2, 0));
        c4.addView(rAuto);
        divider(c4);

        final LinearLayout rCheck = row("检查更新");
        final TextView vCheck = valueText(rCheck, "当前 v" + UpdateChecker.currentVersion(this));
        rCheck.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                vCheck.setText("检查中…");
                UpdateChecker.check(SettingsActivity.this, new UpdateChecker.Callback() {
                    @Override public void onResult(UpdateChecker.Info info) {
                        Prefs.lastCheck(SettingsActivity.this, System.currentTimeMillis());
                        if (info == null) {
                            vCheck.setText("v" + UpdateChecker.currentVersion(SettingsActivity.this) + " · 最新");
                            Toast.makeText(SettingsActivity.this, "已经是最新版本", Toast.LENGTH_SHORT).show();
                        } else {
                            vCheck.setText("发现 v" + info.version);
                            showUpdateDialog(info);
                        }
                    }

                    @Override public void onError() {
                        vCheck.setText("检查失败，请检查网络");
                    }
                });
            }
        });
        c4.addView(rCheck);
        divider(c4);

        LinearLayout rHome = row("设为默认桌面");
        valueText(rHome, "去设置 ＞");
        rHome.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                } catch (Exception e) {
                    try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Exception ignored) {}
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

    private void showUpdateDialog(final UpdateChecker.Info info) {
        String notes = info.notes == null ? "" : info.notes.trim();
        if (notes.length() > 400) notes = notes.substring(0, 400) + "…";
        if (notes.length() == 0) notes = "有新版本可以更新。";
        new android.app.AlertDialog.Builder(this)
                .setTitle("发现新版本 " + info.version)
                .setMessage(notes)
                .setPositiveButton("去下载", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        UpdateChecker.openDownload(SettingsActivity.this, info);
                    }
                })
                .setNegativeButton("以后再说", null)
                .create().show();
    }


    private Switch syncSwitch(boolean checked) {
        Switch s = new Switch(this);
        s.setChecked(checked);
        return s;
    }
}
