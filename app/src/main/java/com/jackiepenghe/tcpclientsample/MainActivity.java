package com.jackiepenghe.tcpclientsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.jackiepenghe.tcpclientlibrary.BaseRequestCallback;
import com.jackiepenghe.tcpclientlibrary.BaseResponseCallback;
import com.jackiepenghe.tcpclientlibrary.TcpClient;
import com.jackiepenghe.tcpclientsample.adapter.ChatAdapter;
import com.jackiepenghe.tcpclientsample.bean.ChatMess;

import java.util.ArrayList;

/**
 * @author jackie
 */
public class MainActivity extends AppCompatActivity {


    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PORT = 8266;
    private static final String IP_ADDRESS = "192.168.4.1";

    private ChatAdapter chatAdapter;
    private EditText etInput;
    private TcpClient tcpClient;
    private ArrayList<ChatMess> chatMessList;
    private LinearLayoutManager chatLayoutManager;
    private BaseResponseCallback responseCallback = new BaseResponseCallback() {
        @Override
        public void onDataReceived(byte[] data) {
            ChatMess chatMess = new ChatMess();
            chatMess.setType(1);
            chatMess.setMesg(new String(data));
            chatMessList.add(chatMess);
            chatAdapter.notifyDataSetChanged();
            chatLayoutManager.scrollToPosition(chatMessList.size() - 1);
        }

        @Override
        public void onFailed(Throwable throwable) {
            handlerError(throwable);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initNet();
        initView();
    }

    private void initNet() {
        tcpClient = TcpClient.build()
                .server(IP_ADDRESS, PORT)
                .breath("heart".getBytes(), 2 * 1000)
                .connTimeout(5 * 1000);
    }

    private void initView() {
        RecyclerView rvChat = findViewById(R.id.recycler_chat);
        chatLayoutManager = new LinearLayoutManager(this);
        rvChat.setLayoutManager(chatLayoutManager);
        chatMessList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, chatMessList);
        rvChat.setAdapter(chatAdapter);

        etInput =  findViewById(R.id.et_input);
        Button btnSend = findViewById(R.id.btn_send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String sendMess = etInput.getText().toString();
                ChatMess chatMess = new ChatMess();
                chatMess.setType(2);
                chatMess.setMesg(sendMess);
                chatMessList.add(chatMess);
                chatAdapter.notifyDataSetChanged();
                chatLayoutManager.scrollToPosition(chatMessList.size() - 1);

                tcpClient.request(sendMess.getBytes(), 8000, new BaseRequestCallback() {
                    @Override
                    public void onTimeout() {
                        Log.e(TAG, "onTimeout:请求超时，稍后重试 ,关闭连接 ");
                        TcpClient.closeTcp(IP_ADDRESS, PORT);
                    }

                    @Override
                    public void onFailed(Throwable throwable) {
                        handlerError(throwable);
                    }
                }, responseCallback);
            }
        });
    }

    private void handlerError(Throwable throwable) {
        Log.e(TAG, "handlerError: 网络访问失败:" + throwable.getMessage());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TcpClient.closeTcp(IP_ADDRESS, PORT);
    }
}
