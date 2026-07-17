package com.hippo.ehviewer.translation;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import android.util.Log;
import com.hippo.ehviewer.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class TranslationApi {
    private static final String TAG = "TranslationApi";
    private static String buildBaseUrl() {
        String host = Settings.getString("translation_base_host", "127.0.0.1");
        int port = Settings.getIntFromStr("translation_base_port", 8000);
        if (host == null || host.trim().isEmpty()) host = "127.0.0.1";
        if (port <= 0) port = 8000;
        return "http://" + host + ":" + port;
    }
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public static boolean testConnection() {
        try {
            String url = buildBaseUrl() ;
            Request req = new Request.Builder().url(url).get().build();
            try (Response resp = CLIENT.newCall(req).execute()) {
                boolean ok = resp.isSuccessful();
                Log.d(TAG, "translation_test code=" + resp.code() + ", ok=" + ok);
                return ok;
            }
        } catch (Exception e) {
            Log.e(TAG, "translation_test error", e);
            return false;
        }
    }

    public interface ConnectionCallback {
        void onResult(boolean ok, int code, Exception e);
    }



    public static void testConnectionAsync(final ConnectionCallback cb) {
        String url = buildBaseUrl() ;
        Log.d(TAG, "translation_test_async url=" + url);
        Request req = new Request.Builder().url(url).get().build();
        CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                Log.e(TAG, "translation_test_async error", e);
                if (cb != null) cb.onResult(false, -1, e);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) {
                boolean ok = response.isSuccessful();
                int code = response.code();
                Log.d(TAG, "translation_test_async code=" + code + ", ok=" + ok);
                try { response.close(); } catch (Exception ignored) {}
                if (cb != null) cb.onResult(ok, code, null);
            }
        });
    }

    public static String submitZip(String zipPath) throws Exception {
        File zip = new File(zipPath);
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/zip"), zip);
        MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("zip", zip.getName(), fileBody)
                .addFormDataPart("output_format", "webp")
                .build();
        Request req = new Request.Builder().url(buildBaseUrl() + "/translate").post(body).build();
        try (Response resp = CLIENT.newCall(req).execute()) {
            String s = resp.body() != null ? resp.body().string() : "{}";
            JSONObject json = new JSONObject(s);
            return json.optString("job_id", null);
        }
    }

    public static String submitSingle(String imagePath) throws Exception {
        File img = new File(imagePath);
        RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), img);
        MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", img.getName(), fileBody)
                .addFormDataPart("output_format", "png")
                .build();
        Request req = new Request.Builder().url(buildBaseUrl() + "/translate").post(body).build();
        try (Response resp = CLIENT.newCall(req).execute()) {
            String s = resp.body() != null ? resp.body().string() : "{}";
            JSONObject json = new JSONObject(s);
            return json.optString("job_id", null);
        }
    }

    public static JSONObject getStatus(String jobId) throws Exception {
        String url = buildBaseUrl() + "/status" + (jobId != null ? ("?job_id=" + jobId) : "");
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = CLIENT.newCall(req).execute()) {
            String s = resp.body() != null ? resp.body().string() : "{}";
            return new JSONObject(s);
        }
    }

    public static JSONObject getCurrentJob() throws Exception {
        Request req = new Request.Builder().url(buildBaseUrl() + "/current_job").get().build();
        try (Response resp = CLIENT.newCall(req).execute()) {
            if (resp.code() == 404) return null;
            String s = resp.body() != null ? resp.body().string() : "{}";
            return new JSONObject(s);
        }
    }

    public interface CurrentJobCallback { void onResult(JSONObject job, Exception e); }
    public interface StatusCallback { void onResult(JSONObject status, Exception e); }

    public static void getCurrentJobAsync(final CurrentJobCallback cb) {
        String url = buildBaseUrl() + "/current_job";
        Log.d(TAG, "poll_current_job url=" + url);
        Request req = new Request.Builder().url(url).get().build();
        CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                Log.e(TAG, "poll_current_job error", e);
                if (cb != null) cb.onResult(null, e);
            }
            @Override public void onResponse(okhttp3.Call call, Response response) {
                try {
                    if (response.code() == 404) {
                        Log.d(TAG, "poll_current_job 404");
                        if (cb != null) cb.onResult(null, null);
                    } else {
                        String s = response.body() != null ? response.body().string() : "{}";
                        JSONObject json = new JSONObject(s);
                        Log.d(TAG, "poll_current_job ok code=" + response.code());
                        if (cb != null) cb.onResult(json, null);
                    }
                } catch (Exception ex) {
                    if (cb != null) cb.onResult(null, ex);
                } finally {
                    try { response.close(); } catch (Exception ignored) {}
                }
            }
        });
    }

    public static void getStatusAsync(String jobId, final StatusCallback cb) {
        String url = buildBaseUrl() + "/status" + (jobId != null ? ("?job_id=" + jobId) : "");
        Log.d(TAG, "poll_status url=" + url);
        Request req = new Request.Builder().url(url).get().build();
        CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                Log.e(TAG, "poll_status error", e);
                if (cb != null) cb.onResult(null, e);
            }
            @Override public void onResponse(okhttp3.Call call, Response response) {
                try {
                    String s = response.body() != null ? response.body().string() : "{}";
                    JSONObject json = new JSONObject(s);
                    Log.d(TAG, "poll_status ok code=" + response.code());
                    if (cb != null) cb.onResult(json, null);
                } catch (Exception ex) {
                    if (cb != null) cb.onResult(null, ex);
                } finally {
                    try { response.close(); } catch (Exception ignored) {}
                }
            }
        });
    }

    public static boolean cancelCurrent() throws Exception {
        Request req = new Request.Builder().url(buildBaseUrl() + "/cancel_current").post(RequestBody.create(MediaType.parse("application/octet-stream"), new byte[0])).build();
        try (Response resp = CLIENT.newCall(req).execute()) {
            return resp.isSuccessful();
        }
    }

    public static boolean downloadResult(String jobId, File outFile) throws Exception {
        String url = buildBaseUrl() + "/result?job_id=" + jobId;
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = CLIENT.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) return false;
            try (InputStream in = resp.body().byteStream(); FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                }
            }
            return true;
        }
    }

    public static boolean downloadResultToStream(String jobId, java.io.OutputStream os) throws Exception {
        String url = buildBaseUrl() + "/result?job_id=" + jobId;
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = CLIENT.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) return false;
            try (InputStream in = resp.body().byteStream()) {
                long cl = resp.body().contentLength();
                android.util.Log.d(TAG, "download_result contentLength=" + cl + ", url=" + url);
                byte[] buf = new byte[8192];
                int r;
                long written = 0;
                while ((r = in.read(buf)) != -1) {
                    os.write(buf, 0, r);
                    written += r;
                }
                try { os.flush(); } catch (Exception ignored) {}
                android.util.Log.d(TAG, "download_result written=" + written);
            }
            return true;
        }
    }

    public interface DownloadCallback {
        void onStart(long contentLength);
        void onProgress(long written, long total);
        void onDone();
    }

    public static boolean downloadResultToStream(String jobId, java.io.OutputStream os, DownloadCallback cb) throws Exception {
        String url = buildBaseUrl() + "/result?job_id=" + jobId;
        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = CLIENT.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) return false;
            try (InputStream in = resp.body().byteStream()) {
                long cl = resp.body().contentLength();
                if (cb != null) cb.onStart(cl);
                byte[] buf = new byte[8192];
                int r;
                long written = 0;
                while ((r = in.read(buf)) != -1) {
                    os.write(buf, 0, r);
                    written += r;
                    if (cb != null) cb.onProgress(written, cl);
                }
                try { os.flush(); } catch (Exception ignored) {}
                if (cb != null) cb.onDone();
            }
            return true;
        }
    }

    public interface CancelCallback { void onResult(boolean ok, Exception e); }

    public static void cancelCurrentAsync(final CancelCallback cb) {
        String url = buildBaseUrl() + "/cancel_current";
        Request req = new Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/octet-stream"), new byte[0]))
                .build();
        CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                if (cb != null) cb.onResult(false, e);
            }
            @Override public void onResponse(okhttp3.Call call, Response response) {
                boolean ok = response.isSuccessful();
                try { response.close(); } catch (Exception ignored) {}
                if (cb != null) cb.onResult(ok, null);
            }
        });
    }
}
