package com.nhs.youtubedl;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhs.youtubedl.Constants;
import com.nhs.youtubedl.callback.DownloadProgressCallback;
import com.nhs.youtubedl.dto.VideoInfo;
import com.nhs.youtubedl.enums.UpdateStatus;
import com.nhs.youtubedl.threads.StreamGobbler;
import com.nhs.youtubedl.threads.StreamProcessExtractor;
import com.nhs.youtubedl.utils.SharedPrefsHelper;
import com.nhs.youtubedl.utils.ZipUtils;

import org.apache.commons.io.FileUtils;

/**
 * Created by Henry on 2020. 11. 01..
 */
public class YoutubeDL {
    private static final YoutubeDL INSTANCE = new YoutubeDL();
    protected static final ObjectMapper objectMapper = new ObjectMapper();

    private boolean isInitialized;
    private File pythonPath;
    private File youtubeDLPath;
    private File binDir;

    private String ENV_LD_LIBRARY_PATH;
    private String ENV_SSL_CERT_FILE;

    private YoutubeDL() {
        this.isInitialized = false;
        this.pythonPath = null;
        this.youtubeDLPath = null;
        this.binDir = null;
    }

    public static YoutubeDL getInstance() {
        return INSTANCE;
    }

    synchronized public void init(Context appContext) throws YoutubeDLException {
        if (isInitialized) return;

        File baseDir = new File(appContext.getFilesDir(), Constants.baseName);
        if(!baseDir.exists()) baseDir.mkdir();

        File packagesDir = new File(baseDir, Constants.packagesRoot);
        binDir = new File(packagesDir, "usr/bin");
        pythonPath = new File(packagesDir, Constants.pythonBin);

        File youtubeDLDir = new File(baseDir, Constants.youtubeDLName);
        youtubeDLPath = new File(youtubeDLDir, Constants.youtubeDLBin);

        ENV_LD_LIBRARY_PATH = packagesDir.getAbsolutePath() + "/usr/lib";
        ENV_SSL_CERT_FILE = packagesDir.getAbsolutePath() + "/usr/etc/tls/cert.pem";

        initPython(appContext, packagesDir);
        initYoutubeDL(appContext, youtubeDLDir);

        isInitialized = true;
    }

    protected void initYoutubeDL(Context appContext, File youtubeDLDir) throws YoutubeDLException {
        if (!youtubeDLDir.exists()) {
            youtubeDLDir.mkdirs();
            try {
                ZipUtils.unzip(appContext.getResources().openRawResource(R.raw.youtube_dl), youtubeDLDir);
            } catch (Exception e) {
                FileUtils.deleteQuietly(youtubeDLDir);
                throw new YoutubeDLException("failed to initialize", e);
            }
        }
    }

    protected void initPython(Context appContext, File packagesDir) throws YoutubeDLException {
        if(!pythonPath.exists()) {
            if (!packagesDir.exists()) {
                packagesDir.mkdirs();
            }

            try {
                ZipUtils.unzip(appContext.getResources().openRawResource(R.raw.python3_7_arm), packagesDir);
                updatePython(appContext, pythonPath.getName());
            } catch (Exception e) {
                FileUtils.deleteQuietly(pythonPath);
                throw new YoutubeDLException("failed to initialize", e);
            }
            pythonPath.setExecutable(true);
        }
    }

    private boolean shouldUpdatePython(@NonNull Context appContext, @NonNull String version) {
        return !version.equals(SharedPrefsHelper.get(appContext, Constants.pythonLibVersion));
    }

    private void updatePython(@NonNull Context appContext, @NonNull String version) {
        SharedPrefsHelper.update(appContext, Constants.pythonLibVersion, version);
    }

    private void assertInit() {
        if (!isInitialized) throw new IllegalStateException("instance not initialized");
    }

    public VideoInfo getInfo(String url) throws YoutubeDLException, InterruptedException {
        YoutubeDLRequest request = new YoutubeDLRequest(url);
        return getInfo(request);
    }

    @NonNull
    public VideoInfo getInfo(YoutubeDLRequest request) throws YoutubeDLException, InterruptedException {
        request.addOption("--dump-json");
        YoutubeDLResponse response = execute(request, null);

        VideoInfo videoInfo;
        try {
            videoInfo = objectMapper.readValue(response.getOut(), VideoInfo.class);
        } catch (IOException e) {
            throw new YoutubeDLException("Unable to parse video information", e);
        }

        if(videoInfo == null){
            throw new YoutubeDLException("Failed to fetch video information");
        }

        return videoInfo;
    }

    public YoutubeDLResponse execute(YoutubeDLRequest request) throws YoutubeDLException, InterruptedException {
        return execute(request, null);
    }

    public YoutubeDLResponse execute(YoutubeDLRequest request, @Nullable DownloadProgressCallback callback) throws YoutubeDLException, InterruptedException {
        assertInit();

        // disable caching unless explicitly requested
        if(request.getOption("--cache-dir") == null){
            request.addOption("--no-cache-dir");
        }

        YoutubeDLResponse youtubeDLResponse;
        Process process;
        int exitCode;
        StringBuffer outBuffer = new StringBuffer(); //stdout
        StringBuffer errBuffer = new StringBuffer(); //stderr
        long startTime = System.currentTimeMillis();

        List<String> args = request.buildCommand();
        List<String> command = new ArrayList<>();
        command.addAll(Arrays.asList(pythonPath.getAbsolutePath(), youtubeDLPath.getAbsolutePath()));
        command.addAll(args);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Map<String, String> env = processBuilder.environment();
        env.put("LD_LIBRARY_PATH", ENV_LD_LIBRARY_PATH);
        env.put("SSL_CERT_FILE", ENV_SSL_CERT_FILE);
        env.put("PATH",  System.getenv("PATH") + ":" + binDir.getAbsolutePath());

        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new YoutubeDLException(e);
        }

        InputStream outStream = process.getInputStream();
        InputStream errStream = process.getErrorStream();

        StreamProcessExtractor stdOutProcessor = new StreamProcessExtractor(outBuffer, outStream, callback);
        StreamGobbler stdErrProcessor = new StreamGobbler(errBuffer, errStream);

        try {
            stdOutProcessor.join();
            stdErrProcessor.join();
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            process.destroy();
            throw e;
        }

        String out = outBuffer.toString();
        String err = errBuffer.toString();

        if (exitCode > 0) {
            throw new YoutubeDLException(err);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        youtubeDLResponse = new YoutubeDLResponse(command, exitCode, elapsedTime, out, err);

        return youtubeDLResponse;
    }

    synchronized public UpdateStatus updateYoutubeDL(Context appContext) throws YoutubeDLException {
        assertInit();
        try {
            return YoutubeDLUpdater.update(appContext);
        } catch (IOException e) {
            throw new YoutubeDLException("failed to update youtube-dl", e);
        }
    }

    @Nullable
    public String version(Context appContext) {
        return YoutubeDLUpdater.version(appContext);
    }

}
