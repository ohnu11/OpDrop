package com.ohnull.opdrop.Requests;

import com.ohnull.opdrop.Models.Config;
import com.ohnull.opdrop.Models.ExUrl;
import com.ohnull.opdrop.Utils.CommonUtils;
import com.ohnull.opdrop.Utils.EDebug;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Random;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class InfinityPostHttpsRequestThread extends Thread implements IRequestThread {

    ExUrl exUrl;
    String cookieStr;
    int sleepCounter = 1;
    SSLSocketFactory sslFactory;
    SocketFactory hFactory;
    Socket socket;
    int threadNum;
    boolean stopThread = false;
    URI uri;
    Random rnd = new Random();

    public InfinityPostHttpsRequestThread(ExUrl exUrl, String cookieStr, int threadNum) {
        this.exUrl = exUrl;
        this.cookieStr = cookieStr;
        sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        hFactory = SocketFactory.getDefault();
        this.threadNum = threadNum;
        try {
            uri = new URI(exUrl.getUrl());
        } catch (Exception e) {
            EDebug.l(e);
        }
        EDebug.l("InfinityPostHttpsRequestThread: INIT -> #" + threadNum + " | " + exUrl.getUrl());
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
        EDebug.l("InfinityPostHttpsRequestThread: RUN -> #" + threadNum + " | " + exUrl.getUrl());
        String responseStr = "";
        OutputStreamWriter out = null;
        BufferedReader in = null;
        int port = uri.getPort();
        if(port == -1) port = uri.getScheme().startsWith("https") ? 443 : 80;
        InetSocketAddress address;
        try {
            address = new InetSocketAddress(InetAddress.getByName(exUrl.getUrl()).getHostName(), port);
        } catch (Exception e) {
            EDebug.l(e);
            address = new InetSocketAddress(uri.getHost(), port);
        }
        String initData = exUrl.getData();
        if(initData == null || initData.isEmpty()){
            initData = "name1=value1&name2=value";
        }
        String finalData;
        while (!stopThread) {
            try {
                responseStr = "";
                if (Config.IS_HIGH_CPU_USAGE.get()) {
                    Thread.sleep(1000L * sleepCounter++);
                } else {
                    sleepCounter = Math.max(--sleepCounter, 1);
                }

                if(port == 443) {
                    socket = sslFactory.createSocket();
                }else{
                    socket = hFactory.createSocket();
                }
                socket.setSoTimeout(5000);
                socket.connect(address, 5000);
                if(socket instanceof SSLSocket) {
                    ((SSLSocket) socket).startHandshake();
                }

                finalData = initData + prepareRndPostDataEnd();
                out = new OutputStreamWriter(socket.getOutputStream());

                out.write("POST "+uri.getPath()+" HTTP/1.1\r\n");
                out.write("Host: "+uri.getHost()+"\r\n");
                out.write("Connection: Keep-Alive\r\n");
                out.write("Cookie: "+cookieStr+"\r\n");
                out.write("Content-Length: "+ finalData.length()+"\r\n");
                out.write("User-Agent: "+ CommonUtils.USER_AGENTS[rnd.nextInt(CommonUtils.USER_AGENTS.length)] +"\r\n");
                out.write("\r\n");
                out.write(finalData);
                out.flush();

                try{
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    responseStr = in.readLine();
                }catch (Exception e){
                    //EDebug.l(e);
                }

                EDebug.l("#" + threadNum
                        + " | url: " + exUrl.getUrl()
                        + " | method: " + exUrl.getMethod()
                        + " | data: " + finalData
                        + " | code: " + responseStr
                        + " | sleepCounter: " + sleepCounter);
            } catch (Exception e) {
                //EDebug.l(e);
                try {
                    Thread.sleep(rnd.nextInt(1000));
                } catch (Exception e2) { }
            }finally {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (socket != null) socket.close();
                }catch (Exception e2){}
            }
        }

        EDebug.l("InfinityPostHttpsRequestThread: FINISHED -> #" + threadNum + " | " + exUrl.getUrl());
    }

    private String prepareRndPostDataEnd() {
        return "&exId=" + rnd.nextInt(100000);
    }
}
