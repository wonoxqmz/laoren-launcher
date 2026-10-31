package com.lcj.oldfriend;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 选择桌面上要显示哪些应用：带搜索，只放常用几个。 */
public class AppPickerDialog extends Dialog {

    public interface Callback { void onPicked(Set<String> pkgs); }

    private final Theme th;
    private final List<AppRepo.App> all;
    private final List<AppRepo.App> shown = new ArrayList<AppRepo.App>();
    private final Set<String> selected = new HashSet<String>();
    private final Adapter adapter;
    private Callback cb;

    public AppPickerDialog(Context c, Theme th, Set<String> pre) {
        super(c);
        this.th = th;
        this.all = AppRepo.load(c);
        this.shown.addAll(all);
        if (pre != null) selected.addAll(pre);

        int dp = (int) c.getResources().getDisplayMetrics().density;
        LinearLayout root = U.v(c);
        root.setBackground(U.solid(th.card, dp * 22));
        root.setPadding(dp * 16, dp * 16, dp * 16, dp * 14);

        TextView title = U.tv(c, "选择桌面上的应用", 22, th.text, true);
        root.addView(title, U.lp(-2, -2, 0));

        TextView hint = U.tv(c, "勾选常用的几个就够了，桌面越少越不容易点错", 13, th.sub, false);
        root.addView(hint, U.lpM(-1, -2, 0, dp * 5, dp * 10));

        // 搜索框
        final EditText search = new EditText(c);
        search.setHint("搜索应用名");
        search.setTextSize(17);
        search.setTextColor(th.text);
        search.setHintTextColor(th.sub);
        search.setSingleLine(true);
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setBackground(U.stroke(U.lighten(th.accent, 0.78f), dp * 12, dp * 2,
                U.lighten(th.accent, 0.5f)));
        search.setPadding(dp * 14, dp * 9, dp * 14, dp * 9);
        root.addView(search, U.lpM(-1, -2, 0, 0, dp * 10));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int cc) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int cc) {}
            @Override public void afterTextChanged(Editable e) { filter(e.toString()); }
        });

        ListView lv = new ListView(c);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        adapter = new Adapter(c, dp);
        lv.setAdapter(adapter);
        root.addView(lv, U.lp(-1, 0, 1f));

        TextView done = U.tv(c, "完 成", 20, 0xFFFFFFFF, true);
        done.setGravity(Gravity.CENTER);
        done.setBackground(U.solid(th.accent, dp * 14));
        done.setPadding(0, dp * 14, 0, dp * 14);
        done.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (cb != null) cb.onPicked(selected);
                dismiss();
            }
        });
        root.addView(done, U.lpM(-1, -2, 0, dp * 10, 0));

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(root, new ViewGroup.LayoutParams(-2, -2));
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams wl = w.getAttributes();
            int screenW = c.getResources().getDisplayMetrics().widthPixels;
            int screenH = c.getResources().getDisplayMetrics().heightPixels;
            wl.width = (int) (screenW * 0.92f);
            wl.height = (int) (screenH * 0.82f);
            w.setAttributes(wl);
        }
    }

    private void filter(String key) {
        String k = key == null ? "" : key.trim().toLowerCase(Locale.CHINA);
        shown.clear();
        if (k.length() == 0) {
            shown.addAll(all);
        } else {
            for (AppRepo.App a : all) {
                if (a.label != null && a.label.toLowerCase(Locale.CHINA).contains(k)) shown.add(a);
            }
        }
        adapter.notifyDataSetChanged();
    }

    public void setCallback(Callback cb) { this.cb = cb; }

    private class Adapter extends BaseAdapter {
        private final Context c;
        private final int dp;
        Adapter(Context c, int dp) { this.c = c; this.dp = dp; }

        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int i) { return shown.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override public View getView(final int i, View recycle, ViewGroup parent) {
            LinearLayout row = U.h(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp * 4, dp * 10, dp * 4, dp * 10);

            final AppRepo.App a = shown.get(i);

            ImageView iv = new ImageView(c);
            iv.setImageDrawable(a.icon);
            row.addView(iv, U.lp(dp * 40, dp * 40, 0));

            TextView name = U.tv(c, a.label, 17, th.text, false);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nlp = U.lp(0, -2, 1f);
            nlp.leftMargin = dp * 12;
            nlp.rightMargin = dp * 8;
            row.addView(name, nlp);

            boolean on = selected.contains(a.pkg);
            TextView mark = U.tv(c, on ? "已添加" : "添加", 14,
                    on ? 0xFFFFFFFF : th.accent, true);
            mark.setGravity(Gravity.CENTER);
            mark.setSingleLine(true);
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
