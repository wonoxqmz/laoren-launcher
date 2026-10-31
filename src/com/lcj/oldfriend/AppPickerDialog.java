package com.lcj.oldfriend;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 选择桌面上要显示哪些应用（只放常用几个，不全放）。 */
public class AppPickerDialog extends Dialog {

    public interface Callback { void onPicked(Set<String> pkgs); }

    private final Theme th;
    private final List<AppRepo.App> apps;
    private final Set<String> selected = new HashSet<String>();
    private final Adapter adapter;
    private Callback cb;

    public AppPickerDialog(Context c, Theme th, Set<String> pre) {
        super(c);
        this.th = th;
        this.apps = AppRepo.load(c);
        if (pre != null) selected.addAll(pre);

        int dp = (int) c.getResources().getDisplayMetrics().density;
        LinearLayout root = U.v(c);
        root.setBackground(U.solid(th.card, dp * 22));
        int pad = dp * 16;
        root.setPadding(pad, pad, pad, pad);

        TextView title = U.tv(c, "选择桌面上的应用", 22, th.text, true);
        root.addView(title, U.lp(-2, -2, 0));

        TextView hint = U.tv(c, "勾选常用的几个就够了，桌面越少越不容易点错", 14, th.sub, false);
        LinearLayout.LayoutParams hlp = U.lp(-1, -2, 0);
        hlp.topMargin = dp * 6;
        hlp.bottomMargin = dp * 10;
        root.addView(hint, hlp);

        ListView lv = new ListView(c);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        adapter = new Adapter(c, dp);
        lv.setAdapter(adapter);
        root.addView(lv, U.lp(-1, 0, 1f));

        TextView done = U.tv(c, "完 成", 20, Color.WHITE, true);
        done.setGravity(Gravity.CENTER);
        done.setBackground(U.solid(th.accent, dp * 14));
        done.setPadding(0, dp * 14, 0, dp * 14);
        done.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (cb != null) cb.onPicked(selected);
                dismiss();
            }
        });
        LinearLayout.LayoutParams dlp = U.lp(-1, -2, 0);
        dlp.topMargin = dp * 10;
        root.addView(done, dlp);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(root, new ViewGroup.LayoutParams(-2, -2));
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams wl = w.getAttributes();
            int screenW = c.getResources().getDisplayMetrics().widthPixels;
            int screenH = c.getResources().getDisplayMetrics().heightPixels;
            wl.width = Math.min(screenW - dp * 28, dp * 380);
            wl.height = (int) (screenH * 0.78f);
            w.setAttributes(wl);
        }
    }

    public void setCallback(Callback cb) { this.cb = cb; }

    private class Adapter extends BaseAdapter {
        private final Context c;
        private final int dp;
        Adapter(Context c, int dp) { this.c = c; this.dp = dp; }

        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int i) { return apps.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override public View getView(final int i, View recycle, ViewGroup parent) {
            LinearLayout row = U.h(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp * 6, dp * 10, dp * 6, dp * 10);

            final AppRepo.App a = apps.get(i);

            ImageView iv = new ImageView(c);
            iv.setImageDrawable(a.icon);
            row.addView(iv, U.lp(dp * 40, dp * 40, 0));

            TextView name = U.tv(c, a.label, 18, th.text, false);
            LinearLayout.LayoutParams nlp = U.lp(0, -2, 1f);
            nlp.leftMargin = dp * 12;
            row.addView(name, nlp);

            boolean on = selected.contains(a.pkg);
            TextView mark = U.tv(c, on ? "已添加" : "添加", 15,
                    on ? Color.WHITE : th.accent, true);
            mark.setGravity(Gravity.CENTER);
            mark.setBackground(U.solid(on ? th.accent : U.lighten(th.accent, 0.85f), dp * 10));
            mark.setPadding(dp * 12, dp * 6, dp * 12, dp * 6);
            row.addView(mark, U.lp(-2, -2, 0));

            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (selected.contains(a.pkg)) selected.remove(a.pkg);
                    else selected.add(a.pkg);
                    notifyDataSetChanged();
                }
            });
            U.tapFx(row);
            return row;
        }
    }
}
