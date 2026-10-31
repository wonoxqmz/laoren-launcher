package com.lcj.oldfriend;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.List;

/**
 * 主桌面（HOME）。
 * 页0：时间卡 + 联系人网格（点按拨号，长按编辑）
 * 页1：全部应用大图标
 * 底栏：联系人 / 应用 / 电话 / 设置
 */
public class LauncherActivity extends Activity {

    private static final int REQ_CALL_PERM = 11;
    private static final int REQ_PICK_CONTACT = 12;
    private static final int REQ_OPEN_SETTINGS = 13;

    private Theme th;
    private ViewFlipper flipper;
    private LinearLayout contactRows;
    private LinearLayout appRows;
    private LinearLayout bottomBar;
    private List<Contact> contacts;
    private List<AppRepo.App> apps;
    private String pendingCall;
    private float downX, downY;
    private ContactEditDialog lastDialog;
    private int lastThemeIdx = -1;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        th = Theme.get(Prefs.theme(this));
        lastThemeIdx = Prefs.theme(this);
        Tts.init(this);
        buildUi();
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        int t = Prefs.theme(this);
        th = Theme.get(t);
        if (t != lastThemeIdx) { // 主题变了就整体重建
            lastThemeIdx = t;
            buildUi();
        }
        refresh();
    }

    @Override public void onBackPressed() {
        // 桌面不退出；在第二页时先回联系人页
        if (flipper.getDisplayedChild() != 0) setPage(0);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        // 横向轻扫翻页（不干扰纵向滚动）
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX(); downY = ev.getY(); break;
            case MotionEvent.ACTION_UP: {
                float dx = ev.getX() - downX, dy = ev.getY() - downY;
                float lim = U.dp(this, 90);
                if (Math.abs(dx) > lim && Math.abs(dy) < U.dp(this, 50)) {
                    if (dx < 0 && flipper.getDisplayedChild() == 0 && Prefs.showApps(this)) setPage(1);
                    else if (dx > 0 && flipper.getDisplayedChild() == 1) setPage(0);
                }
                break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    // ================= UI 构建 =================

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(th.bg);

        LinearLayout col = U.v(this);
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));

        flipper = new ViewFlipper(this);
        flipper.addView(buildContactPage());
        flipper.addView(buildAppPage());
        col.addView(flipper, U.lp(-1, 0, 1f));

        bottomBar = buildBottomBar();
        col.addView(bottomBar, U.lp(-1, U.dp(this, 86), 0));

        setContentView(root, new ViewGroup.LayoutParams(-1, -1));
    }

    private View buildContactPage() {
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        LinearLayout col = U.v(this);
        col.setPadding(U.dp(this, 12), U.dp(this, 14), U.dp(this, 12), U.dp(this, 14));
        sc.addView(col, new ViewGroup.LayoutParams(-1, -2));

        // 时间卡
        FrameLayout timeCard = new FrameLayout(this);
        timeCard.setBackground(U.solid(th.card, U.dp(this, 24)));
        timeCard.setPadding(U.dp(this, 20), U.dp(this, 18), U.dp(this, 16), U.dp(this, 14));
        TimeView tv = new TimeView(this, th);
        timeCard.addView(tv, new FrameLayout.LayoutParams(-1, U.dp(this, 150), Gravity.CENTER));
        IconView gear = new IconView(this, IconView.SETTINGS);
        gear.setColor(th.sub);
        gear.setPadding(U.dp(this, 8), U.dp(this, 8), U.dp(this, 8), U.dp(this, 8));
        gear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivityForResult(new Intent(LauncherActivity.this, SettingsActivity.class),
                        REQ_OPEN_SETTINGS);
            }
        });
        FrameLayout.LayoutParams glp = new FrameLayout.LayoutParams(U.dp(this, 36), U.dp(this, 36),
                Gravity.TOP | Gravity.END);
        timeCard.addView(gear, glp);
        col.addView(timeCard, U.lp(-1, -2, 0));
        ((LinearLayout.LayoutParams) timeCard.getLayoutParams()).bottomMargin = U.dp(this, 14);

        // 联系人行容器
        contactRows = U.v(this);
        col.addView(contactRows, U.lp(-1, -2, 0));
        return sc;
    }

    private View buildAppPage() {
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        LinearLayout col = U.v(this);
        col.setPadding(U.dp(this, 14), U.dp(this, 18), U.dp(this, 14), U.dp(this, 14));
        TextView title = U.tv(this, "全部应用", 26, th.text, true);
        col.addView(title, U.lp(-2, -2, 0));
        ((LinearLayout.LayoutParams) title.getLayoutParams()).bottomMargin = U.dp(this, 14);
        appRows = U.v(this);
        col.addView(appRows, U.lp(-1, -2, 0));
        sc.addView(col, new ViewGroup.LayoutParams(-1, -2));
        return sc;
    }

    private LinearLayout buildBottomBar() {
        LinearLayout bar = U.h(this);
        bar.setGravity(Gravity.CENTER);
        bar.setBackground(U.solid(th.dark ? U.darken(th.card, 0.3f) : 0xFFFFFFFF, U.dp(this, 24)));
        int pad = U.dp(this, 8);
        bar.setPadding(pad, pad, pad, pad);
        rebuildBottomBar(bar);
        return bar;
    }

    private void rebuildBottomBar(LinearLayout bar) {
        bar.removeAllViews();
        boolean apps = Prefs.showApps(this);
        addItem(bar, IconView.CONTACTS, "联系人", new View.OnClickListener() {
            @Override public void onClick(View v) { setPage(0); }
        }, flipper != null && flipper.getDisplayedChild() == 0);
        if (apps) {
            addItem(bar, IconView.APPS, "应用", new View.OnClickListener() {
                @Override public void onClick(View v) { setPage(1); }
            }, flipper != null && flipper.getDisplayedChild() == 1);
        }
        addItem(bar, IconView.PHONE, "电话", new View.OnClickListener() {
            @Override public void onClick(View v) { Tts.click(LauncherActivity.this, "电话"); startActivity(new Intent(LauncherActivity.this, DialerActivity.class)); }
        }, false);
        addItem(bar, IconView.SETTINGS, "设置", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivityForResult(new Intent(LauncherActivity.this, SettingsActivity.class),
                        REQ_OPEN_SETTINGS);
            }
        }, false);
        if (contacts != null && contacts.isEmpty()) {
            // 没有联系人时，底栏前面追加一个醒目的 SOS 式添加按钮
            addItem(bar, IconView.SOS, "添加", new View.OnClickListener() {
                @Override public void onClick(View v) { editContact(null, -1); }
            }, false);
        }
    }

    private void addItem(LinearLayout bar, int type, String label, View.OnClickListener oc, boolean active) {
        LinearLayout it = U.v(this);
        it.setGravity(Gravity.CENTER);
        it.setOnClickListener(oc);

        IconView ic = new IconView(this, type);
        ic.setColor(active ? th.accent : th.sub);
        it.addView(ic, U.lp(U.dp(this, 26), U.dp(this, 26), 0));

        TextView t = U.tv(this, label, 15, active ? th.accent : th.sub, true);
        t.setGravity(Gravity.CENTER);
        ((LinearLayout.LayoutParams) t.getLayoutParams()).topMargin = U.dp(this, 4);
        it.addView(t, U.lp(-2, -2, 0));

        bar.addView(it, U.lp(0, -1, 1f));
    }

    // ================= 数据刷新 =================

    private void refresh() {
        contacts = Store.load(this);
        rebuildBottomBar(bottomBar);
        fillContacts();
        if (Prefs.showApps(this)) {
            apps = AppRepo.load(this);
            fillApps();
        }
    }

    private void setPage(int i) {
        if (i == flipper.getDisplayedChild()) return;
        flipper.setDisplayedChild(i);
        rebuildBottomBar(bottomBar);
    }

    private void fillContacts() {
        contactRows.removeAllViews();
        int cols = Prefs.cols(this);
        int n = contacts.size();

        if (n == 0) {
            LinearLayout empty = U.v(this);
            empty.setGravity(Gravity.CENTER);
            empty.setBackground(U.stroke(th.card, U.dp(this, 24), U.dp(this, 2), U.lighten(th.accent, 0.4f)));
            empty.setPadding(U.dp(this, 20), U.dp(this, 40), U.dp(this, 20), U.dp(this, 40));
            TextView t1 = U.tv(this, "还没有联系人", 24, th.text, true);
            t1.setGravity(Gravity.CENTER);
            TextView t2 = U.tv(this, "点下方「添加」按钮\n把家人电话放上桌面", 18, th.sub, false);
            t2.setGravity(Gravity.CENTER);
            empty.addView(t1, U.lp(-2, -2, 0));
            ((LinearLayout.LayoutParams) t2.getLayoutParams()).topMargin = U.dp(this, 12);
            empty.addView(t2, U.lp(-2, -2, 0));
            empty.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { editContact(null, -1); }
            });
            contactRows.addView(empty, U.lp(-1, -2, 0));
            return;
        }

        int idx = 0;
        for (int rowStart = 0; rowStart < n; rowStart += cols) {
            LinearLayout row = U.h(this);
            for (int c = 0; c < cols; c++) {
                int i = rowStart + c;
                if (i < n) {
                    row.addView(contactCard(contacts.get(i), i), U.lp(0, U.dp(this, 150), 1f));
                } else {
                    View filler = new View(this);
                    row.addView(filler, U.lp(0, U.dp(this, 150), 1f));
                }
            }
            contactRows.addView(row, U.lp(-1, -2, 0));
            ((LinearLayout.LayoutParams) row.getLayoutParams()).topMargin = U.dp(this, 10);
            idx++;
        }
    }

    private View contactCard(final Contact ct, final int index) {
        LinearLayout card = U.v(this);
        card.setGravity(Gravity.CENTER);
        card.setBackground(U.solid(th.card, (int) Prefs.radiusPx(this)));
        card.setPadding(U.dp(this, 8), U.dp(this, 12), U.dp(this, 8), U.dp(this, 10));

        AvatarView av = new AvatarView(this);
        av.setContact(ct);
        int side = U.dp(this, 92);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(side, side);
        alp.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(av, alp);

        TextView name = U.tv(this, ct.name, Prefs.nameSp(this), th.text, true);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        ((LinearLayout.LayoutParams) name.getLayoutParams()).topMargin = U.dp(this, 8);
        card.addView(name, U.lp(-1, -2, 0));

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Tts.click(LauncherActivity.this, "呼叫 " + ct.name);
                if (Prefs.callConfirm(LauncherActivity.this)) confirmCall(ct);
                else dial(ct.number);
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (Prefs.locked(LauncherActivity.this)) {
                    toast("桌面已锁定，请先在设置中解除");
                    return true;
                }
                showContactMenu(index);
                return true;
            }
        });
        return card;
    }

    private void fillApps() {
        appRows.removeAllViews();
        if (apps == null || apps.isEmpty()) {
            TextView t = U.tv(this, "没有找到应用", 20, th.sub, false);
            appRows.addView(t, U.lp(-2, -2, 0));
            return;
        }
        int cols = 4; // 应用页固定 4 列，小屏也够用
        int n = apps.size();
        for (int rowStart = 0; rowStart < n; rowStart += cols) {
            LinearLayout row = U.h(this);
            for (int c = 0; c < cols; c++) {
                int i = rowStart + c;
                if (i < n) {
                    row.addView(appItem(apps.get(i)), U.lp(0, -2, 1f));
                } else {
                    row.addView(new View(this), U.lp(0, -2, 1f));
                }
            }
            appRows.addView(row, U.lp(-1, -2, 0));
            ((LinearLayout.LayoutParams) row.getLayoutParams()).topMargin = U.dp(this, 16);
        }
    }

    private View appItem(final AppRepo.App app) {
        LinearLayout it = U.v(this);
        it.setGravity(Gravity.CENTER);
        it.setPadding(U.dp(this, 4), U.dp(this, 4), U.dp(this, 4), U.dp(this, 4));

        ImageView iv = new ImageView(this);
        iv.setImageDrawable(app.icon);
        int side = U.dp(this, 58);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(side, side);
        ilp.gravity = Gravity.CENTER_HORIZONTAL;
        it.addView(iv, ilp);

        TextView name = U.tv(this, app.label, Prefs.appSp(this), th.text, false);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        ((LinearLayout.LayoutParams) name.getLayoutParams()).topMargin = U.dp(this, 6);
        it.addView(name, U.lp(-1, -2, 0));

        it.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Tts.click(LauncherActivity.this, app.label);
                AppRepo.launch(LauncherActivity.this, app.pkg);
            }
        });
        return it;
    }

    // ================= 拨号 =================

    private void confirmCall(final Contact ct) {
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("呼叫 " + ct.name)
                .setMessage(ct.number)
                .setPositiveButton("拨 打", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { dial(ct.number); }
                })
                .setNegativeButton("取 消", null)
                .create();

        dlg.show();
    }

    private void dial(String number) {
        if (number == null || number.trim().length() == 0) {
            toast("号码为空");
            return;
        }
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED) {
            pendingCall = number;
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQ_CALL_PERM);
            return;
        }
        placeCall(number);
    }

    private void placeCall(String number) {
        try {
            Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number.trim()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number.trim())));
            } catch (Exception ignored) {}
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_CALL_PERM && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED
                && pendingCall != null) {
            placeCall(pendingCall);
        } else if (code == REQ_CALL_PERM) {
            toast("没有拨号权限，改为跳转系统拨号确认");
            if (pendingCall != null) {
                try {
                    startActivity(new Intent(Intent.ACTION_DIAL,
                            Uri.parse("tel:" + pendingCall.trim())));
                } catch (Exception ignored) {}
            }
        }
        pendingCall = null;
    }

    // ================= 联系人编辑 =================

    private void showContactMenu(final int index) {
        String[] items = {"编 辑", "上 移", "下 移", "删 除"};
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(contacts.get(index).name)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        if (w == 0) {
                            editContact(contacts.get(index), index);
                        } else if (w == 1 && index > 0) {
                            swap(index, index - 1);
                        } else if (w == 2 && index < contacts.size() - 1) {
                            swap(index, index + 1);
                        } else if (w == 3) {
                            confirmDelete(index);
                        }
                    }
                })
                .create();

        dlg.show();
    }

    private void swap(int a, int b) {
        Contact t = contacts.get(a);
        contacts.set(a, contacts.get(b));
        contacts.set(b, t);
        Store.save(this, contacts);
        fillContacts();
    }

    private void confirmDelete(final int index) {
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("删除联系人")
                .setMessage("确定删除「" + contacts.get(index).name + "」吗？")
                .setPositiveButton("删 除", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        contacts.remove(index);
                        Store.save(LauncherActivity.this, contacts);
                        fillContacts();
                        rebuildBottomBar(bottomBar);
                    }
                })
                .setNegativeButton("取 消", null)
                .create();

        dlg.show();
    }

    private void editContact(final Contact origin, final int index) {
        if (Prefs.locked(this) && origin != null) {
            toast("桌面已锁定，请先在设置中解除");
            return;
        }
        final ContactEditDialog d = new ContactEditDialog(this, th, origin,
                contacts == null ? 0 : contacts.size());
        d.setOnPickFromPhone(new Runnable() {
            @Override public void run() { pickSystemContact(); }
        });
        d.setCallback(new ContactEditDialog.Callback() {
            @Override public void onSaved(Contact c) {
                if (index >= 0 && index < contacts.size()) contacts.set(index, c);
                else contacts.add(c);
                Store.save(LauncherActivity.this, contacts);
                fillContacts();
                rebuildBottomBar(bottomBar);
            }

            @Override public void onDelete() {
                if (index >= 0 && index < contacts.size()) {
                    contacts.remove(index);
                    Store.save(LauncherActivity.this, contacts);
                    fillContacts();
                    rebuildBottomBar(bottomBar);
                }
            }
        });
        lastDialog = d;
        d.show();
    }

    private void pickSystemContact() {
        boolean needPerm = Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED;
        if (needPerm) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 21);
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(i, REQ_PICK_CONTACT);
        } catch (Exception e) {
            toast("打不开通讯录");
        }
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PICK_CONTACT && res == RESULT_OK && data != null && data.getData() != null) {
            readSystemContact(data.getData());
        }
    }

    private void readSystemContact(Uri uri) {
        try {
            String name = null, number = null;
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    String id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
                    boolean hasPhone = c.getInt(c.getColumnIndex(
                            ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0;
                    if (hasPhone) {
                        Cursor p = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                                new String[]{id}, null);
                        if (p != null) {
                            if (p.moveToFirst()) {
                                number = p.getString(p.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                            }
                            p.close();
                        }
                    }
                }
                c.close();
            }
            if (name != null) {
                String num = number == null ? "" : number;
                if (lastDialog != null && lastDialog.isShowing()) {
                    lastDialog.fill(name, num); // 填回正在编辑的对话框
                } else {
                    editContactPrefill(name, num);
                }
            }
            else toast("没有读到联系人");
        } catch (Exception e) {
            toast("读取失败");
        }
    }

    /** 从通讯录选完后，弹出预填好的编辑框。 */
    private void editContactPrefill(String name, String number) {
        final Contact c = new Contact(name, number, Store.pickColor(contacts.size()));
        final ContactEditDialog d = new ContactEditDialog(this, th, c, contacts.size());
        d.setCallback(new ContactEditDialog.Callback() {
            @Override public void onSaved(Contact saved) {
                contacts.add(saved);
                Store.save(LauncherActivity.this, contacts);
                fillContacts();
                rebuildBottomBar(bottomBar);
            }
            @Override public void onDelete() {}
        });
        d.show();
    }

    // ================= 杂项 =================

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
