package com.nhs.youtubedl;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.nhs.youtubedl.enums.UpdateStatus;
import com.nhs.youtubedl.utils.SharedPrefsHelper;
import com.nhs.youtubedl.utils.ZipUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

class YoutubeDLUpdater {
    private static final String releasesUrl = "https://api.github.com/repos/nhs-inc/coinsinger-youtube-dl/releases/latest";

    private YoutubeDLUpdater() {}

    static UpdateStatus update(Context appContext) throws IOException, YoutubeDLException {
        JsonNode json = checkForUpdate(appContext);
        if(null == json) return UpdateStatus.ALREADY_UP_TO_DATE;

        String downloadUrl = getDownloadUrl(json);
        File file = download(appContext, downloadUrl);

        File youtubeDLDir = null;
        try {
            youtubeDLDir = getYoutubeDLDir(appContext);
            //purge older version
            FileUtils.deleteQuietly(youtubeDLDir);
            //install newer version
            youtubeDLDir.mkdirs();
            ZipUtils.unzip(file, youtubeDLDir);
        } catch (Exception e) {
            //if something went wrong restore default version
            FileUtils.deleteQuietly(youtubeDLDir);
            YoutubeDL.getInstance().initYoutubeDL(appContext, youtubeDLDir);
            throw new YoutubeDLException(e);
        } finally {
            file.delete();
        }

        updateSharedPrefs(appContext, getTag(json));
        return UpdateStatus.DONE;
    }

    private static void updateSharedPrefs(Context appContext, String tag) {
        SharedPrefsHelper.update(appContext, Constants.YOUTUBE_DL_VERSION, tag);
    }

    private static JsonNode checkForUpdate(Context appContext) throws IOException {
        URL url = new URL(releasesUrl);
        JsonNode json = YoutubeDL.objectMapper.readTree(url);
        String newVersion = getTag(json);
        String oldVersion = SharedPrefsHelper.get(appContext, Constants.YOUTUBE_DL_VERSION);
        if(newVersion.equals(oldVersion)){
            return null;
        }
        return json;
    }

    private static String getTag(JsonNode json){
        return json.get("tag_name").asText();
    }

    @NonNull
    private static String getDownloadUrl(@NonNull JsonNode json) throws IOException, YoutubeDLException {
        ArrayNode assets = (ArrayNode) json.get("assets");
        String downloadUrl = "";
        for (JsonNode asset : assets) {
            if (Constants.youtubeDLZipFile.equals(asset.get("name").asText())) {
                downloadUrl = asset.get("browser_download_url").asText();
                break;
            }
        }
        if (downloadUrl.isEmpty()) throw new YoutubeDLException("unable to get download url");
        return downloadUrl;
    }

    @NonNull
    private static File download(Context appContext, String url) throws IOException {
        URL downloadUrl = new URL(url);
        File file = File.createTempFile(Constants.youtubeDLZipFile.replace(".zip", ""), "zip", appContext.getCacheDir());
        FileUtils.copyURLToFile(downloadUrl, file, 5000, 10000);
        return file;
    }

    @NonNull
    private static File getYoutubeDLDir(Context appContext) {
        File baseDir = new File(appContext.getNoBackupFilesDir(), Constants.baseName);
        return new File(baseDir, Constants.youtubeDLDirName);
    }

    @Nullable
    static String version(Context appContext) {
        return SharedPrefsHelper.get(appContext, Constants.YOUTUBE_DL_VERSION);
    }

}
