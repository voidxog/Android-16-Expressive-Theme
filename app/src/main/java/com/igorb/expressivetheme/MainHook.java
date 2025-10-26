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
import android.content.Context;
import android.provider.Settings;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

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
    
    // New constants for SystemUI aconfig flags
    private static final String SYSTEMUI_ACONFIG_CLASS = "com.android.systemui.flags.Flags";
    private static final String QS_UI_REFACTOR_FLAG = "qsUiRefactor";
    private static final String QS_UI_REFACTOR_COMPOSE_FRAGMENT_FLAG = "qsUiRefactorComposeFragment";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

        XposedBridge.log(TAG + ": Attempting to hook package: " + lpparam.packageName);

        // --- Hook SystemUI aconfig flags ---
        if ("com.android.systemui".equals(lpparam.packageName)) {
            try {
                // Hook QS UI Refactor flag
                XposedHelpers.findAndHookMethod(
                        SYSTEMUI_ACONFIG_CLASS,
                        lpparam.classLoader,
                        QS_UI_REFACTOR_FLAG,
                        new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                XposedBridge.log(TAG + ": [SystemUI] Forcing qsUiRefactor flag to return true");
                                return true;
                            }
                        }
                );
                XposedBridge.log(TAG + ": [SystemUI] Successfully hooked qsUiRefactor flag");

                // Hook QS UI Refactor Compose Fragment flag
                XposedHelpers.findAndHookMethod(
                        SYSTEMUI_ACONFIG_CLASS,
                        lpparam.classLoader,
                        QS_UI_REFACTOR_COMPOSE_FRAGMENT_FLAG,
                        new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                                XposedBridge.log(TAG + ": [SystemUI] Forcing qsUiRefactorComposeFragment flag to return true");
                                return true;
                            }
                        }
                );
                XposedBridge.log(TAG + ": [SystemUI] Successfully hooked qsUiRefactorComposeFragment flag");

            } catch (XposedHelpers.ClassNotFoundError e) {
                XposedBridge.log(TAG + ": [SystemUI] Flags class not found: " + e.getMessage());
            } catch (NoSuchMethodError e) {
                XposedBridge.log(TAG + ": [SystemUI] Flag method not found: " + e.getMessage());
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": [SystemUI] FAILED to hook aconfig flags:");
                XposedBridge.log(t);
            }
        }

        // --- Set system property for expressive design ---
        try {
            XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.os.SystemProperties", lpparam.classLoader),
                    "set",
                    "is_expressive_design_enabled",
                    "true"
            );
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Set system property is_expressive_design_enabled=true");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Failed to set system property:");
            XposedBridge.log(t);
        }

        // --- Your existing hooks (keep all your current code below) ---

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
                    SETTINGS_THEME_KT_CLASS,
                    lpparam.classLoader,
                    "isSpaExpressiveEnabled",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Replacing SettingsThemeKt.isSpaExpressiveEnabled(). Returning true.");
                            return true;
                        }
                    }
            );
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Successfully hooked and replaced SettingsThemeKt.isSpaExpressiveEnabled()");

        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Class not found for isSpaExpressiveEnabled hook (Expected if app doesn't use SPA Theme): " + e.getMessage());
        } catch (NoSuchMethodError e) {
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] Method not found isSpaExpressiveEnabled(): " + e.getMessage());
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": [" + lpparam.packageName + "] FAILED to hook SettingsThemeKt.isSpaExpressiveEnabled() with unexpected error:");
            XposedBridge.log(t);
        }

        XposedBridge.log(TAG + ": Finished hooking attempt for " + lpparam.packageName);
    }
        }
