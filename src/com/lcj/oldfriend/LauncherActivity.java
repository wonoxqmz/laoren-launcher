package com.lcj.oldfriend;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 主桌面（HOME）。
 * 页0：大字时间卡 + 联系人（点一下直接打微信语音/视频）
 * 页1：自己挑的常用应用
 * 底栏：联系人 / 应用 / 微信（设置从时间卡右上角齿轮进）
 */
public class LauncherActivity extends Activity {

    private static final int REQ_PICK_CONTACT = 12;
    private static final int REQ_PICK_PHOTO = 14;
    private static final int REQ_SETTINGS = 13;

    private Theme th;
    private ViewFlipper flipper;
    private LinearLayout contactRows;
    private LinearLayout appRows;
    private LinearLayout bottomBar;
    private List<Contact> contacts;
    private Set<String> homeApps;
    private ContactEditDialog lastDialog;
    private int lastThemeIdx = -1;
    private float downX, downY;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        th = Theme.get(Prefs.theme(this));
        lastThemeIdx = Prefs.theme(this);
        Tts.init(this);
        buildUi();
        refresh();
        maybeCheckUpdate();
    }

    @Override protected void onResume() {
        super.onResume();
        int t = Prefs.theme(this);
        th = Theme.get(t);
        if (t != lastThemeIdx) { // 换主题后整体重建
            lastThemeIdx = t;
            buildUi();
        }
        refresh();
    }

    @Override public void onBackPressed() {
        if (flipper.getDisplayedChild() != 0) setPage(0);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX(); downY = ev.getY(); break;
            case MotionEvent.ACTION_UP: {
                float dx = ev.getX() - downX, dy = ev.getY() - downY;
                float lim = U.dp(this, 110);
                if (Math.abs(dx) > lim && Math.abs(dy) < U.dp(this, 45)) {
                    if (dx < 0 && flipper.getDisplayedChild() == 0) setPage(1);
                    else if (dx > 0 && flipper.getDisplayedChild() == 1) setPage(0);
                }
                break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    // ================= UI =================
    // 视觉规格按参考版量出来的值走：
    //   联系人卡片 140dp 高、圆角 15dp、白底 + 1dp 描边、照片铺满、名字 24sp 白字压渐变
    //   底栏 75dp 高、圆角 15dp、34% 透明主色、大图标无文字
    //   应用格 2 列、52dp 圆角图标 + 13sp 名字

    private int dockH;

    private void buildUi() {
        dockH = U.dp(this, 75);
        int edge = U.dp(this, 12);

        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{U.lighten(th.accent, th.dark ? 0.84f : 0.93f), th.bg});
        root.setBackground(bg);

        flipper = new ViewFlipper(this);
        flipper.addView(buildContactPage());
        flipper.addView(buildAppPage());
        android.widget.FrameLayout.LayoutParams flp =
                new android.widget.FrameLayout.LayoutParams(-1, -1);
        flp.bottomMargin = dockH + edge * 2;
        root.addView(flipper, flp);

        bottomBar = buildBottomBar();
        android.widget.FrameLayout.LayoutParams blp =
                new android.widget.FrameLayout.LayoutParams(-1, dockH);
        blp.gravity = Gravity.BOTTOM;
        blp.leftMargin = edge;
        blp.rightMargin = edge;
        blp.bottomMargin = edge;
        root.addView(bottomBar, blp);

        setContentView(root, new ViewGroup.LayoutParams(-1, -1));
    }

    private View buildContactPage() {
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = U.v(this);
        col.setPadding(U.dp(this, 12), U.dp(this, 12), U.dp(this, 12), U.dp(this, 12));
        sc.addView(col, new ViewGroup.LayoutParams(-1, -2));

        // 大字时间卡（参考版第一屏最显眼的就是它）
        android.widget.FrameLayout timeCard = new android.widget.FrameLayout(this);
        timeCard.setBackground(U.stroke(th.card, U.dp(this, 15), U.dp(this, 1), th.cardStroke()));
        TimeView tv = new TimeView(this, th);
        timeCard.addView(tv, new android.widget.FrameLayout.LayoutParams(-1, U.dp(this, 128)));

        // 设置入口：右上角齿轮（不占底栏位置）
        IconView gear = new IconView(this, IconView.SETTINGS);
        gear.setColor(th.sub);
        gear.setPadding(U.dp(this, 9), U.dp(this, 9), U.dp(this, 9), U.dp(this, 9));
        gear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivityForResult(new Intent(LauncherActivity.this, SettingsActivity.class), REQ_SETTINGS);
            }
        });
        timeCard.addView(gear, new android.widget.FrameLayout.LayoutParams(
                U.dp(this, 36), U.dp(this, 36), Gravity.TOP | Gravity.END));

        col.addView(timeCard, U.lpM(-1, U.dp(this, 128), 0, 0, U.dp(this, 12)));

        contactRows = U.v(this);
        col.addView(contactRows, U.lp(-1, -2, 0));
        return sc;
    }

    private View buildAppPage() {
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = U.v(this);
        col.setPadding(U.dp(this, 12), U.dp(this, 14), U.dp(this, 12), U.dp(this, 12));

        LinearLayout head = U.h(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = U.tv(this, "常用应用", 22, th.text, true);
        head.addView(title, U.lp(0, -2, 1f));
        TextView edit = U.tv(this, "编辑", 15, th.accent, true);
        edit.setPadding(U.dp(this, 14), U.dp(this, 7), U.dp(this, 14), U.dp(this, 7));
        edit.setBackground(U.solid(U.lighten(th.accent, 0.87f), U.dp(this, 12)));
        edit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickApps(); }
        });
        head.addView(edit, U.lp(-2, -2, 0));
        col.addView(head, U.lpM(-1, -2, 0, U.dp(this, 4), U.dp(this, 12)));

        appRows = U.v(this);
        col.addView(appRows, U.lp(-1, -2, 0));
        sc.addView(col, new ViewGroup.LayoutParams(-1, -2));
        return sc;
    }

    private LinearLayout buildBottomBar() {
        LinearLayout bar = U.h(this);
        bar.setGravity(Gravity.CENTER);
        bar.setBackground(U.solid(th.dock(), U.dp(this, 15)));
        if (Build.VERSION.SDK_INT >= 21) bar.setElevation(U.dp(this, 6));
        rebuildBottomBar(bar);
        return bar;
    }

    private void rebuildBottomBar(LinearLayout bar) {
        if (bar == null) return;
        bar.removeAllViews();
        addItem(bar, DockIcon.CONTACTS, 0, "家人", new View.OnClickListener() {
            @Override public void onClick(View v) { setPage(0); }
        });
        addItem(bar, DockIcon.APPS, 1, "应用", new View.OnClickListener() {
            @Override public void onClick(View v) { setPage(1); }
        });
        addItem(bar, DockIcon.WECHAT, -1, "微信", new View.OnClickListener() {
            @Override public void onClick(View v) {
                Tts.click(LauncherActivity.this, "微信");
                if (!WxCaller.openWeChat(LauncherActivity.this)) toast("没装微信");
            }
        });
    }

    private void addItem(LinearLayout bar, int type, int page, String label, View.OnClickListener oc) {
        boolean active = page >= 0 && flipper != null && flipper.getDisplayedChild() == page;
        android.widget.FrameLayout it = new android.widget.FrameLayout(this);
        it.setOnClickListener(oc);
        it.setContentDescription(label);

        DockIcon ic = new DockIcon(this, type);
        ic.setColor(th.onDock());
        ic.setActive(active);
        int side = U.dp(this, active ? 40 : 36);
        android.widget.FrameLayout.LayoutParams ilp =
                new android.widget.FrameLayout.LayoutParams(side, side, Gravity.CENTER);
        it.addView(ic, ilp);

        U.tapFx(it);
        bar.addView(it, U.lp(0, -1, 1f));
    }

    private void setPage(int i) {
        if (flipper.getDisplayedChild() == i) return;
        flipper.setDisplayedChild(i);
        rebuildBottomBar(bottomBar);
    }

    // ================= 数据 =================

    private void refresh() {
        contacts = Store.load(this);
        homeApps = Store.loadHomeApps(this);
        fillContacts();
        fillApps();
        rebuildBottomBar(bottomBar);
    }

    private void fillContacts() {
        if (contactRows == null) return;
        contactRows.removeAllViews();
        int n = contacts.size();
        int gap = U.dp(this, 8);

        if (n == 0) {
            LinearLayout empty = U.v(this);
            empty.setGravity(Gravity.CENTER);
            empty.setBackground(U.stroke(th.card, U.dp(this, 15), U.dp(this, 1), th.cardStroke()));
            empty.setPadding(U.dp(this, 20), U.dp(this, 34), U.dp(this, 20), U.dp(this, 34));
            TextView t1 = U.tv(this, "还没有家人", 24, th.text, true);
            t1.setGravity(Gravity.CENTER);
            TextView t2 = U.tv(this, "点这里添加\n点一下就打微信视频电话", 17, th.sub, false);
            t2.setGravity(Gravity.CENTER);
            t2.setLineSpacing(U.dp(this, 4), 1f);
            empty.addView(t1, U.lp(-2, -2, 0));
            empty.addView(t2, U.lpM(-2, -2, 0, U.dp(this, 12), 0));
            empty.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { editContact(null, -1); }
            });
            U.tapFx(empty);
            contactRows.addView(empty, U.lp(-1, -2, 0));
            return;
        }

        for (int start = 0; start < n; start += 2) {
            LinearLayout row = U.h(this);
            for (int c = 0; c < 2; c++) {
                int i = start + c;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, U.dp(this, 140), 1f);
                if (c == 0) lp.rightMargin = gap / 2; else lp.leftMargin = gap / 2;
                if (i < n) row.addView(contactCard(contacts.get(i), i), lp);
                else row.addView(new View(this), lp);
            }
            contactRows.addView(row, U.lpM(-1, -2, 0, gap, 0));
        }
    }

    /** 联系人卡：整块照片 + 底部深色渐变 + 白色大字，和参考版一致。 */
    private View contactCard(final Contact ct, final int index) {
        final int r = (int) Prefs.radiusPx(this);
        android.widget.FrameLayout card = new android.widget.FrameLayout(this);
        card.setBackground(U.stroke(th.card, r, U.dp(this, 1), th.cardStroke()));

        AvatarView av = new AvatarView(this);
        av.setRectMode(true);
        av.setCorner(r);
        av.setContact(ct);
        card.addView(av, new android.widget.FrameLayout.LayoutParams(-1, -1));

        // 底部渐变，保证白字压在任何照片上都看得清
        View scrim = new View(this);
        android.graphics.drawable.GradientDrawable g =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
                        new int[]{0xC0000000, 0x50000000, 0x00000000});
        g.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        scrim.setBackground(g);
        android.widget.FrameLayout.LayoutParams slp =
                new android.widget.FrameLayout.LayoutParams(-1, U.dp(this, 66), Gravity.BOTTOM);
        card.addView(scrim, slp);

        TextView name = U.tv(this, ct.name, Prefs.nameSp(this), 0xFFFFFFFF, true);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setGravity(Gravity.BOTTOM | Gravity.START);
        name.setPadding(U.dp(this, 12), 0, U.dp(this, 10), U.dp(this, 10));
        if (Build.VERSION.SDK_INT >= 11) name.setShadowLayer(4f, 0, 1, 0x99000000);
        card.addView(name, new android.widget.FrameLayout.LayoutParams(-1, -1));

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Tts.click(LauncherActivity.this,
                        (ct.callType == Contact.CALL_VIDEO ? "视频呼叫 " : "语音呼叫 ") + ct.name);
                WxCaller.call(LauncherActivity.this, ct);
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (Prefs.locked(LauncherActivity.this)) {
                    toast("桌面已锁定，请先在设置里解除");
                    return true;
                }
                showContactMenu(index);
                return true;
            }
        });
        U.tapFx(card);
        return card;
    }

    private void fillApps() {
        if (appRows == null) return;
        appRows.removeAllViews();
        List<AppRepo.App> all = AppRepo.load(this);
        List<AppRepo.App> picked = new ArrayList<AppRepo.App>();
        for (AppRepo.App a : all) if (homeApps.contains(a.pkg)) picked.add(a);

        int n = picked.size();
        int gap = U.dp(this, 8);
        for (int start = 0; start < n + 1; start += 2) {
            LinearLayout row = U.h(this);
            for (int c = 0; c < 2; c++) {
                int i = start + c;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, U.dp(this, 128), 1f);
                if (c == 0) lp.rightMargin = gap / 2; else lp.leftMargin = gap / 2;
                if (i < n) row.addView(appItem(picked.get(i)), lp);
                else if (i == n) row.addView(addAppCell(), lp);
                else row.addView(new View(this), lp);
            }
            appRows.addView(row, U.lpM(-1, -2, 0, gap, 0));
        }
    }

    private View addAppCell() {
        LinearLayout it = U.v(this);
        it.setGravity(Gravity.CENTER);
        it.setBackground(U.clickBg(U.lighten(th.accent, 0.9f),
                U.lighten(th.accent, 0.82f), U.dp(this, 15)));

        TextView plus = U.tv(this, "＋", 34, th.accent, true);
        plus.setGravity(Gravity.CENTER);
        it.addView(plus, U.lp(-2, -2, 0));

        TextView name = U.tv(this, "添加应用", 15, th.accent, true);
        name.setGravity(Gravity.CENTER);
        it.addView(name, U.lpM(-2, -2, 0, U.dp(this, 6), 0));

        it.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickApps(); }
        });
        U.tapFx(it);
        return it;
    }

    private View appItem(final AppRepo.App app) {
        LinearLayout it = U.v(this);
        it.setGravity(Gravity.CENTER);
        it.setBackground(U.clickBg(th.card, U.lighten(th.accent, 0.9f), U.dp(this, 15)));
        if (!th.dark) it.setBackground(U.stroke(th.card, U.dp(this, 15),
                U.dp(this, 1), th.cardStroke()));

        ImageView iv = new ImageView(this);
        iv.setImageDrawable(app.icon);
        int side = U.dp(this, 56);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(side, side);
        ilp.gravity = Gravity.CENTER_HORIZONTAL;
        it.addView(iv, ilp);

        TextView name = U.tv(this, app.label, Prefs.appSp(this), th.text, false);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        it.addView(name, U.lpM(-1, -2, 0, U.dp(this, 8), 0));

        it.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Tts.click(LauncherActivity.this, app.label);
                AppRepo.launch(LauncherActivity.this, app.pkg);
            }
        });
        it.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (Prefs.locked(LauncherActivity.this)) {
                    toast("桌面已锁定，请先在设置里解除");
                    return true;
                }
                removeApp(app);
                return true;
            }
        });
        U.tapFx(it);
        return it;
    }

    private void removeApp(final AppRepo.App app) {
        new AlertDialog.Builder(this)
                .setTitle(app.label)
                .setMessage("把这个应用从桌面移除吗？")
                .setPositiveButton("移 除", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface di, int w) {
                        homeApps.remove(app.pkg);
                        Store.saveHomeApps(LauncherActivity.this, homeApps);
                        fillApps();
                    }
                })
                .setNegativeButton("取 消", null)
                .create().show();
    }

    private void pickApps() {
        final AppPickerDialog d = new AppPickerDialog(this, th, homeApps);
        d.setCallback(new AppPickerDialog.Callback() {
            @Override public void onPicked(Set<String> pkgs) {
                homeApps = pkgs;
                Store.saveHomeApps(LauncherActivity.this, homeApps);
                fillApps();
            }
        });
        d.show();
    }

    // ================= 联系人菜单 =================

    private void showContactMenu(final int index) {
        String[] items = {"微信视频", "微信语音", "编 辑", "上 移", "下 移", "删 除"};
        new AlertDialog.Builder(this)
                .setTitle(contacts.get(index).name)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        Contact ct = contacts.get(index);
                        if (w == 0) { ct.callType = Contact.CALL_VIDEO; WxCaller.call(LauncherActivity.this, ct); }
                        else if (w == 1) { ct.callType = Contact.CALL_VOICE; WxCaller.call(LauncherActivity.this, ct); }
                        else if (w == 2) editContact(ct, index);
                        else if (w == 3 && index > 0) swap(index, index - 1);
                        else if (w == 4 && index < contacts.size() - 1) swap(index, index + 1);
                        else if (w == 5) confirmDelete(index);
                    }
                })
                .create().show();
    }

    private void swap(int a, int b) {
        Contact t = contacts.get(a);
        contacts.set(a, contacts.get(b));
        contacts.set(b, t);
        Store.save(this, contacts);
        fillContacts();
    }

    private void confirmDelete(final int index) {
        new AlertDialog.Builder(this)
                .setTitle("删除联系人")
                .setMessage("确定删除「" + contacts.get(index).name + "」吗？")
                .setPositiveButton("删 除", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        contacts.remove(index);
                        Store.save(LauncherActivity.this, contacts);
                        fillContacts();
                    }
                })
                .setNegativeButton("取 消", null)
                .create().show();
    }

    private void editContact(final Contact origin, final int index) {
        if (Prefs.locked(this) && origin != null) {
            toast("桌面已锁定，请先在设置里解除");
            return;
        }
        final ContactEditDialog d = new ContactEditDialog(this, th, origin,
                contacts == null ? 0 : contacts.size());
        d.setOnPickFromPhone(new Runnable() {
            @Override public void run() { pickSystemContact(); }
        });
        d.setOnPickPhoto(new Runnable() {
            @Override public void run() { launchPhotoPicker(); }
        });
        d.setCallback(new ContactEditDialog.Callback() {
            @Override public void onSaved(Contact c) {
                if (index >= 0 && index < contacts.size()) contacts.set(index, c);
                else contacts.add(c);
                Store.save(LauncherActivity.this, contacts);
                fillContacts();
            }
            @Override public void onDelete() {
                if (index >= 0 && index < contacts.size()) {
                    contacts.remove(index);
                    Store.save(LauncherActivity.this, contacts);
                    fillContacts();
                }
            }
        });
        lastDialog = d;
        d.show();
    }

    // ================= 通讯录导入 =================

    private void pickSystemContact() {
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 21);
            return;
        }
        try {
            startActivityForResult(new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI),
                    REQ_PICK_CONTACT);
        } catch (Exception e) { toast("打不开通讯录"); }
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PICK_CONTACT && res == RESULT_OK && data != null && data.getData() != null) {
            readSystemContact(data.getData());
        } else if (req == REQ_PICK_PHOTO && res == RESULT_OK && data != null && data.getData() != null) {
            if (lastDialog != null && lastDialog.isShowing()) {
                lastDialog.onPhotoPicked(data.getData());
            }
        }
    }

    private void launchPhotoPicker() {
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(i, "选择照片"), REQ_PICK_PHOTO);
        } catch (Exception e) { toast("打不开相册"); }
    }

    private void readSystemContact(Uri uri) {
        try {
            String name = null, number = null;
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    String id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
                    if (c.getInt(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        Cursor p = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                                new String[]{id}, null);
                        if (p != null) {
                            if (p.moveToFirst()) number = p.getString(p.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));
                            p.close();
                        }
                    }
                }
                c.close();
            }
            if (name != null) {
                if (lastDialog != null && lastDialog.isShowing()) {
                    lastDialog.fill(name, number == null ? "" : number);
                } else {
                    prefillDialog(name, number == null ? "" : number);
                }
            } else toast("没有读到联系人");
        } catch (Exception e) { toast("读取失败"); }
    }

    private void prefillDialog(String name, String number) {
        Contact c = new Contact(name, name, Store.pickColor(contacts.size()));
        c.number = number;
        final ContactEditDialog d = new ContactEditDialog(this, th, c, contacts.size());
        d.setOnPickFromPhone(new Runnable() {
            @Override public void run() { pickSystemContact(); }
        });
        d.setOnPickPhoto(new Runnable() {
            @Override public void run() { launchPhotoPicker(); }
        });
        d.setCallback(new ContactEditDialog.Callback() {
            @Override public void onSaved(Contact saved) {
                contacts.add(saved);
                Store.save(LauncherActivity.this, contacts);
                fillContacts();
            }
            @Override public void onDelete() {}
        });
        lastDialog = d;
        d.show();
    }

    // ================= 自动检查更新 =================

    private boolean updateChecked = false;

    /** 桌面打开后延迟几秒静默检查，12 小时最多一次，不打扰使用。 */
    private void maybeCheckUpdate() {
        if (updateChecked) return;
        if (!Prefs.autoUpdate(this)) return;
        long last = Prefs.lastCheck(this);
        if (System.currentTimeMillis() - last < 12L * 3600L * 1000L) return;
        updateChecked = true;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                UpdateChecker.check(LauncherActivity.this, new UpdateChecker.Callback() {
                    @Override public void onResult(UpdateChecker.Info info) {
                        Prefs.lastCheck(LauncherActivity.this, System.currentTimeMillis());
                        if (info != null && !isFinishing()) showUpdate(info);
                    }

                    @Override public void onError() {
                        // 没网/接口挂了：静默，别打扰老人
                    }

                });
            }
        }, 2500);
    }

    private void showUpdate(final UpdateChecker.Info info) {
        String notes = info.notes == null ? "" : info.notes.trim();
        if (notes.length() > 320) notes = notes.substring(0, 320) + "…";
        if (notes.length() == 0) notes = "有新版本可以更新，更新后功能更稳。";
        new AlertDialog.Builder(this)
                .setTitle("发现新版本 " + info.version)
                .setMessage(notes)
                .setPositiveButton("去下载", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        UpdateChecker.openDownload(LauncherActivity.this, info);
                    }
                })
                .setNegativeButton("以后再说", null)
                .create().show();
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
