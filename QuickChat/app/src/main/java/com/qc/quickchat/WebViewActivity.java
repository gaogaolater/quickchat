package com.qc.quickchat;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.qc.webview.MyJavascriptInterface;
import com.qc.webview.MyWebViewClient;

public class WebViewActivity extends AppCompatActivity {

    WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        mWebView = (WebView)findViewById(R.id.wv_all);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        mWebView.setDownloadListener(new DownloadListener(){
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        mWebView.addJavascriptInterface(new MyJavascriptInterface(mWebView),"meishiapp");
        mWebView.setWebViewClient(new MyWebViewClient());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            mWebView.setWebContentsDebuggingEnabled(true);
        }
        mWebView.loadUrl("file:///android_asset/html/index.html");
    }

}
