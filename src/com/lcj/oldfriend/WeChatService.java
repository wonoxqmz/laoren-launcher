package com.lcj.oldfriend;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 微信通话自动化服务（无障碍）。
 *
 * 流程与市面上的「老人桌面」一致，但完全自己实现：
 *   打开微信 → 点搜索 → 输入备注名 → 点搜索结果进聊天 → 点「+」→ 点「视频通话/语音通话」
 *
 * 兼容策略：优先按文字/内容描述匹配（微信控件 id 每个版本都在变），
 * 任一步超时就提示用户手动操作，不会卡死在微信里。
 */
public class WeChatService extends AccessibilityService {

    static final int STEP_IDLE = 0;
    static final int STEP_WAIT_HOME = 1;
    static final int STEP_SEARCH_CLICK = 2;
    static final int STEP_INPUT = 3;
    static final int STEP_PICK = 4;
    static final int STEP_CHAT_MENU = 5;
    static final int STEP_CALL_ITEM = 6;

    private static WeChatService inst;
    private static String target = null;
    private static boolean video = true;
    private static int step = STEP_IDLE;
    private static long deadline = 0;

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            tick();
            if (step != STEP_IDLE) h.postDelayed(this, 400);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        inst = this;
        // 如果用户在服务连上之前就点了联系人，这里接着跑
        if (step != STEP_IDLE) {
            h.removeCallbacks(ticker);
            h.post(ticker);
        }
    }

    @Override public void onDestroy() {
        inst = null;
        super.onDestroy();
    }

    @Override public void onInterrupt() {}

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (step == STEP_IDLE) return;
        CharSequence pkg = e.getPackageName();
        if (pkg != null && "com.tencent.mm".equals(pkg.toString())) tick();
    }

    // ---------------- 外部入口 ----------------

    /** 发起一次微信通话；返回 false 表示前置条件不满足。 */
    public static boolean call(Context c, Contact ct) {
        if (!WxCaller.wxInstalled(c)) {
            Toast.makeText(c, "没装微信，无法拨打微信电话", Toast.LENGTH_LONG).show();
            return false;
        }
        if (!isReady()) {
            WxCaller.openAccessibilitySettings(c);
            Toast.makeText(c, "请先开启「老友桌面」的无障碍服务，才能自动拨微信", Toast.LENGTH_LONG).show();
            return false;
        }
        target = ct.searchName();
        video = ct.callType == Contact.CALL_VIDEO;
        step = STEP_WAIT_HOME;
        deadline = System.currentTimeMillis() + 20000;
        if (inst != null) {
            inst.h.removeCallbacks(inst.ticker);
            inst.h.post(inst.ticker);
        }
        return WxCaller.openWeChat(c);
    }

    public static boolean isReady() {
        return inst != null;
    }

    public static void cancel(Context c) {
        step = STEP_IDLE;
        target = null;
        if (inst != null) inst.h.removeCallbacks(inst.ticker);
    }

    // ---------------- 状态机 ----------------

    private void tick() {
        if (step == STEP_IDLE) return;
        if (System.currentTimeMillis() > deadline) {
            fail("没能在微信里完成操作，请手动拨打");
            return;
        }
        AccessibilityNodeInfo root = null;
        try { root = getRootInActiveWindow(); } catch (Exception ignored) {}
        if (root == null) return;

        switch (step) {
            case STEP_WAIT_HOME: {
                // 微信首页：找搜索入口
                AccessibilityNodeInfo s = findByText(root, "搜索");
                if (s == null) s = findByContentDesc(root, "搜索");
                if (s != null) {
                    if (click(s)) step = STEP_INPUT;
                    else step = STEP_SEARCH_CLICK;
                } else {
                    // 可能首页还没出来，或已经在别的页面，继续等
                }
                break;
            }
            case STEP_SEARCH_CLICK: {
                AccessibilityNodeInfo s = findByText(root, "搜索");
                if (s != null && click(s)) step = STEP_INPUT;
                break;
            }
            case STEP_INPUT: {
                AccessibilityNodeInfo edit = findEdit(root);
                if (edit != null) {
                    if (setText(edit, target)) step = STEP_PICK;
                    else step = STEP_PICK; // 设置失败也让用户/系统自己搜，尝试直接找结果
                }
                break;
            }
            case STEP_PICK: {
                AccessibilityNodeInfo item = findByText(root, target);
                if (item == null) item = findByTextContains(root, target);
                if (item != null) {
                    if (click(item)) step = STEP_CHAT_MENU;
                }
                break;
            }
            case STEP_CHAT_MENU: {
                AccessibilityNodeInfo plus = findByContentDescContains(root, "更多功能");
                if (plus == null) plus = findByContentDescContains(root, "更多");
                if (plus == null) plus = findBottomRightButton(root); // 版本差异兜底
                if (plus != null && click(plus)) step = STEP_CALL_ITEM;
                break;
            }
            case STEP_CALL_ITEM: {
                String want = video ? "视频通话" : "语音通话";
                AccessibilityNodeInfo it = findByTextContains(root, want);
                if (it != null && click(it)) {
                    done();
                }
                break;
            }
        }
    }

    private void done() {
        step = STEP_IDLE;
        target = null;
        h.removeCallbacks(ticker);
        Toast.makeText(getApplicationContext(), video ? "正在发起微信视频通话…" : "正在发起微信语音通话…",
                Toast.LENGTH_SHORT).show();
    }

    private void fail(String msg) {
        step = STEP_IDLE;
        target = null;
        h.removeCallbacks(ticker);
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        // 回桌面，别把老人留在微信的陌生页面
        try {
            android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            i.addCategory(android.content.Intent.CATEGORY_HOME);
            i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {}
    }

    // ---------------- 节点查找 / 操作 ----------------

    private boolean click(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo cur = n;
        for (int i = 0; i < 5 && cur != null; i++) {
            if (cur.isClickable()) {
                return cur.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            cur = cur.getParent();
        }
        // 都不可以点：试试对原文节点强点
        return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private boolean setText(AccessibilityNodeInfo edit, String text) {
        if (text == null) return false;
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                android.os.Bundle b = new android.os.Bundle();
                b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                return edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
            }
        } catch (Exception ignored) {}
        return false;
    }

    private AccessibilityNodeInfo findByText(AccessibilityNodeInfo root, String text) {
        for (AccessibilityNodeInfo n : all(root)) {
            CharSequence t = n.getText();
            if (t != null && text.equals(t.toString().trim())) return n;
        }
        return null;
    }

    private AccessibilityNodeInfo findByTextContains(AccessibilityNodeInfo root, String text) {
        for (AccessibilityNodeInfo n : all(root)) {
            CharSequence t = n.getText();
            if (t != null && t.toString().contains(text)) return n;
        }
        return null;
    }

    private AccessibilityNodeInfo findByContentDesc(AccessibilityNodeInfo root, String text) {
        for (AccessibilityNodeInfo n : all(root)) {
            CharSequence t = n.getContentDescription();
            if (t != null && text.equals(t.toString().trim())) return n;
        }
        return null;
    }

    private AccessibilityNodeInfo findByContentDescContains(AccessibilityNodeInfo root, String text) {
        for (AccessibilityNodeInfo n : all(root)) {
            CharSequence t = n.getContentDescription();
            if (t != null && t.toString().contains(text)) return n;
        }
        return null;
    }

    private AccessibilityNodeInfo findEdit(AccessibilityNodeInfo root) {
        for (AccessibilityNodeInfo n : all(root)) {
            if ("android.widget.EditText".equals(n.getClassName())) return n;
            if (n.isEditable()) return n;
        }
        return null;
    }

    /** 聊天页右下角那个「+」：按 ImageButton + 屏幕位置兜底找。 */
    private AccessibilityNodeInfo findBottomRightButton(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo best = null;
        android.graphics.Rect br = new android.graphics.Rect();
        int bestRight = -1, bestBottom = -1;
        for (AccessibilityNodeInfo n : all(root)) {
            if (!"android.widget.ImageButton".equals(n.getClassName())) continue;
            n.getBoundsInScreen(br);
            if (br.width() <= 0 || br.height() <= 0) continue;
            if (br.right > bestRight || (br.right == bestRight && br.bottom > bestBottom)) {
                bestRight = br.right;
                bestBottom = br.bottom;
                best = n;
            }
        }
        return best;
    }


    /** 广度优先遍历整棵树。 */
    private List<AccessibilityNodeInfo> all(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> out = new ArrayList<AccessibilityNodeInfo>();
        if (root == null) return out;
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<AccessibilityNodeInfo>();
        q.add(root);
        int guard = 0;
        while (!q.isEmpty() && guard++ < 2000) {
            AccessibilityNodeInfo n = q.poll();
            out.add(n);
            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo c = n.getChild(i);
                if (c != null) q.add(c);
            }
        }
        return out;
    }
}
