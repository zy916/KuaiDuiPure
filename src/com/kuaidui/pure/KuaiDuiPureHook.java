package com.kuaidui.pure;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class KuaiDuiPureHook extends XposedModule {
    private static final String TAG = "KuaiDuiPure";
    private static final String TARGET = "com.kuaiduizuoye.scan";

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!TARGET.equals(param.getPackageName())) return;
        Log.i(TAG, "KuaiDuiPure loaded: " + param.getPackageName());
        install(this, param.getClassLoader());
    }

    private static void install(XposedInterface xposed, ClassLoader cl) {
        blockAdConfig(xposed, cl);
        blockAdSdk(xposed, cl);
        skipSplash(xposed, cl);
        blockAdViews(xposed, cl);
        Log.i(TAG, "KuaiDuiPure install done");
    }

    private static void blockAdConfig(XposedInterface xposed, ClassLoader cl) {
        String adConfig = "ai.a";
        ReflectUtils.hookAllMethodsDefault(xposed, adConfig, cl, "d", null);
        ReflectUtils.hookAllMethodsDefault(xposed, adConfig, cl, "f", null);
        ReflectUtils.hookAllMethodsDefault(xposed, adConfig, cl, "h", null);
        ReflectUtils.hookAllMethodsDefault(xposed, adConfig, cl, "j", Boolean.FALSE);
        ReflectUtils.hookAllMethodsDefault(xposed, adConfig, cl, "k", Boolean.FALSE);
        Log.i(TAG, "ad config blockers installed");
    }

    private static void blockAdSdk(XposedInterface xposed, ClassLoader cl) {
        block(xposed, cl, "com.bytedance.sdk.openadsdk.TTAdNativeImpl",
                "loadFeedAd", "loadSplashAd", "loadDrawFeedAd", "loadBannerExpressAd",
                "loadNativeExpressAd", "loadStream", "loadRewardVideoAd", "loadFullScreenVideoAd");
        block(xposed, cl, "com.qq.e.ads.splash.SplashAD",
                "fetchAndShowIn", "fetchFullScreenAndShowIn", "fetchAdOnly", "showAd");
        block(xposed, cl, "com.qq.e.ads.interstitial.InterstitialAD", "loadAd", "show");
        block(xposed, cl, "com.qq.e.ads.nativ.NativeUnifiedAD", "loadData");
        block(xposed, cl, "com.qq.e.ads.nativ.NativeExpressAD", "loadAD");
        block(xposed, cl, "com.qq.e.ads.banner2.UnifiedBannerView", "fetchAd");
        block(xposed, cl, "com.baidu.mobads.sdk.api.SplashAd", "loadAd", "showAd");
        block(xposed, cl, "com.baidu.mobads.sdk.api.NativeAd", "loadAd", "showAd");
        block(xposed, cl, "com.baidu.mobads.sdk.api.NativeUnifiedAd", "loadAd", "showAd");
        block(xposed, cl, "com.baidu.mobads.sdk.api.BannerAd", "loadAd", "showAd");
        block(xposed, cl, "com.kwad.sdk.api.KsSplashScreenAd", "load", "show");
        block(xposed, cl, "com.kwad.sdk.api.KsNativeAd", "load", "show");
        block(xposed, cl, "com.kwad.sdk.api.KsInterstitialAd", "load", "show");
        block(xposed, cl, "com.kwad.sdk.api.KsFeedAd", "load", "show");
        Log.i(TAG, "ad sdk blockers installed");
    }

    private static void block(XposedInterface xposed, ClassLoader cl, String className, String... methods) {
        for (String method : methods) {
            ReflectUtils.hookAllMethodsDefault(xposed, className, cl, method, null);
        }
    }

    private static void skipSplash(XposedInterface xposed, ClassLoader cl) {
        try {
            Class<?> rgd = ReflectUtils.findClass("rg.d", cl);
            if (rgd != null) {
                Constructor<?> ctor = rgd.getDeclaredConstructor(Activity.class);
                ctor.setAccessible(true);
                xposed.hook(ctor).intercept(chain -> {
                    Object result = chain.proceed();
                    try {
                        ReflectUtils.setField(chain.getThisObject(), "a", Boolean.FALSE);
                        Log.i(TAG, "rg.d splash disabled");
                    } catch (Throwable ignored) {
                    }
                    return result;
                });
            }
        } catch (Throwable t) {
            Log.w(TAG, "skip rg.d splash failed: " + t);
        }

        ReflectUtils.hookMethod(xposed,
                "com.kuaiduizuoye.scan.activity.newadvertisement.splash.activity.ResumeSplashActivity",
                cl, "onCreate", new Class<?>[]{Bundle.class}, chain -> {
                    Object result = chain.proceed();
                    try {
                        ((Activity) chain.getThisObject()).finish();
                        Log.i(TAG, "ResumeSplashActivity finished");
                    } catch (Throwable ignored) {
                    }
                    return result;
                });
        Log.i(TAG, "splash skip installed");
    }

    private static void blockAdViews(XposedInterface xposed, ClassLoader cl) {
        try {
            Method setVisibility = View.class.getDeclaredMethod("setVisibility", int.class);
            xposed.hook(setVisibility).intercept(chain -> {
                View view = (View) chain.getThisObject();
                if (isTargetView(view) && isAdView(view)) {
                    return chain.proceed(new Object[]{View.GONE});
                }
                return chain.proceed();
            });
        } catch (Throwable t) {
            Log.w(TAG, "hide ad setVisibility failed: " + t);
        }

        try {
            for (Method addView : ViewGroup.class.getDeclaredMethods()) {
                if (!"addView".equals(addView.getName())) continue;
                xposed.hook(addView).intercept(chain -> {
                    Object arg0 = chain.getArg(0);
                    if (arg0 instanceof View) {
                        View child = (View) arg0;
                        if (isTargetView(child) && isAdView(child)) {
                            child.setVisibility(View.GONE);
                            ViewGroup.LayoutParams lp = child.getLayoutParams();
                            if (lp != null) {
                                lp.width = 0;
                                lp.height = 0;
                                child.setLayoutParams(lp);
                            }
                            Log.i(TAG, "hide ad view: " + child.getClass().getName());
                        }
                    }
                    return chain.proceed();
                });
            }
        } catch (Throwable t) {
            Log.w(TAG, "hide ad addView failed: " + t);
        }
        Log.i(TAG, "ad view blockers installed");
    }

    private static boolean isTargetView(View view) {
        if (view == null) return false;
        Context ctx = view.getContext();
        for (int i = 0; ctx != null && i < 12; i++) {
            try {
                if (TARGET.equals(ctx.getPackageName())) return true;
            } catch (Throwable ignored) {
            }
            if (!(ctx instanceof ContextWrapper)) break;
            Context base = ((ContextWrapper) ctx).getBaseContext();
            if (base == ctx) break;
            ctx = base;
        }
        return false;
    }

    private static boolean isAdView(View view) {
        String name = view.getClass().getName().toLowerCase();
        return name.contains("splashad")
                || name.contains("feedad")
                || name.contains("adcontainer")
                || name.contains("aditem")
                || name.contains("interstitial")
                || name.contains("nativead")
                || name.contains("bannerad")
                || name.contains("inspiread")
                || name.contains("newadvertisement");
    }
}

