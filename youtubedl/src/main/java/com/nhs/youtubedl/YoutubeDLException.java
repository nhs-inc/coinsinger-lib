package com.nhs.youtubedl;

/**
 * Created by Henry on 2020. 11. 01..
 */
public class YoutubeDLException extends Exception {
    public YoutubeDLException(String message) {
        super(message);
    }
    public YoutubeDLException(String message, Throwable e) {
        super(message, e);
    }
    public YoutubeDLException(Throwable e) {
        super(e);
    }
}
