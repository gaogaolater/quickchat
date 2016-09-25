package com.quickchat.quickchatapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.quickchat.speex.SpeexPlayer;
import com.quickchat.speex.SpeexRecorder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MWebViewActivity extends Activity {
    WebView wv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mwebview);
        wv = (WebView) findViewById(R.id.webView);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new webViewClient());
        wv.addJavascriptInterface(new JavaScriptInterfaceChatApp(this), "mc");
        wv.clearCache(true);
        wv.loadUrl("http://192.168.199.122:3000/chat");
    }

    class webViewClient extends WebViewClient {
        //去掉提示 默认打开方式
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    private class JavaScriptInterfaceChatApp {
        MWebViewActivity activity;
        WebView wv;
        SpeexRecorder recorderInstance = null;
        SpeexPlayer splayer = null;
        String fileName = null;
        Long startRecordTime = 0L;
        private OkHttpClient mOkHttpClient = new OkHttpClient();
        private Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String path = msg.obj.toString();
                wv.loadUrl("javascript:upload_ok('" + path + "')");
                super.handleMessage(msg);
            }
        };

        public JavaScriptInterfaceChatApp(MWebViewActivity mWebViewActivity) {
            activity = mWebViewActivity;
            wv = activity.wv;
        }

        @JavascriptInterface
        public void startPlay(String path) {
            if (splayer != null && splayer.isPlaying()) {
                splayer.stop();
                splayer = null;
            }

            final SharedPreferences sp = activity.getSharedPreferences("SP",MODE_PRIVATE);
            String val = sp.getString(path,"");
            if(val == null || "".equals(val)){
                if(!path.startsWith("http")){
                    splayer = new SpeexPlayer(path);
                    splayer.startPlay();
                    return;
                }
                Request request = new Request.Builder()
                        .url(path)
                        .build();
                //发起异步请求，并加入回调
                mOkHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.i("upload", e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        InputStream is = null;
                        byte[] buf = new byte[1024];
                        FileOutputStream fos = null;
                        int len = 0;
                        String filePath = Environment.getExternalStorageDirectory().getPath()+ "/"+new Date().getTime()+".spx";
                        try {
                            is = response.body().byteStream();
                            File file = new File(filePath);
                            fos = new FileOutputStream(file);
                            while ((len = is.read(buf)) != -1) {
                                fos.write(buf, 0, len);
                            }
                            fos.flush();
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString("path", filePath);
                            editor.commit();
                            Log.d("h_bl", "文件下载成功");
                        } catch (Exception e) {
                            Log.d("h_bl", "文件下载失败");
                        } finally {
                            if(is!=null)
                                is.close();
                            if(fos!=null)
                                fos.close();
                            splayer = new SpeexPlayer(filePath);
                            splayer.startPlay();
                        }
                    }
                });
            }else{
                splayer = new SpeexPlayer(val);
                splayer.startPlay();
            }
        }

        @JavascriptInterface
        public void stopPlay() {
            if (splayer != null)
                splayer.stop();
        }

        @JavascriptInterface
        public String pausePlay() {
            return null;
        }

        @JavascriptInterface
        public void startRecord() {
            startRecordTime = new Date().getTime();
            fileName = Environment.getExternalStorageDirectory().getPath() + "/" + startRecordTime.toString() + ".spx";
            // recorderInstance = new PcmRecorder();
            recorderInstance = new SpeexRecorder(fileName);
            Thread th = new Thread(recorderInstance);
            th.start();
            recorderInstance.setRecording(true);
        }

        @JavascriptInterface
        public String stopRecord(String socketid) {
            Long now = new Date().getTime();
            if (recorderInstance != null) {
                recorderInstance.setRecording(false);
                boolean tf = new File(fileName).exists();
                if (tf) {
                    startPlay(fileName);
                    upload(fileName,socketid);
                }
                return fileName;
            }
            return "-2";
        }

        @JavascriptInterface
        public void upload(final String path,final String socketid) {
            if (path == null || path.length() < 5) {
                return;
            }
            //多个文件集合
            MultipartBody.Builder builder = new MultipartBody.Builder();
            //设置为表单类型
            builder.setType(MultipartBody.FORM);
            builder.addFormDataPart("socketid",socketid);
            File file = new File(path);
            RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
            builder.addFormDataPart("spxfile", file.getName(), fileBody);
            Request request = new Request.Builder()
                    .url("http://192.168.199.122:3000/chat/upload")
                    .post(builder.build())
                    .build();
            //发起异步请求，并加入回调
            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.i("upload", e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.i("upload", "ok");
                    Message msg = handler.obtainMessage();
                    msg.obj = path;
                    msg.sendToTarget();
                }
            });
        }
    }
}
