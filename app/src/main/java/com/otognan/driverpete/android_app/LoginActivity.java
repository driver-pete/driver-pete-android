package com.otognan.driverpete.android_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.http.SslError;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
                    String cookies_header = CookieManager.getInstance().getCookie(url);
                    if (cookies_header == null) {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(LoginActivity.this);
                        builder1.setMessage("No token is received from the server");
                        builder1.setCancelable(true);
                        builder1.setPositiveButton("Ok",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                        AlertDialog alert11 = builder1.create();
                        alert11.show();
                        return;
                    }
                    view.destroy();
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
            try {
                loginWebView.loadUrl(serverUrl + "/auth/facebook");
            } catch (Exception ex) {
                ex.printStackTrace();

                Log.d("WebClient", "EXCEPTION");
                loginWebView.destroy();
            }

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
