package com.le1567.spring;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private WebView web;
    private long lastBack = 0;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Window w = getWindow();
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            w.setAttributes(lp);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
            w.setStatusBarColor(Color.TRANSPARENT);
            w.setNavigationBarColor(Color.TRANSPARENT);
        }
        w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setTextZoom(100);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        web.setBackgroundColor(0xFF420D09);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        web.addJavascriptInterface(new Bridge(), "Native");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");

        hideBars();
        requestNotificationPermission();
        FestivalScheduler.schedule(this);
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideBars();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 7);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (web == null) { super.onBackPressed(); return; }
        web.evaluateJavascript("window.__onBack ? window.__onBack() : false", value -> {
            boolean handled = "true".equals(value);
            if (!handled) {
                long now = System.currentTimeMillis();
                if (now - lastBack < 2000) {
                    finish();
                } else {
                    lastBack = now;
                    toastOnUi("再按一次返回键退出");
                }
            }
        });
    }

    private void toastOnUi(final String msg) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    public class Bridge {

        @JavascriptInterface
        public boolean isApp() { return true; }

        @JavascriptInterface
        public String platform() { return "android"; }

        @JavascriptInterface
        public void vibrate(int ms) {
            try {
                Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (v == null || !v.hasVibrator()) return;
                if (Build.VERSION.SDK_INT >= 26) {
                    v.vibrate(VibrationEffect.createOneShot(Math.max(6, ms),
                            VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(Math.max(6, ms));
                }
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public void toast(String msg) { toastOnUi(msg); }

        @JavascriptInterface
        public boolean saveImage(String base64) {
            try {
                if (Build.VERSION.SDK_INT < 29
                        && checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE")
                        != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(() -> requestPermissions(
                            new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 8));
                    toastOnUi("请允许存储权限后再保存");
                    return false;
                }
                byte[] raw = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                if (bmp == null) return false;
                Uri uri;
                if (Build.VERSION.SDK_INT >= 29) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                }
                android.content.ContentValues cv = new android.content.ContentValues();
                cv.put(MediaStore.Images.Media.DISPLAY_NAME,
                        "spring-countdown-" + System.currentTimeMillis() + ".png");
                cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                if (Build.VERSION.SDK_INT >= 29) {
                    cv.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/春节倒计时");
                    cv.put(MediaStore.Images.Media.IS_PENDING, 1);
                }
                Uri out = getContentResolver().insert(uri, cv);
                if (out == null) return false;
                OutputStream os = getContentResolver().openOutputStream(out);
                if (os == null) return false;
                bmp.compress(Bitmap.CompressFormat.PNG, 96, os);
                os.close();
                if (Build.VERSION.SDK_INT >= 29) {
                    cv.clear();
                    cv.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(out, cv, null, null);
                }
                toastOnUi("已保存到相册");
                return true;
            } catch (Exception e) {
                toastOnUi("保存失败");
                return false;
            }
        }

        @JavascriptInterface
        public boolean shareImage(String base64, String text) {
            try {
                byte[] raw = Base64.decode(base64, Base64.DEFAULT);
                File dir = new File(getCacheDir(), "share");
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, "card.png");
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(raw);
                fo.close();
                Uri uri = ShareProvider.uriFor(f);
                Intent it = new Intent(Intent.ACTION_SEND);
                it.setType("image/png");
                it.putExtra(Intent.EXTRA_STREAM, uri);
                it.putExtra(Intent.EXTRA_TEXT, text == null ? "" : text);
                it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(it, "分享贺卡"));
                return true;
            } catch (Exception e) {
                toastOnUi("分享失败");
                return false;
            }
        }

        @JavascriptInterface
        public void setKeepAwake(final boolean on) {
            runOnUiThread(() -> {
                if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }

        @JavascriptInterface
        public String nextFestival() {
            return FestivalScheduler.describeNext();
        }

        @JavascriptInterface
        public void rescheduleFestivals() {
            FestivalScheduler.schedule(MainActivity.this);
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
