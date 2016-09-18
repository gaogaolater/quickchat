package com.qc.webview;

import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by aaa on 16/9/18.
 */
public class MyJavascriptInterface {

    private final WebView mWebView;
    private final OkHttpClient client = new OkHttpClient();
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String resultBase64 = msg.obj.toString();
            mWebView.loadUrl("javascript:cb('"+resultBase64+"')");
        }
    };

    public MyJavascriptInterface(WebView mWebView){
        this.mWebView = mWebView;
    }

    @JavascriptInterface
    public void call_api(String url, final String callback) {

        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback(){

            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
                String result = response.body().string();
                Log.i("info",result);
                result = result.replace("\\r\\n", "<br>");
                String resultBase64 = Base64.encodeToString(result.getBytes(), Base64.DEFAULT);
                Message msg = handler.obtainMessage();
                msg.obj = resultBase64;
                msg.sendToTarget();
            }
        });
    }


}
