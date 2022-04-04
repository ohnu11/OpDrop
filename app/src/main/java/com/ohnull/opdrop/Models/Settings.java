
package com.ohnull.opdrop.Models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Settings {

    @SerializedName("latest_code_version")
    @Expose
    private Integer latestCodeVersion;
    @SerializedName("update_dialog_text")
    @Expose
    private String updateDialogText;

    public Integer getLatestCodeVersion() {
        return latestCodeVersion;
    }

    public void setLatestCodeVersion(Integer latestCodeVersion) {
        this.latestCodeVersion = latestCodeVersion;
    }

    public String getUpdateDialogText() {
        return updateDialogText;
    }

    public void setUpdateDialogText(String updateDialogText) {
        this.updateDialogText = updateDialogText;
    }

    @Override
    public String toString() {
        return "Settings{" +
                "latestCodeVersion=" + latestCodeVersion +
                ", updateDialogText='" + updateDialogText + '\'' +
                '}';
    }
}
