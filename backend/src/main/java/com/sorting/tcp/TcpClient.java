package com.sorting.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

@Component
public class TcpClient {

    private static final Logger log = LoggerFactory.getLogger(TcpClient.class);

    @Value("${sorting.tcp.host:127.0.0.1}")
    private String defaultHost;

    @Value("${sorting.tcp.port:9100}")
    private int defaultPort;

    @Value("${sorting.tcp.timeout:3000}")
    private int timeout;

    @Value("${sorting.tcp.enabled:false}")
    private boolean enabled;

    public boolean sendSorterCommand(String host, int port, int sorterId) {
        if (!enabled) {
            log.info("TCP 通讯已禁用，模拟发送分拣指令: 分拣机{}", sorterId);
            return true;
        }

        Socket socket = null;
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(timeout);

            OutputStream out = socket.getOutputStream();
            byte[] command = buildSwingCommand(sorterId);
            out.write(command);
            out.flush();

            log.info("已向分拣机 {} 发送摆动指令 ({}:{})", sorterId, host, port);

            InputStream in = socket.getInputStream();
            byte[] response = new byte[16];
            int len = in.read(response);

            if (len > 0) {
                log.info("分拣机 {} 响应: {}", sorterId, bytesToHex(response, len));
                return parseResponse(response, len);
            }

            return true;

        } catch (SocketTimeoutException e) {
            log.warn("分拣机 {} 通讯超时 ({}:{})", sorterId, host, port);
            return false;
        } catch (IOException e) {
            log.error("分拣机 {} 通讯失败: {}", sorterId, e.getMessage());
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error("关闭Socket失败: {}", e.getMessage());
                }
            }
        }
    }

    public boolean sendSorterCommand(int sorterId) {
        return sendSorterCommand(defaultHost, defaultPort, sorterId);
    }

    private byte[] buildSwingCommand(int sorterId) {
        byte[] cmd = new byte[8];
        cmd[0] = (byte) 0xAA;
        cmd[1] = (byte) 0x01;
        cmd[2] = (byte) (sorterId & 0xFF);
        cmd[3] = (byte) ((sorterId >> 8) & 0xFF);
        cmd[4] = (byte) 0x01;
        cmd[5] = (byte) 0x00;
        cmd[6] = calculateChecksum(cmd, 6);
        cmd[7] = (byte) 0x55;
        return cmd;
    }

    private byte calculateChecksum(byte[] data, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (data[i] & 0xFF);
        }
        return (byte) (sum & 0xFF);
    }

    private boolean parseResponse(byte[] response, int len) {
        if (len < 4) {
            return false;
        }
        return response[0] == (byte) 0xAA && response[len - 1] == (byte) 0x55;
    }

    private String bytesToHex(byte[] bytes, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
        }
        return sb.toString().trim();
    }
}
