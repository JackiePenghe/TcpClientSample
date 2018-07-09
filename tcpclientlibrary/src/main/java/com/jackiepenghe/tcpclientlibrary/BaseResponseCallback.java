package com.jackiepenghe.tcpclientlibrary;

/**
 * 读取数据返回接口
 * @author jackie
 */

public abstract class BaseResponseCallback implements AbsBaseCallback {
    /**
     * 接收到数据时进行的回调
     * @param data 接收到的数据
     */
    public abstract void onDataReceived(byte[] data);
}