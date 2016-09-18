package com.qc.quickchat;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    TextView flag_boy;
    TextView flag_girl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        flag_boy = (TextView) findViewById(R.id.flag_boy);
        flag_boy = (TextView) findViewById(R.id.flag_boy);
        new Runnable() {
            @Override
            public void run() {
                new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                    }
                }.handleMessage(new Message());
            }
        };
    }

    public void changeSex(View view) {
        int id = view.getId();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==1){
                flag_boy.setText("haha");
                super.handleMessage(msg);
            }
        }

    };

    class myThread implements Runnable {
        @Override
        public void run() {
            Message msg = Message.obtain();
            msg.what = 1;
            msg.obj = "123";
            handler.handleMessage(msg);
        }
    }
}
