package com.lcj.oldfriend;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 已安装应用列表（带桌面入口的）。 */
public class AppRepo {

    public static class App {
        public String label;
        public String pkg;
        public Drawable icon;
    }

    public static List<App> load(final Context c) {
        List<App> out = new ArrayList<App>();
        try {
            PackageManager pm = c.getPackageManager();
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
            String self = c.getPackageName();
            for (ResolveInfo r : list) {
                if (r.activityInfo == null) continue;
                String pkg = r.activityInfo.packageName;
                if (self.equals(pkg)) continue;
                App a = new App();
                a.label = String.valueOf(r.loadLabel(pm));
                a.pkg = pkg;
                a.icon = r.loadIcon(pm);
                if ((r.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                        || true) {
                    out.add(a);
                }
            }
            // 去重（同包名多个入口保留第一个）
            List<App> dedup = new ArrayList<App>();
            for (App a : out) {
                boolean dup = false;
                for (App b : dedup) if (b.pkg.equals(a.pkg) && b.label.equals(a.label)) { dup = true; break; }
                if (!dup) dedup.add(a);
            }
            Collections.sort(dedup, new Comparator<App>() {
                @Override public int compare(App a, App b) {
                    return a.label.compareToIgnoreCase(b.label);
                }
            });
            return dedup;
        } catch (Exception e) {
            return out;
        }
    }

    public static void launch(Context c, String pkg) {
        try {
            Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                c.startActivity(i);
            }
        } catch (Exception ignored) {}
    }
}
