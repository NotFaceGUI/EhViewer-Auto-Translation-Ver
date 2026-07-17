package com.hippo.ehviewer.translation;

import android.util.Log;

import com.hippo.ehviewer.EhApplication;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TranslationMetaStore {
    private static final String TAG = "TranslationMetaStore";

    private final long gid;
    private final Map<Integer, TranslationMeta> pages = new HashMap<>();
    private final File storageFile;

    public TranslationMetaStore(long gid) {
        this.gid = gid;
        this.storageFile = new File(EhApplication.getInstance().getFilesDir(), "translation_meta_" + gid + ".json");
        load();
    }

    public synchronized TranslationMeta get(int page) {
        return pages.get(page);
    }

    public synchronized boolean hasMeta(int page) {
        return pages.containsKey(page);
    }

    public synchronized void put(TranslationMeta meta) {
        if (meta == null) return;
        pages.put(meta.page, meta);
        save();
    }

    public synchronized void putAll(java.util.Collection<TranslationMeta> metas) {
        if (metas == null) return;
        for (TranslationMeta m : metas) {
            if (m != null) pages.put(m.page, m);
        }
        save();
    }

    private void load() {
        try {
            if (!storageFile.exists()) return;
            byte[] data = readAll(storageFile);
            if (data == null || data.length == 0) return;
            JSONObject root = new JSONObject(new String(data, StandardCharsets.UTF_8));
            JSONObject pagesObj = root.optJSONObject("pages");
            if (pagesObj != null) {
                java.util.Iterator<String> keys = pagesObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        int page = Integer.parseInt(key);
                        TranslationMeta m = TranslationMeta.fromJson(pagesObj.optJSONObject(key));
                        pages.put(page, m);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "load failed", e);
        }
    }

    private void save() {
        try {
            JSONObject root = new JSONObject();
            root.put("gid", gid);
            JSONObject pagesObj = new JSONObject();
            for (Map.Entry<Integer, TranslationMeta> e : pages.entrySet()) {
                pagesObj.put(String.valueOf(e.getKey()), e.getValue().toJson());
            }
            root.put("pages", pagesObj);
            writeAll(storageFile, root.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "save failed", e);
        }
    }

    private static byte[] readAll(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = fis.read(buf)) != -1) bos.write(buf, 0, r);
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static void writeAll(File f, byte[] data) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(data);
            fos.flush();
        }
    }
}
