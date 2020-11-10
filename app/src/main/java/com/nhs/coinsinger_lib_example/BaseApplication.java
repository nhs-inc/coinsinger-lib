package com.nhs.coinsinger_lib_example;

import android.app.Application;

import com.nhs.youtubedl.YoutubeDL;
import com.nhs.youtubedl.YoutubeDLException;

/**
 * Created by Henry on 2020. 11. 10..
 */
public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        try {
            YoutubeDL.getInstance().init(this);
        } catch (YoutubeDLException e) {
            e.printStackTrace();
        }
    }
}
