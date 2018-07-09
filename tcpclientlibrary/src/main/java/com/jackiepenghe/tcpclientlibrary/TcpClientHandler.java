package com.jackiepenghe.tcpclientlibrary;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ThreadFactory;

import static com.jackiepenghe.tcpclientlibrary.TcpClient.ON_RECEIVE_DATA;
import static com.jackiepenghe.tcpclientlibrary.TcpClient.ON_RECEIVE_DATA_TIMEOUT;
import static com.jackiepenghe.tcpclientlibrary.TcpClient.ON_REQUEST_FAIL;
import static com.jackiepenghe.tcpclientlibrary.TcpClient.RE_CONN_FAIL;
import static com.jackiepenghe.tcpclientlibrary.TcpClient.RE_CONN_SUCCESS;
import static com.jackiepenghe.tcpclientlibrary.TcpClient.SOCKET_CLOSED;
import static com.jackiepenghe.tcpclientlibrary.TcpClient.SOCKET_OK;

/**
 * Tcp客户端的Handler
 *
 * @author jackie
 */
public class TcpClientHandler extends Handler {

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            return new Thread(runnable);
        }
    };

    private long sendTime;
    private long recTime;
    private byte[] sendData;
    private int readTimeout;
    /**
     * 心跳数据
     */
    private byte[] mBreath;
    /**
     * 心跳间隔时间
     */
    private int mBreathTime;
    private BaseResponseCallback baseResponseCallback;
    private BaseRequestCallback baseRequestCallback;
    /**
     * 连接超时时间 默认设置为10s
     */
    private int mConnTimeout = 10 * 1000;

    /**
     * 服务器ip
     */
    private String mServerIp;
    private int mServerPort;

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case SOCKET_OK:
                //发送数据
                Socket socket = (Socket) msg.obj;
                request(socket);
                break;
            case SOCKET_CLOSED:
                //尝试进行重新连接
                connectServerInner();
                break;
            case RE_CONN_SUCCESS:
                //发送数据
                socket = (Socket) msg.obj;
                request(socket);
                receive(socket);
                sendBreadth(socket);
                break;
            case ON_RECEIVE_DATA:
                if (baseResponseCallback != null) {
                    LoopBuffer instance = LoopBuffer.getInstance();
                    byte[] read = instance.read(instance.count());
                    instance.remove(read.length);
                    baseResponseCallback.onDataReceived(read);
                }
                break;
            case RE_CONN_FAIL:
                Throwable connE = (Throwable) msg.obj;
                throwBack(connE);
                break;
            case ON_REQUEST_FAIL:
                Throwable requestE = (Throwable) msg.obj;
                throwBack(requestE);
                break;

            case ON_RECEIVE_DATA_TIMEOUT:
                if (baseRequestCallback != null) {
                    baseRequestCallback.onTimeout();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 内部处理：发送请求
     *
     * @param socket Socket
     */
    private synchronized void request(final Socket socket) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream os = socket.getOutputStream();
                    if (sendData != null) {
                        //发送请求
                        os.write(sendData);
                        os.flush();
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                    Message message = obtainMessage();
                    message.what = ON_REQUEST_FAIL;
                    message.obj = e;
                    sendMessage(message);
                }
            }
        };
        THREAD_FACTORY.newThread(runnable).start();
        sendTime = System.currentTimeMillis();
        if (readTimeout != 0) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (recTime < sendTime) {
                        sendEmptyMessage(ON_RECEIVE_DATA_TIMEOUT);
                    }
                }
            }, readTimeout + 1000);
        }
    }

    /**
     * 开启线程连接服务器
     */
    private void connectServerInner() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket();
                    //设置读取超时时间
                    socket.setSoTimeout(10 * 1000);
                    socket.connect(new InetSocketAddress(mServerIp, mServerPort), mConnTimeout);
                    //连接服务器成功
                    //添加到列表
                    TcpClientManager.addTCP(socket);
                    Message message = obtainMessage();
                    message.what = RE_CONN_SUCCESS;
                    message.obj = socket;
                    sendMessage(message);
                } catch (final IOException e) {
                    e.printStackTrace();
                    Message message = obtainMessage();
                    message.what = RE_CONN_FAIL;
                    message.obj = e;
                    sendMessage(message);
                }
            }
        };
        THREAD_FACTORY.newThread(runnable).start();
    }

    /**
     * 内部处理：接收数据
     *
     * @param socket Socket
     */
    private void receive(final Socket socket) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (socket.isConnected() && !socket.isClosed()) {
                    //接收socket数据
                    try {
                        InputStream is = socket.getInputStream();
                        byte[] data = new byte[1024];
                        int len = is.read(data);
                        if (len < 0) {
                            continue;
                        }
                        byte[] newData = new byte[len];
                        System.arraycopy(data, 0, newData, 0, len);
                        LoopBuffer.getInstance().write(newData);
                        recTime = System.currentTimeMillis();
                        sendEmptyMessage(ON_RECEIVE_DATA);
                    } catch (SocketTimeoutException ignored) {
                    } catch (SocketException e) {
                        if (socket.isClosed()) {
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        THREAD_FACTORY.newThread(runnable).start();
    }

    /**
     * 内部处理：发送心跳
     */
    private void sendBreadth(final Socket socket) {
        if (mBreath == null) {
            return;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (socket != null && !socket.isClosed()) {
                    try {
                        OutputStream os = socket.getOutputStream();
                        //发送心跳
                        os.write(mBreath);
                        os.flush();
                        Thread.sleep(mBreathTime);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                        if (socket.isClosed()) {
                            break;
                        }
                    }
                }
            }
        };
        THREAD_FACTORY.newThread(runnable).start();
    }

    /**
     * 将异常放出，让外面自行处理
     *
     * @param throwable 异常
     */
    private void throwBack(Throwable throwable) {
        if (baseResponseCallback != null) {
            baseResponseCallback.onFailed(throwable);
        }
        if (baseRequestCallback != null) {
            baseRequestCallback.onFailed(throwable);
        }
    }

    public void setSendData(byte[] sendData) {
        this.sendData = sendData;
    }

    public void setReadTimeOut(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setResponseCallback(BaseResponseCallback baseResponseCallback) {
        this.baseResponseCallback = baseResponseCallback;
    }

    public void setRequestCallback(BaseRequestCallback baseRequestCallback) {
        this.baseRequestCallback = baseRequestCallback;
    }

    public void setmServerIp(String mServerIp) {
        this.mServerIp = mServerIp;
    }

    public void setmServerPort(int mServerPort) {
        this.mServerPort = mServerPort;
    }

    public void setmConnTimeout(int mConnTimeout) {
        this.mConnTimeout = mConnTimeout;
    }

    public void setmBreath(byte[] mBreath) {
        this.mBreath = mBreath;
    }

    public void setmBreathTime(int mBreathTime) {
        this.mBreathTime = mBreathTime;
    }
}
