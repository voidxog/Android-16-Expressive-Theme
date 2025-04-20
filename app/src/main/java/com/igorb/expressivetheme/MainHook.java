/*
 * WearableSpoof
 * Copyright (C) 2023 Simon1511
 * CaimanSpoof
 * Copyright (C) 2024 RisenID
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.igorb.expressivetheme;

import android.os.Build;
// --- Add ALL of these imports ---
import android.content.Context; // For Context class
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook; // MethodHookParam is nested here
import de.robv.android.xposed.XC_MethodReplacement; // For XC_MethodReplacement
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam; // For LoadPackageParam (nested class)

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainHook implements IXposedHookLoadPackage {

    private static final Set<String> PRIMARY_TARGET_PACKAGES = new HashSet<>(Arrays.asList(
            "com.android.settings",
            "com.google.android.permissioncontroller",
            "com.google.android.healthconnect.controller",
            "com.android.systemui",
            "com.google.android.repairmode",
            "com.android.devicediagnostics"
    ));

    private static final String TAG = "ForceExpressiveLSPosed";
    private static final String SETTINGS_THEME_HELPER_CLASS = "com.android.settingslib.widget.SettingsThemeHelper";
    private static final String EXPRESSIVE_THEME_STATE_ENUM = SETTINGS_THEME_HELPER_CLASS + "$ExpressiveThemeState";
    private static final String SETTINGS_THEME_KT_CLASS = "com.android.settingslib.spa.framework.theme.SettingsThemeKt";


    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

        XposedBridge.log(TAG + ": Attempting to hook package: " + lpparam.packageName);

        // --- Hook 1: SettingsThemeHelper.tryInit() ---
        try {
            Class<?> settingsThemeHelperClass = XposedHelpers.findClass(SETTINGS_THEME_HELPER_CLASS, lpparam.classLoader);
            Class<?> expressiveThemeStateEnumClass = XposedHelpers.findClass(EXPRESSIVE_THEME_STATE_ENUM, lpparam.classLoader);
            Object enabledState = XposedHelpers.getStaticObjectField(expressiveThemeStateEnumClass, "ENABLED");

            XposedHelpers.findAndHookMethod(
                    settingsThemeHelperClass,
                    "tryInit",
                    Context.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Replacing SettingsThemeHelper.tryInit(). Forcing ENABLED state.");
                            XposedHelpers.setStaticObjectField(settingsThemeHelperClass, "expressiveThemeState", enabledState);
                            return null;
                        }
                    }
            );
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Successfully hooked and replaced SettingsThemeHelper.tryInit()");

        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Class not found for tryInit hook (Expected if app doesn't use SettingsLib): " + e.getMessage());
        } catch (NoSuchFieldError e) {
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Field not found during tryInit hook: " + e.getMessage());
        } catch (NoSuchMethodError e) {
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Method not found tryInit(Context): " + e.getMessage());
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] FAILED to hook SettingsThemeHelper.tryInit() with unexpected error:");
            XposedBridge.log(t);
        }

        // --- Hook 2: SettingsThemeHelper.isExpressiveTheme() ---
        try {
            XposedHelpers.findAndHookMethod(
                    SETTINGS_THEME_HELPER_CLASS,
                    lpparam.classLoader,
                    "isExpressiveTheme",
                    Context.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Replacing SettingsThemeHelper.isExpressiveTheme(). Returning true.");
                            return true;
                        }
                    }
            );
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Successfully hooked and replaced SettingsThemeHelper.isExpressiveTheme()");

        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Class not found for isExpressiveTheme hook (Expected if app doesn't use SettingsLib): " + e.getMessage());
        } catch (NoSuchMethodError e) {
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Method not found isExpressiveTheme(Context): " + e.getMessage());
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] FAILED to hook SettingsThemeHelper.isExpressiveTheme() with unexpected error:");
            XposedBridge.log(t);
        }

        // --- Hook 3: SettingsThemeKt.isSpaExpressiveEnabled() (For SPA Framework) ---
        try {
            XposedHelpers.findAndHookMethod(
                    SETTINGS_THEME_KT_CLASS,    // The new target class
                    lpparam.classLoader,
                    "isSpaExpressiveEnabled",   // The target static method
                    // No parameters for this method
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                            // param object exists but has no args for a static method with no parameters
                            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Replacing SettingsThemeKt.isSpaExpressiveEnabled(). Returning true.");
                            // Method returns boolean, so return true
                            return true;
                        }
                    }
            );
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Successfully hooked and replaced SettingsThemeKt.isSpaExpressiveEnabled()");

        } catch (XposedHelpers.ClassNotFoundError e) {
            // This is expected if the app doesn't use the SPA framework or this specific class
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Class not found for isSpaExpressiveEnabled hook (Expected if app doesn't use SPA Theme): " + e.getMessage());
        } catch (NoSuchMethodError e) {
            // Expected if the method signature is different or missing
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Method not found isSpaExpressiveEnabled(): " + e.getMessage());
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] FAILED to hook SettingsThemeKt.isSpaExpressiveEnabled() with unexpected error:");
            XposedBridge.log(t);
        }
        // --- End of Hook 3 ---


        XposedBridge.log(TAG + ": Finished hooking attempt for " + lpparam.packageName);
    }
}