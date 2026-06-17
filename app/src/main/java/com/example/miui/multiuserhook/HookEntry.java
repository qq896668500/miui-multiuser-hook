package com.example.miui.multiuserhook;

import android.database.MatrixCursor;
import android.net.Uri;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "MiuiMultiUserHook";
    private static final String TARGET_PACKAGE = "com.miui.securityspace";
    private static final String TARGET_CLASS = "com.miui.securityspace.provider.search.SecSettingsSearchProvider";
    private static final String MODULE_PACKAGE = "com.example.miui.multiuserhook";
    private static final String MODULE_ACTIVITY = "com.example.miui.multiuserhook.MultiUserSwitchActivity";
    private static final String ACTION_MULTI_USER_SWITCH = "com.example.miui.multiuserhook.action.MULTI_USER_SWITCH";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        log("Loaded target package: " + lpparam.packageName);
        hookQuery(lpparam);
    }

    private void hookQuery(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> providerClass = XposedHelpers.findClass(TARGET_CLASS, lpparam.classLoader);

            XposedHelpers.findAndHookMethod(
                    providerClass,
                    "query",
                    Uri.class,
                    String[].class,
                    String.class,
                    String[].class,
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object result = param.getResult();
                            if (!(result instanceof MatrixCursor)) {
                                return;
                            }

                            MatrixCursor cursor = (MatrixCursor) result;
                            cursor.newRow()
                                    .add("title", "多用户切换")
                                    .add("summaryOn", "切换到其他用户空间")
                                    .add("intentAction", ACTION_MULTI_USER_SWITCH)
                                    .add("intentTargetPackage", MODULE_PACKAGE)
                                    .add("intentTargetClass", MODULE_ACTIVITY);

                            log("Added multi-user search item");
                        }
                    }
            );
        } catch (Throwable t) {
            log("hookQuery failed", t);
        }
    }

    private static void log(String msg) {
        XposedBridge.log(TAG + ": " + msg);
    }

    private static void log(String msg, Throwable t) {
        XposedBridge.log(TAG + ": " + msg);
        XposedBridge.log(t);
    }
}
