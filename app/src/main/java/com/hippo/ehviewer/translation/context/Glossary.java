package com.hippo.ehviewer.translation.context;

import android.util.Log;

import com.hippo.ehviewer.EhApplication;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Glossary {
    private static final String TAG = "Glossary";

    public static class Entry {
        public String src;
        public String dst;
        public String note;

        public Entry() {}

        public Entry(String src, String dst) {
            this.src = src;
            this.dst = dst;
        }
    }

    private final long gid;
    private final List<Entry> entries = new ArrayList<>();
    private String lastSummary = "";

    public Glossary(long gid) {
        this.gid = gid;
        load();
    }

    public List<Entry> getEntries() { return entries; }
    public String getLastSummary() { return lastSummary; }
    public void setLastSummary(String s) { this.lastSummary = s == null ? "" : s; }

    public synchronized void addEntry(String src, String dst) {
        if (src == null || dst == null) return;
        String s = src.trim();
        String d = dst.trim();
        if (s.isEmpty() || d.isEmpty()) return;
        for (Entry e : entries) {
            if (e.src != null && e.src.equalsIgnoreCase(s)) {
                e.dst = d;
                return;
            }
        }
        Entry e = new Entry(s, d);
        entries.add(e);
    }

    public synchronized void addAll(List<Entry> newEntries) {
        if (newEntries == null) return;
        for (Entry e : newEntries) addEntry(e.src, e.dst);
    }

    public synchronized String buildContextText() {
        if (entries.isEmpty() && lastSummary.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        if (!entries.isEmpty()) {
            sb.append("【角色名/术语统一对照表 —— 翻译时严格使用以下译名，确保全文一致】\n");
            int limit = Math.min(entries.size(), 64);
            for (int i = 0; i < limit; i++) {
                Entry e = entries.get(i);
                sb.append("- ").append(e.src).append(" → ").append(e.dst);
                if (e.note != null && !e.note.isEmpty()) sb.append("（").append(e.note).append("）");
                sb.append("\n");
            }
            if (entries.size() > 64) sb.append("- ...（共").append(entries.size()).append("条）\n");
        }
        if (!lastSummary.isEmpty()) {
            sb.append("\n【前几页剧情梗概 —— 翻译时注意语境连贯】\n");
            sb.append(lastSummary).append("\n");
        }
        return sb.toString();
    }

    private File storageFile() {
        return new File(EhApplication.getInstance().getFilesDir(), "glossary_" + gid + ".json");
    }

    public synchronized void save() {
        try {
            JSONObject root = new JSONObject();
            JSONArray arr = new JSONArray();
            for (Entry e : entries) {
                JSONObject o = new JSONObject();
                o.put("src", e.src);
                o.put("dst", e.dst);
                o.put("note", e.note);
                arr.put(o);
            }
            root.put("entries", arr);
            root.put("lastSummary", lastSummary);
            writeAll(storageFile(), root.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "save failed", e);
        }
    }

    private void load() {
        try {
            File f = storageFile();
            if (!f.exists()) return;
            byte[] data = readAll(f);
            if (data == null || data.length == 0) return;
            JSONObject root = new JSONObject(new String(data, StandardCharsets.UTF_8));
            JSONArray arr = root.optJSONArray("entries");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    Entry e = new Entry();
                    e.src = o.optString("src", null);
                    e.dst = o.optString("dst", null);
                    e.note = o.optString("note", null);
                    if (e.src != null && e.dst != null) entries.add(e);
                }
            }
            lastSummary = root.optString("lastSummary", "");
        } catch (Exception e) {
            Log.e(TAG, "load failed", e);
        }
    }

    private static byte[] readAll(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
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
