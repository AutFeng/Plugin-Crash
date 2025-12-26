package com.plugin.inject.crash.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CrashActivity extends Activity {
    private WebView webView;
    private String stackTrace = "";

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint({"SetTextI18n", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        setContentView(webView);

        getWindow().setStatusBarContrastEnforced(false);
        getWindow().setNavigationBarContrastEnforced(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        getWindow().setStatusBarColor(Color.WHITE);

        stackTrace = getIntent().getStringExtra("stack_trace");
        if (stackTrace == null) {
            stackTrace = "No stack trace available";
        }

        webView.loadUrl("file:///android_asset/crash_info.html");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String escapedTrace = stackTrace
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");
                webView.evaluateJavascript(
                    "document.querySelector('[tag=\"crash_info_text\"]').textContent = \"" + escapedTrace + "\";",
                    null
                );
            }
        });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void restartApp() {
            finishAffinity();
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            startActivity(intent);
            System.exit(0);
        }

        @JavascriptInterface
        public void copyError() {
            copyStackTraceToClipboard(stackTrace);
            runOnUiThread(() ->
                Toast.makeText(CrashActivity.this, "错误信息已复制到剪切板", Toast.LENGTH_SHORT).show()
            );
        }

        @JavascriptInterface
        public void shareError() {
            shareStackTrace(stackTrace);
        }
    }

    private void copyStackTraceToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Crash Stack Trace", text);
            clipboard.setPrimaryClip(clip);
        } catch (Exception e) {
            Toast.makeText(this, "复制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SimpleDateFormat")
    private void shareStackTrace(String stackTrace) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "应用崩溃信息");

            StringBuilder shareContent = new StringBuilder();
            shareContent.append("Android版本: ").append(Build.VERSION.RELEASE).append("\n");
            shareContent.append("设备型号: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
            shareContent.append("崩溃时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())).append("\n");
            shareContent.append("\n");
            shareContent.append("堆栈信息:\n");
            shareContent.append(stackTrace).append("\n");

            shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent.toString());

            startActivity(Intent.createChooser(shareIntent, "分享崩溃信息"));
        } catch (Exception e) {
            Toast.makeText(this, "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
