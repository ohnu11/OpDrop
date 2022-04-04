package com.ohnull.opdrop.Helpers;

import android.net.TrafficStats;
import android.os.Process;

import com.ohnull.opdrop.Utils.CommonUtils;
import com.ohnull.opdrop.Utils.EDebug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpeedTestHelper extends Thread {

    private ISpeedCallback callback;

    public SpeedTestHelper(ISpeedCallback callback){
        this.callback = callback;
    }

    @Override
    public void run() {
        try{
            if(callback == null){
                EDebug.l("@ SpeedTestHelper::ISpeedCallback == null");
                return;
            }

            double curDlMbps, curUlMbps, latestDl, latestUl;
            long startMeasTime, testTime, callbackTime;

            List<Double> dlList = new ArrayList<>();
            List<Double> ulList = new ArrayList<>();
            callbackTime = System.currentTimeMillis();

            while (!Thread.interrupted()) {

                startMeasTime = System.currentTimeMillis();
                latestDl = getDlTrafficStatsThrptMb();
                latestUl = getUlTrafficStatsThrptMb();
                Thread.sleep(200);
                testTime = System.currentTimeMillis() - startMeasTime;
                curDlMbps = ((getDlTrafficStatsThrptMb() - latestDl) * 8.0) / (testTime / 1000.0);
                curUlMbps = ((getUlTrafficStatsThrptMb() - latestUl) * 8.0) / (testTime / 1000.0);
                dlList.add(curDlMbps);
                ulList.add(curUlMbps);

                if(dlList.size() >= 20) dlList.remove(0);
                if(ulList.size() >= 20) ulList.remove(0);

                if(System.currentTimeMillis() - callbackTime >= 1000) {
                    if (callback != null) {
                        double avgDl = 0;
                        for (Double dl : dlList) avgDl += dl;
                        avgDl = avgDl / dlList.size();

                        double avgUl = 0;
                        for (Double ul : ulList) avgUl += ul;
                        avgUl = avgUl / ulList.size();

                        callback.onSample(
                                CommonUtils.roundDouble(avgDl, 2),
                                CommonUtils.roundDouble(avgUl, 2)
                        );
                    } else {
                        EDebug.l("@ SpeedTestHelper::ISpeedCallback == null");
                        return;
                    }
                    callbackTime = System.currentTimeMillis();
                }
            }
        }catch (Exception e){
            EDebug.l(e);
        }
    }

    private double getDlTrafficStatsThrptMb() {
        return TrafficStats.getUidRxBytes(Process.myUid()) / 1000000.0;
    }

    private double getUlTrafficStatsThrptMb() {
        return TrafficStats.getUidTxBytes(Process.myUid()) / 1000000.0;
    }

    public interface ISpeedCallback{
        void onSample(double dlMbps, double ulMbps);
    }

}
