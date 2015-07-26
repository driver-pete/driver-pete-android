package com.otognan.driverpete.android;

import android.app.Activity;
import android.content.Intent;
import android.net.http.SslError;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.otognan.driverpete.android.R;

import java.net.HttpCookie;
import java.util.List;

public class LoginActivity extends ActionBarActivity {

    private static final String LOG_TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final String serverUrl = this.getIntent().getExtras().getString("serverUrl");

        final Activity parentActivity = this;

        WebView loginWebView = (WebView) findViewById(R.id.loginWebView);

        loginWebView.setWebViewClient(new WebViewClient() {

            // Accept private ssl certificates
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                           SslError error) {
                handler.proceed();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.startsWith(serverUrl)) {
                    view.destroy();
                    String cookies_header = CookieManager.getInstance().getCookie(url);
                    List<HttpCookie> cookies = HttpCookie.parse(cookies_header);
                    for (HttpCookie cookie : cookies) {
                        if (cookie.getName().equals("AUTH-TOKEN")) {
                            String token = cookie.getValue();
                            Log.d("WebClient", "FOUND TOKEN:" + token);

                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("token", token);
                            setResult(RESULT_OK, returnIntent);
                            parentActivity.finish();
                            return;
                        }
                    }
                    Log.d("WebClient", "NO TOKEN AFTER LOGIN");
                    Intent returnIntent = new Intent();
                    setResult(RESULT_CANCELED, returnIntent);
                    parentActivity.finish();
                }
            }
        });

        WebSettings webSettings = loginWebView.getSettings();
        webSettings.setJavaScriptEnabled(false);

        boolean logout = false;
        if (logout) {
            String token = "<TOKEN_FROM_DB>";
            loginWebView.loadUrl("https://www.facebook.com/logout.php?access_token=" + token + "&confirm=1&next=" + "https%3A%2F%2F192.168.1.2%3A8443%2Fauth%2Ffacebook");
        } else {
            loginWebView.loadUrl(serverUrl + "/auth/facebook");
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "Clearing login webview cache..");
        WebView loginWebView = (WebView) findViewById(R.id.loginWebView);
        loginWebView.clearCache(true);
        super.onDestroy();
    }
}
