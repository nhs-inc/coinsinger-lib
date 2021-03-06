package com.nhs.youtubedl.threads;

import android.util.Log;

import com.nhs.youtubedl.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class StreamGobbler extends Thread {
    private static final String TAG = "StreamGobbler";

    private InputStream stream;
    private StringBuffer buffer;

    public StreamGobbler(StringBuffer buffer, InputStream stream) {
        this.stream = stream;
        this.buffer = buffer;
        start();
    }

    public void run() {
        try {
            Reader in = new InputStreamReader(stream, "UTF-8");
            int nextChar;
            while ((nextChar = in.read()) != -1) {
                this.buffer.append((char) nextChar);
            }
        } catch (IOException e) {
            if(BuildConfig.DEBUG) Log.e(TAG, "failed to read stream", e);
        }
    }
}