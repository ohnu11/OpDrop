package com.ohnull.opdrop.Requests;

import com.ohnull.opdrop.Models.Config;
import com.ohnull.opdrop.Models.ExUrl;
import com.ohnull.opdrop.Utils.CommonUtils;
import com.ohnull.opdrop.Utils.EDebug;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Random;

public class InfinityTcpRequestThread extends Thread implements IRequestThread {

    ExUrl exUrl;
    int sleepCounter = 1;
    int threadNum;
    boolean stopThread = false;

    public InfinityTcpRequestThread(ExUrl exUrl, int threadNum) {
        this.exUrl = exUrl;
        this.threadNum = threadNum;
        EDebug.l("InfinityTcpRequestThread: INIT -> #" + threadNum + " | " + exUrl.getUrl());
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
        EDebug.l("InfinityTcpRequestThread: RUN -> #" + threadNum + " | " + exUrl.getUrl());
        Random rnd = new Random();
        Socket socket = null;
        String data = exUrl.getData();
        if(data == null || data.isEmpty()){
            data = "name1=value1&name2=value&" + CommonUtils.generateRandomString(1024);
        }

        URI uri;
        try {
            uri = new URI(exUrl.getUrl());
        } catch (Exception e) {
            EDebug.l(e);
            return;
        }

        OutputStream outputStream = null;
        BufferedReader bufferedReader = null;
        String firstLine;
        long timeoutRead;
        InetSocketAddress address = new InetSocketAddress(uri.getHost(), uri.getPort());

        while (!stopThread) {
            try {
                firstLine = "";
                if (Config.IS_HIGH_CPU_USAGE.get()) {
                    Thread.sleep(1000L * sleepCounter++);
                } else {
                    sleepCounter = Math.max(--sleepCounter, 1);
                }
                socket = new Socket();
                socket.setSoTimeout(5000);
                socket.connect(address, 5000);
                outputStream = socket.getOutputStream();
                outputStream.write(data.getBytes());
                outputStream.flush();

                try {
                    bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    timeoutRead = System.currentTimeMillis();
                    firstLine = bufferedReader.readLine();
                    while (bufferedReader.readLine() != null && System.currentTimeMillis() - timeoutRead < 1000){}
                }catch (Exception e){
//                    EDebug.l(e);
                }

//                EDebug.l("#" + threadNum +" | url: " + exUrl + " | sleepCounter: " + sleepCounter + " | firstLine: " + firstLine);
            } catch (Exception e) {
//                EDebug.l(e);
                try {
                    Thread.sleep(rnd.nextInt(1000));
                } catch (Exception e2) { }
            }finally {
                try {
                    if (outputStream != null) outputStream.close();
                    if (bufferedReader != null) bufferedReader.close();
                    if (socket != null) socket.close();
                }catch (Exception e2){}
            }
        }

        EDebug.l("InfinityTcpRequestThread: FINISHED -> #" + threadNum + " | " + exUrl.getUrl());
    }
}
