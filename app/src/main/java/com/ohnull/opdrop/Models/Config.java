
package com.ohnull.opdrop.Models;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Config {

    public static AtomicBoolean IS_HIGH_CPU_USAGE = new AtomicBoolean(false);

    @SerializedName("version")
    @Expose
    private Integer version;
    @SerializedName("max_threads")
    @Expose
    private Integer maxThreads;
    @SerializedName("urls")
    @Expose
    private List<ExUrl> exUrls = null;
    @SerializedName("settings")
    @Expose
    private Settings settings;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(Integer maxThreads) {
        this.maxThreads = maxThreads;
    }

    public List<ExUrl> getUrls() {
        return exUrls;
    }

    public void setUrls(List<ExUrl> exUrls) {
        this.exUrls = exUrls;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String toString() {
        return "Config{" +
                "version=" + version +
                ", maxThreads=" + maxThreads +
                ", urls=" + exUrls +
                ", settings=" + settings +
                '}';
    }
}
