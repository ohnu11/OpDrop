
package com.ohnull.opdrop.Models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ExUrl {

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_TCP = "TCP";
    public static final String METHOD_UDP = "UDP";

    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("method")
    @Expose
    private String method;
    @SerializedName("data")
    @Expose
    private String data;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getData() {
        if(data != null && !data.isEmpty()){
            data = data.replaceAll("\\\\", "");
        }
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isMethod(String method){
        return getMethod().toLowerCase().contentEquals(method.toLowerCase());
    }

    @Override
    public String toString() {
        return "Url{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
