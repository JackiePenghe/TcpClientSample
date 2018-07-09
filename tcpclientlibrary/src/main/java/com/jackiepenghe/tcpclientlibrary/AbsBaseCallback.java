package com.jackiepenghe.tcpclientlibrary;

/**
 * 基本回调接口
 *
 * @author jackie
 */
public interface AbsBaseCallback {
    /**
     * 失败
     * @param throwable 异常
     */
    void onFailed(Throwable throwable);
}