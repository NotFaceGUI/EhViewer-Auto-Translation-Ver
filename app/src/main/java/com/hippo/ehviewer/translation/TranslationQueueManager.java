package com.hippo.ehviewer.translation;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.DownloadInfo;
import com.hippo.ehviewer.spider.SpiderDen;
import com.hippo.unifile.UniFile;

import java.io.File;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.nio.charset.StandardCharsets;

public class TranslationQueueManager {
    private static final TranslationQueueManager INSTANCE = new TranslationQueueManager();
    private final List<TranslationTaskInfo> list = new ArrayList<>();
    public interface Listener { void onChanged(); }
    private final List<Listener> listeners = new ArrayList<>();
    private String currentJobId;
    private final TranslationQueuePoller poller = new TranslationQueuePoller();
    private final File storageFile = new File(EhApplication.getInstance().getFilesDir(), "translation_queue.json");

    public static TranslationQueueManager getInstance() { return INSTANCE; }
    public TranslationQueueManager() { readQueueFromDisk(); }
    public void startPolling() { poller.start(); }

    public synchronized boolean enqueue(DownloadInfo info) {
        for (TranslationTaskInfo e : list) {
            if (e.gid == info.gid && e.state != TranslationTaskInfo.State.Canceled && e.state != TranslationTaskInfo.State.Completed) {
                return false;
            }
        }
        TranslationTaskInfo t = new TranslationTaskInfo();
        t.gid = info.gid;
        t.token = info.token;
        t.title = info.title;
        t.thumb = info.thumb;
        t.uploader = info.uploader;
        t.rating = info.rating;
        t.progress = 0;
        t.state = TranslationTaskInfo.State.Waiting;
        t.jobId = generateJobId();
        t.singlePage = false;
        t.pageIndex = -1;
        t.rangeStart = 0;
        t.rangeEnd = 0;
        list.add(0, t);
        notifyChanged();
        if (currentJobId == null) {
            // Try start immediately
            startNextSafely(t);
        }
        return true;
    }

    public synchronized boolean enqueueRange(DownloadInfo info, int startPage, int endPage) {
        int s = Math.max(1, startPage);
        int epage = Math.max(s, endPage);
        for (TranslationTaskInfo e : list) {
            if (e.gid == info.gid && !e.singlePage && e.rangeStart == s && e.rangeEnd == epage && e.state != TranslationTaskInfo.State.Canceled && e.state != TranslationTaskInfo.State.Completed) {
                return false;
            }
        }
        TranslationTaskInfo t = new TranslationTaskInfo();
        t.gid = info.gid;
        t.token = info.token;
        t.title = info.title + " [" + s + "-" + epage + "]";
        t.thumb = info.thumb;
        t.uploader = info.uploader;
        t.rating = info.rating;
        t.progress = 0;
        t.state = TranslationTaskInfo.State.Waiting;
        t.jobId = generateJobId();
        t.singlePage = false;
        t.pageIndex = -1;
        t.rangeStart = s;
        t.rangeEnd = epage;
        list.add(0, t);
        notifyChanged();
        if (currentJobId == null) {
            startNextSafely(t);
        }
        return true;
    }

    public synchronized boolean addUploadedJob(DownloadInfo info, String jobId, Integer startPage, Integer endPage) {
        int s = startPage != null ? Math.max(1, startPage) : 0;
        int epage = endPage != null ? Math.max(s, endPage) : 0;
        for (TranslationTaskInfo e : list) {
            boolean sameRange = (!e.singlePage) && (e.rangeStart == s) && (e.rangeEnd == epage);
            if (e.gid == info.gid && sameRange && e.state != TranslationTaskInfo.State.Canceled && e.state != TranslationTaskInfo.State.Completed) {
                return false;
            }
        }
        TranslationTaskInfo t = new TranslationTaskInfo();
        t.gid = info.gid;
        t.token = info.token;
        t.title = info.title + ((s > 0 && epage >= s) ? (" [" + s + "-" + epage + "]") : "");
        t.thumb = info.thumb;
        t.uploader = info.uploader;
        t.rating = info.rating;
        t.progress = 0;
        t.state = TranslationTaskInfo.State.Translating;
        t.jobId = jobId;
        t.singlePage = false;
        t.pageIndex = -1;
        t.rangeStart = s;
        t.rangeEnd = epage;
        list.add(0, t);
        currentJobId = jobId;
        notifyChanged();
        poller.start();
        return true;
    }

    public static File buildZipFor(DownloadInfo info, Integer startPage, Integer endPage) throws Exception {
        GalleryInfo gi = new GalleryInfo();
        gi.gid = info.gid;
        gi.title = info.title;
        UniFile dir = SpiderDen.getGalleryDownloadDir(gi);
        if (dir == null || !dir.exists()) return null;
        File tmpZip = File.createTempFile("submit_" + info.gid + "_", ".zip");
        boolean useRange = (startPage != null && endPage != null && startPage > 0 && endPage >= startPage);
        if (useRange) {
            compressRangeToZip(dir, tmpZip, startPage, endPage);
        } else {
            compressDirToZip(dir, tmpZip);
        }
        return tmpZip;
    }

    public synchronized boolean enqueueForce(DownloadInfo info) {
        TranslationTaskInfo ex = find(info.gid);
        if (ex != null) {
            ex.state = TranslationTaskInfo.State.Canceled;
            notifyChanged();
        }
        TranslationTaskInfo t = new TranslationTaskInfo();
        t.gid = info.gid;
        t.token = info.token;
        t.title = info.title;
        t.thumb = info.thumb;
        t.uploader = info.uploader;
        t.rating = info.rating;
        t.progress = 0;
        t.state = TranslationTaskInfo.State.Waiting;
        t.jobId = generateJobId();
        t.singlePage = false;
        t.pageIndex = -1;
        list.add(0, t);
        notifyChanged();
        startNextSafely(t);
        return true;
    }

    public synchronized boolean enqueueSinglePage(DownloadInfo info, int pageIndex) {
        for (TranslationTaskInfo e : list) {
            if (e.gid == info.gid && e.singlePage && e.pageIndex == pageIndex && e.state != TranslationTaskInfo.State.Canceled && e.state != TranslationTaskInfo.State.Completed) {
                return false;
            }
        }
        TranslationTaskInfo t = new TranslationTaskInfo();
        t.gid = info.gid;
        t.token = info.token;
        t.title = info.title;
        t.thumb = info.thumb;
        t.uploader = info.uploader;
        t.rating = info.rating;
        t.progress = 0;
        t.state = TranslationTaskInfo.State.Waiting;
        t.jobId = generateJobId();
        t.singlePage = true;
        t.pageIndex = pageIndex;
        t.sourcePath = null;
        list.add(0, t);
        notifyChanged();
        if (currentJobId == null) {
            startSinglePageSafely(t);
        }
        return true;
    }

    public synchronized boolean enqueueSinglePageWithPath(DownloadInfo info, int pageIndex, String sourcePath) {
        for (TranslationTaskInfo e : list) {
            if (e.gid == info.gid && e.singlePage && e.pageIndex == pageIndex && e.state != TranslationTaskInfo.State.Canceled && e.state != TranslationTaskInfo.State.Completed) {
                return false;
            }
        }
        TranslationTaskInfo t = new TranslationTaskInfo();
        t.gid = info.gid;
        t.token = info.token;
        t.title = info.title;
        t.thumb = info.thumb;
        t.uploader = info.uploader;
        t.rating = info.rating;
        t.progress = 0;
        t.state = TranslationTaskInfo.State.Waiting;
        t.jobId = generateJobId();
        t.singlePage = true;
        t.pageIndex = pageIndex;
        t.sourcePath = sourcePath;
        list.add(0, t);
        notifyChanged();
        if (currentJobId == null) {
            startSinglePageSafely(t);
        }
        poller.start();
        return true;
    }

    public synchronized boolean enqueueSinglePageForce(DownloadInfo info, int pageIndex, String sourcePath) {
        for (int i = list.size() - 1; i >= 0; i--) {
            TranslationTaskInfo e = list.get(i);
            if (e.gid == info.gid && e.singlePage && e.pageIndex == pageIndex) {
                e.state = TranslationTaskInfo.State.Canceled;
                notifyChanged();
                break;
            }
        }
        TranslationTaskInfo t = new TranslationTaskInfo();
        t.gid = info.gid;
        t.token = info.token;
        t.title = info.title;
        t.thumb = info.thumb;
        t.uploader = info.uploader;
        t.rating = info.rating;
        t.progress = 0;
        t.state = TranslationTaskInfo.State.Waiting;
        t.jobId = generateJobId();
        t.singlePage = true;
        t.pageIndex = pageIndex;
        t.sourcePath = sourcePath;
        list.add(0, t);
        notifyChanged();
        startSinglePageSafely(t);
        return true;
    }

    private String generateJobId() {
        int n = (int)(Math.random() * 900000) + 100000;
        return "T-" + n;
    }

    private void startNextSafely(TranslationTaskInfo t) {
        new Thread(() -> {
            try {
                GalleryInfo gi = new GalleryInfo();
                gi.gid = t.gid;
                gi.title = t.title;
                UniFile dir = SpiderDen.getGalleryDownloadDir(gi);
                if (dir != null && dir.exists()) {
                    UniFile translated = dir.findFile("translated");
                    if (translated == null) translated = dir.createDirectory("translated");
                    File tmpZip = File.createTempFile("submit_" + t.gid + "_", ".zip");
                    if (t.rangeStart > 0 && t.rangeEnd >= t.rangeStart) {
                        compressRangeToZip(dir, tmpZip, t.rangeStart, t.rangeEnd);
                    } else {
                        compressDirToZip(dir, tmpZip);
                    }
                    String jobId = TranslationApi.submitZip(tmpZip.getAbsolutePath());
                    try { tmpZip.delete(); } catch (Exception ignored) {}
                    if (jobId != null) {
                        t.jobId = jobId;
                        t.state = TranslationTaskInfo.State.Translating;
                        currentJobId = jobId;
                        notifyChanged();
                        poller.start();
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void startSinglePageSafely(TranslationTaskInfo t) {
        new Thread(() -> {
            try {
                GalleryInfo gi = new GalleryInfo();
                gi.gid = t.gid;
                gi.token = t.token;
                gi.title = t.title;
                UniFile dir = SpiderDen.getGalleryDownloadDir(gi);
                if (dir != null && dir.exists()) {
                    UniFile translated = dir.findFile("translated");
                    if (translated == null) translated = dir.createDirectory("translated");
                    String pathForSubmit = t.sourcePath;
                    if (pathForSubmit == null || pathForSubmit.isEmpty()) {
                        String pageNum = String.format(Locale.US, "%08d", t.pageIndex + 1);
                        UniFile[] files = dir.listFiles();
                        UniFile origin = null;
                        if (files != null) {
                            for (UniFile f : files) {
                                String n = f.getName();
                                if (n == null) continue;
                                if ("translated".equals(n)) continue;
                                if (f.isDirectory()) continue;
                                if (n.contains(pageNum)) { origin = f; break; }
                            }
                        }
                        if (origin != null) {
                            File tmp = null;
                            Uri uri = origin.getUri();
                            if (UniFile.isFileUri(uri)) {
                                pathForSubmit = new File(uri.getPath()).getAbsolutePath();
                            } else {
                                tmp = File.createTempFile("translate_single_" + gi.gid + "_" + (t.pageIndex+1) + "_", ".img");
                                InputStream is = origin.openInputStream();
                                FileOutputStream fos = new FileOutputStream(tmp);
                                try {
                                    byte[] buf = new byte[8192];
                                    int r;
                                    while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                                    fos.flush();
                                } finally {
                                    try { is.close(); } catch (Exception ignored) {}
                                    try { fos.close(); } catch (Exception ignored) {}
                                }
                                pathForSubmit = tmp.getAbsolutePath();
                            }
                        }
                    }
                    if (pathForSubmit != null && !pathForSubmit.isEmpty()) {
                        String jobId = TranslationApi.submitSingle(pathForSubmit);
                        if (jobId != null) {
                            t.jobId = jobId;
                            t.state = TranslationTaskInfo.State.Translating;
                            currentJobId = jobId;
                            notifyChanged();
                            poller.start();
                        }
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private static void compressDirToZip(UniFile srcDir, File outZip) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outZip));
        try {
            zipRec(zos, srcDir, "");
        } finally {
            try { zos.close(); } catch (Exception ignored) {}
        }
    }

    private static void compressRangeToZip(UniFile srcDir, File outZip, int startPage, int endPage) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outZip));
        try {
            UniFile[] children = srcDir.listFiles();
            if (children != null) {
                for (int i = startPage; i <= endPage; i++) {
                    String pageNum = String.format(java.util.Locale.US, "%08d", i);
                    for (UniFile c : children) {
                        String name = c.getName();
                        if (name == null) continue;
                        if ("translated".equals(name)) continue;
                        if (c.isDirectory()) continue;
                        if (name.contains(pageNum)) {
                            zos.putNextEntry(new java.util.zip.ZipEntry(name));
                            InputStream is = c.openInputStream();
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = is.read(buf)) != -1) zos.write(buf, 0, r);
                            try { is.close(); } catch (Exception ignored) {}
                            zos.closeEntry();
                        }
                    }
                }
            }
        } finally {
            try { zos.close(); } catch (Exception ignored) {}
        }
    }

    private static void zipRec(ZipOutputStream zos, UniFile file, String basePath) throws Exception {
        if (file.isDirectory()) {
            UniFile[] children = file.listFiles();
            if (children != null) {
                for (UniFile c : children) {
                    String next = basePath.isEmpty() ? c.getName() : (basePath + "/" + c.getName());
                    zipRec(zos, c, next);
                }
            }
        } else {
            String entryName = basePath.isEmpty() ? file.getName() : basePath;
            zos.putNextEntry(new ZipEntry(entryName));
            InputStream is = file.openInputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = is.read(buf)) != -1) zos.write(buf, 0, r);
            try { is.close(); } catch (Exception ignored) {}
            zos.closeEntry();
        }
    }

    public synchronized void pause(long gid) {
        TranslationTaskInfo t = find(gid);
        if (t != null && t.state == TranslationTaskInfo.State.Translating) t.state = TranslationTaskInfo.State.Paused;
        notifyChanged();
    }

    public synchronized void cancel(long gid) {
        TranslationTaskInfo t = find(gid);
        if (t != null) t.state = TranslationTaskInfo.State.Canceled;
        notifyChanged();
    }

    public synchronized void remove(long gid) {
        TranslationTaskInfo t = find(gid);
        if (t != null) list.remove(t);
        notifyChanged();
    }

    public synchronized void clearAll() {
        try {
            TranslationApi.cancelCurrentAsync((ok, e) -> {});
        } catch (Throwable ignored) {}
        list.clear();
        currentJobId = null;
        notifyChanged();
    }

    public synchronized TranslationTaskInfo get(long gid) { return find(gid); }

    public synchronized List<TranslationTaskInfo> getList() { return Collections.unmodifiableList(list); }

    public synchronized String getCurrentJobId() { return currentJobId; }

    public synchronized void addListener(Listener l) { listeners.add(l); }
    public synchronized void removeListener(Listener l) { listeners.remove(l); }
    private void notifyChanged() { for (Listener l : listeners) try { l.onChanged(); } catch (Exception ignored) {} writeQueueToDisk(); }

    public synchronized void updateCurrent(String jobId, int progress, String state) {
        updateCurrentDetailed(jobId, progress >= 0 ? progress : null, null, state);
    }

    public synchronized void updateCurrentDetailed(String jobId, Integer processPct, Integer translatePct, String state) {
        if ("done".equalsIgnoreCase(state)) {
            currentJobId = null;
        } else if (jobId != null) {
            currentJobId = jobId;
        }
        for (TranslationTaskInfo t : list) {
            boolean isTarget = jobId != null && jobId.equals(t.jobId);
            if (isTarget) {
                t.state = TranslationTaskInfo.State.Translating;
                if (processPct != null) t.processProgress = Math.max(0, Math.min(100, processPct));
                if (translatePct != null) t.translateProgress = Math.max(0, Math.min(100, translatePct));
                // 组合进度：两个进度相加后除以 2 的平均值；如果只有一个提供则显示该值
                if (processPct != null && translatePct != null) {
                    t.progress = (t.processProgress + t.translateProgress) / 2;
                } else if (translatePct != null) {
                    t.progress = t.translateProgress;
                } else if (processPct != null) {
                    t.progress = t.processProgress;
                }
                if ("done".equalsIgnoreCase(state)) {
                    t.state = TranslationTaskInfo.State.Completed;
                    t.processProgress = 100;
                    t.translateProgress = 100;
                    t.progress = 100;
                } else if ("cancelled".equalsIgnoreCase(state) || "canceled".equalsIgnoreCase(state)) {
                    t.state = TranslationTaskInfo.State.Canceled;
                } else if ("failed".equalsIgnoreCase(state) || "timeout".equalsIgnoreCase(state)) {
                    t.state = TranslationTaskInfo.State.Canceled;
                }
            } else if (t.state == TranslationTaskInfo.State.Translating) {
                t.state = TranslationTaskInfo.State.Waiting;
            }
        }
        notifyChanged();
        String jid = jobId != null ? jobId : currentJobId;
        if (jid != null) {
            for (TranslationTaskInfo t : list) {
                if (jid.equals(t.jobId) && t.state == TranslationTaskInfo.State.Completed && !t.downloaded) {
                    new Thread(() -> tryDownload(t)).start();
                }
            }
        }
    }

    private void tryDownload(TranslationTaskInfo t) {
        try {
            Log.d("TranslationQueue", "tryDownload jid=" + t.jobId + ", gid=" + t.gid);
            GalleryInfo gi = new GalleryInfo();
            gi.gid = t.gid;
            gi.title = t.title;
            UniFile dir = SpiderDen.getGalleryDownloadDir(gi);
            if (dir == null || !dir.exists()) return;
            UniFile translated = dir.findFile("translated");
            if (translated == null) translated = dir.createDirectory("translated");
            boolean ok = false;
            java.io.OutputStream os = null;
            UniFile out = null;
            t.downloading = true;
            t.downloadProgress = 0;
            notifyChanged();
            try {
                if (translated != null) {
                    out = translated.createFile("result_" + t.jobId + ".zip");
                    os = out.openOutputStream();
                    ok = TranslationApi.downloadResultToStream(t.jobId, os, new TranslationApi.DownloadCallback() {
                        @Override public void onStart(long total) {}
                        @Override public void onProgress(long written, long total) {
                            int pct = total > 0 ? (int)((written * 100) / total) : -1;
                            if (pct >= 0) { t.downloadProgress = Math.max(0, Math.min(100, pct)); notifyChanged(); }
                        }
                        @Override public void onDone() { t.downloadProgress = 100; notifyChanged(); }
                    });
                }
            } catch (Exception e) {
                Log.e("TranslationQueue", "download to UniFile failed, fallback", e);
            } finally {
                try { if (os != null) os.close(); } catch (Exception ignored) {}
            }
            if (!ok) {
                // fallback to external storage
                try {
                    File ext = new File(Environment.getExternalStorageDirectory(), "EhTranslated");
                    // noinspection ResultOfMethodCallIgnored
                    ext.mkdirs();
                    File f = new File(ext, "result_" + t.jobId + ".zip");
                    FileOutputStream fos = new FileOutputStream(f);
                    try {
                        ok = TranslationApi.downloadResultToStream(t.jobId, fos, new TranslationApi.DownloadCallback() {
                            @Override public void onStart(long total) {}
                            @Override public void onProgress(long written, long total) {
                                int pct = total > 0 ? (int)((written * 100) / total) : -1;
                                if (pct >= 0) { t.downloadProgress = Math.max(0, Math.min(100, pct)); notifyChanged(); }
                            }
                            @Override public void onDone() { t.downloadProgress = 100; notifyChanged(); }
                        });
                        Log.d("TranslationQueue", "fallback downloaded to " + f.getAbsolutePath());
                    } finally {
                        try { fos.close(); } catch (Exception ignored) {}
                    }
                } catch (Exception e2) {
                    Log.e("TranslationQueue", "fallback write failed", e2);
                }
            }
            if (ok) {
                // unzip into translated folder and delete zip
                try {
                    GalleryInfo gi2 = new GalleryInfo();
                    gi2.gid = t.gid;
                    gi2.title = t.title;
                    UniFile dir2 = SpiderDen.getGalleryDownloadDir(gi2);
                    if (dir2 != null && dir2.exists()) {
                        UniFile translated2 = dir2.findFile("translated");
                        if (translated2 == null) translated2 = dir2.createDirectory("translated");
                        UniFile zipFile = translated2 != null ? translated2.findFile("result_" + t.jobId + ".zip") : null;
                        if (zipFile != null) {
                            InputStream zis = zipFile.openInputStream();
                            ZipInputStream zin = new ZipInputStream(zis);
                            try {
                                ZipEntry entry;
                                byte[] buf = new byte[8192];
                                while ((entry = zin.getNextEntry()) != null) {
                                    if (entry.isDirectory()) { zin.closeEntry(); continue; }
                                    String name = entry.getName();
                                    UniFile existed = translated2.findFile(name);
                                    if (existed != null) { existed.delete(); }
                                    UniFile outEntry = translated2.createFile(name);
                                    java.io.OutputStream eos = outEntry.openOutputStream();
                                    try {
                                        int r;
                                        while ((r = zin.read(buf)) != -1) { eos.write(buf, 0, r); }
                                    } finally {
                                        try { eos.close(); } catch (Exception ignored) {}
                                    }
                                    zin.closeEntry();
                                }
                            } finally {
                                try { zin.close(); } catch (Exception ignored) {}
                                try { zis.close(); } catch (Exception ignored) {}
                            }
                            zipFile.delete();
                        }
                    }
                } catch (Exception e) {
                    Log.e("TranslationQueue", "unzip failed", e);
                }
                t.downloaded = true;
            }
            t.downloading = false;
            notifyChanged();
        } catch (Exception ignored) {}
    }

    private void writeQueueToDisk() {
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (TranslationTaskInfo t : list) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("gid", t.gid);
                o.put("token", t.token);
                o.put("title", t.title);
                o.put("thumb", t.thumb);
                o.put("uploader", t.uploader);
                o.put("rating", t.rating);
                o.put("jobId", t.jobId);
                o.put("state", t.state != null ? t.state.name() : null);
                o.put("singlePage", t.singlePage);
                o.put("pageIndex", t.pageIndex);
                o.put("rangeStart", t.rangeStart);
                o.put("rangeEnd", t.rangeEnd);
                o.put("downloaded", t.downloaded);
                o.put("processProgress", t.processProgress);
                o.put("translateProgress", t.translateProgress);
                o.put("downloading", t.downloading);
                o.put("downloadProgress", t.downloadProgress);
                arr.put(o);
            }
            org.json.JSONObject root = new org.json.JSONObject();
            root.put("currentJobId", currentJobId);
            root.put("list", arr);
            FileOutputStream fos = new FileOutputStream(storageFile);
            try {
                fos.write(root.toString().getBytes(StandardCharsets.UTF_8));
                fos.flush();
            } finally { try { fos.close(); } catch (Exception ignored) {} }
        } catch (Exception ignored) {}
    }

    private void readQueueFromDisk() {
        try {
            if (!storageFile.exists()) return;
            byte[] data;
            FileInputStream fis = new FileInputStream(storageFile);
            try { data = fis.readAllBytes(); } finally { try { fis.close(); } catch (Exception ignored) {} }
            String s = new String(data, StandardCharsets.UTF_8);
            org.json.JSONObject root = new org.json.JSONObject(s);
            currentJobId = root.optString("currentJobId", null);
            org.json.JSONArray arr = root.optJSONArray("list");
            list.clear();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                TranslationTaskInfo t = new TranslationTaskInfo();
                t.gid = o.optLong("gid", 0);
                t.token = o.optString("token", null);
                t.title = o.optString("title", null);
                t.thumb = o.optString("thumb", null);
                t.uploader = o.optString("uploader", null);
                t.rating = (float) o.optDouble("rating", 0.0);
                t.jobId = o.optString("jobId", null);
                String st = o.optString("state", null);
                try { t.state = st != null ? TranslationTaskInfo.State.valueOf(st) : null; } catch (Exception ignored) {}
                t.singlePage = o.optBoolean("singlePage", false);
                t.pageIndex = o.optInt("pageIndex", -1);
                t.rangeStart = o.optInt("rangeStart", 0);
                t.rangeEnd = o.optInt("rangeEnd", 0);
                t.downloaded = o.optBoolean("downloaded", false);
                t.processProgress = o.optInt("processProgress", 0);
                t.translateProgress = o.optInt("translateProgress", 0);
                t.downloading = o.optBoolean("downloading", false);
                t.downloadProgress = o.optInt("downloadProgress", 0);
                list.add(t);
            }
            }
        } catch (Exception ignored) {}
    }

    private TranslationTaskInfo find(long gid) {
        for (TranslationTaskInfo t : list) if (t.gid == gid) return t;
        return null;
    }
}
