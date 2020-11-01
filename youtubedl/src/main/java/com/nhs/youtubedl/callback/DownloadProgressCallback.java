package com.nhs.youtubedl.callback;

public interface DownloadProgressCallback {
    void onProgressUpdate(float progress, long etaInSeconds);
}
