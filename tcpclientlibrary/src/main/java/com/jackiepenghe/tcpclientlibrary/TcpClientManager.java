package com.jackiepenghe.tcpclientlibrary;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * TcpClient 管理类
 *
 * @author jackie
 */

public class TcpClientManager {
    private static List<Socket> managerList = new ArrayList<>();

    /**
     * 添加socket 到列表
     *
     * @param socket Socket
     */
    public static void addTCP(Socket socket) {
        managerList.add(socket);
    }

    /**
     * 从列表移除socket
     *
     * @param socket Socket
     */
    public static void removeTcp(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                managerList.remove(socket);
            }
        }
    }

    /**
     * 查询指定ip地址的服务器是否在列表中
     *
     * @param ip 服务器IP地址
     * @param port 服务器端口
     * @return Socket
     */
    public static Socket queryTarget(String ip, int port) {
        if (managerList.size() > 0) {
            for (Socket inner :
                    managerList) {
                InetAddress inetAddress = inner.getInetAddress();
                if (inetAddress != null
                        && inetAddress.getHostName().equals(ip)
                        && inner.getPort() == port) {
                    return inner;
                }
            }
        }
        return null;
    }

    /**
     * 关闭所有Tcp客户端
     */
    public static void closeAll() {
        if (managerList.size() > 0) {
            for (Socket inner :
                    managerList) {
                removeTcp(inner);
            }
        }
        managerList.clear();
    }
}
