package com.jackiepenghe.tcpclientlibrary;

/**
 * 接收数据callback
 * @author jackie
 */
public abstract class BaseRequestCallback implements AbsBaseCallback {
    /**
     * 超时
     */
    public abstract void onTimeout();
}
