package com.quickchat.quickchatapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.quickchat.speex.SpeexPlayer;
import com.quickchat.speex.SpeexRecorder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

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
    private String urlHeader = "http://121.42.186.141:3000";
    //private String urlHeader = "http://192.168.199.122:3000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mwebview);
        wv = (WebView) findViewById(R.id.webView);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new webViewClient());
        wv.addJavascriptInterface(new JavaScriptInterfaceChatApp(this), "mc");
        wv.clearCache(true);
        wv.loadUrl(urlHeader + "/chat");
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

        private boolean downloading = false;

        /**
         * 从网络播放
         *
         * @param fileName
         */
        @JavascriptInterface
        public void startPlay(final String fileName) {
            if (downloading == true) return;
            if (splayer != null && splayer.isPlaying()) {
                splayer.stop();
                splayer = null;
            }
            final SharedPreferences cache = activity.getSharedPreferences("speex", Context.MODE_PRIVATE);
            String cachedPath = cache.getString(fileName, "");
            if (!"".equals(cachedPath)) {
                splayer = new SpeexPlayer(cachedPath);
                splayer.startPlay();
                Log.i("play", "缓存");
                return;
            }
            downloading = true;
            Log.i("play", "非缓存");
            String filePath = urlHeader + "/public/upload/" + fileName + ".spx";
            Request request = new Request.Builder()
                    .url(filePath)
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
                    String filePath = Environment.getExternalStorageDirectory().getPath() + "/" + new Date().getTime() + ".spx";
                    try {
                        is = response.body().byteStream();
                        File file = new File(filePath);
                        fos = new FileOutputStream(file);
                        while ((len = is.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                        }
                        fos.flush();
                        Log.d("h_bl", "文件下载成功");
                    } catch (Exception e) {
                        Log.d("h_bl", "文件下载失败");
                    } finally {
                        if (is != null)
                            is.close();
                        if (fos != null)
                            fos.close();
                        splayer = new SpeexPlayer(filePath);
                        splayer.startPlay();
                        SharedPreferences.Editor editor = cache.edit();
                        editor.putString(fileName, filePath);
                        editor.commit();
                        downloading = false;
                    }
                }
            });
        }

        /**
         * 本地播放
         *
         * @param path 本地路径
         */
        private void startPlayForLocal(String path) {
            splayer = new SpeexPlayer(path);
            splayer.startPlay();
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
        public String stopRecord(final String socketid) {
            Long now = new Date().getTime();
            if (recorderInstance != null) {
                recorderInstance.setRecording(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        boolean tf = new File(fileName).exists();
                        if (tf) {
                            //startPlayForLocal(fileName);
                            upload(fileName, socketid);
                        }
                    }
                }).start();
                return fileName;
            }
            return "-2";
        }

        @JavascriptInterface
        public void upload(final String path, final String socketid) {
            if (path == null || path.length() < 5) {
                return;
            }
            //多个文件集合
            MultipartBody.Builder builder = new MultipartBody.Builder();
            //设置为表单类型
            builder.setType(MultipartBody.FORM);
            builder.addFormDataPart("socketid", socketid);
            File file = new File(path);
            RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
            builder.addFormDataPart("spxfile", file.getName(), fileBody);
            Request request = new Request.Builder()
                    .url(urlHeader + "/chat/upload")
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
