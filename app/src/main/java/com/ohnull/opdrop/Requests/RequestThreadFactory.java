package com.ohnull.opdrop.Requests;

import com.ohnull.opdrop.Models.ExUrl;

public class RequestThreadFactory {

    public static IRequestThread create(ExUrl exUrl, int threadNum, String cookie) {
        if(exUrl == null || exUrl.getUrl() == null || exUrl.getUrl().isEmpty()) return null;
        if(exUrl.isMethod(ExUrl.METHOD_GET)){
            return new InfinityGetHttpsRequestThread(exUrl, cookie, threadNum);
        }else if(exUrl.isMethod(ExUrl.METHOD_POST)){
            return new InfinityPostHttpsRequestThread(exUrl, cookie, threadNum);
        }else if(exUrl.isMethod(ExUrl.METHOD_TCP)){
            return new InfinityTcpRequestThread(exUrl, threadNum);
        }else if(exUrl.isMethod(ExUrl.METHOD_UDP)){
            return new InfinityUdpRequestThread(exUrl, threadNum);
        }else{
            return null;
        }
    }

}
