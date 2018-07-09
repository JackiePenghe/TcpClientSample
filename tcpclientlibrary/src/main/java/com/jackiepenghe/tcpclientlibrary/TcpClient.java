package com.jackiepenghe.tcpclientlibrary;

import android.os.Message;
import android.support.annotation.NonNull;

import java.net.Socket;
import java.util.concurrent.ThreadFactory;

/**
 * Tcp客户端
 *
 * @author jackie
 */
@SuppressWarnings("unused")
public class TcpClient {
    /**
     * Handler
     */
    private static TcpClientHandler mHandler = new TcpClientHandler();

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            return new Thread(runnable);
        }
    };
    /**
     * 服务器ip
     */
    private String mServerIp;
    /**
     * 服务器端口号
     */
    private int mServerPort;
    private static TcpClient client;

    private TcpClient() {
    }

    /**
     * 获取TCPClient的实例
     *
     * @return TCPClient的实例
     */
    public static TcpClient build() {
        if (client == null) {
            synchronized (TcpClient.class) {
                if (client == null) {
                    client = new TcpClient();
                }
            }
        }
        return client;
    }

    static final int SOCKET_OK = 10003;
    static final int SOCKET_CLOSED = 10004;
    static final int RE_CONN_SUCCESS = 10005;
    static final int RE_CONN_FAIL = 10006;
    static final int ON_RECEIVE_DATA = 10007;
    static final int ON_REQUEST_FAIL = 10008;
    static final int ON_RECEIVE_DATA_TIMEOUT = 10009;

    /**
     * 设置目标服务器的ip和端口号
     *
     * @param ip   ip地址
     * @param port 端口号
     * @return TcpClient
     */
    public TcpClient server(String ip, int port) {
        mHandler.setmServerIp(ip);
        mHandler.setmServerPort(port);
        mServerIp = ip;
        mServerPort = port;
        return this;
    }

    /**
     * 设置连接超时时间
     *
     * @param connTimeout 连接超时时间
     * @return TcpClient
     */
    public TcpClient connTimeout(int connTimeout) {
        mHandler.setmConnTimeout(connTimeout);
        return this;
    }

    /**
     * 设置心跳数据
     *
     * @param breath     心跳内容
     * @param breathTime 心跳间隔时间
     * @return TcpClient
     */
    public TcpClient breath(byte[] breath, int breathTime) {
        mHandler.setmBreath(breath);
        mHandler.setmBreathTime(breathTime);
        return this;
    }

    //----------------------发送数据：判断连接是否存在；连接存在 则直接发送数据，连接不存在，尝试进行连接，连接成功发送数据

    /**
     * 发送请求到服务器
     *
     * @param data     数据
     * @param timeout  请求响应超时时间
     * @param request  请求状态
     * @param response 接收数据
     */
    public synchronized void request(byte[] data, int timeout, BaseRequestCallback request, BaseResponseCallback response) {
        mHandler.setReadTimeOut(timeout);
        mHandler.setRequestCallback(request);
        mHandler.setSendData(data);
        mHandler.setResponseCallback(response);
        isServerOpen();
    }
    //----------------------接收数据：判断连接是否存在；连接存在 则不进行操作，连接不存在，尝试进行连接，连接成功接收数据

    /**
     * 接收数据
     *
     * @param baseResponseCallBack 接收数据
     */
    public void onResponse(BaseResponseCallback baseResponseCallBack) {
        mHandler.setResponseCallback(baseResponseCallBack);
        isServerOpen();
    }

    /**
     * 关闭指定连接
     *
     * @param ip 服务器IP地址
     * @param port 服务器端口
     */
    public static void closeTcp(final String ip, final int port) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Socket socket = TcpClientManager.queryTarget(ip, port);
                if (socket != null) {
                    TcpClientManager.removeTcp(socket);
                }
            }
        };
        THREAD_FACTORY.newThread(runnable).start();
    }

    /**
     * 关闭当前连接
     */
    public void closeTcp() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Socket socket = TcpClientManager.queryTarget(mServerIp, mServerPort);
                if (socket != null) {
                    TcpClientManager.removeTcp(socket);
                }
            }
        };
        THREAD_FACTORY.newThread(runnable).start();
    }

    public void closeAll(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                    TcpClientManager.closeAll();
            }
        };
        THREAD_FACTORY.newThread(runnable).start();
    }

    /**
     * 判断网络是否打开的操作
     */
    private void isServerOpen() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //查看指定服务器是否已经连接
                Socket socket = TcpClientManager.queryTarget(mServerIp, mServerPort);
                if (socket != null) {
                    Message message = mHandler.obtainMessage();
                    message.what = SOCKET_OK;
                    message.obj = socket;
                    mHandler.sendMessage(message);
                } else {
                    Message message = mHandler.obtainMessage();
                    message.what = SOCKET_CLOSED;
                    mHandler.sendMessage(message);
                }
            }
        };
        THREAD_FACTORY.newThread(runnable).start();
    }
}
