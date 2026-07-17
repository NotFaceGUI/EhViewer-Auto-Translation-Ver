package com.hippo.ehviewer.translation.provider;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ModelFetcher {
    private static final String TAG = "ModelFetcher";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface Callback {
        void onResult(List<ModelInfo> models, Exception e);
    }

    public static void fetchModels(Provider p, Callback cb) {
        if (p == null) { if (cb != null) cb.onResult(null, new IllegalArgumentException("provider null")); return; }
        if (p.protocol == Provider.PROTOCOL_GEMINI) {
            fetchGemini(p, cb);
        } else {
            fetchOpenAICompat(p, cb);
        }
    }

    private static Request.Builder authedBuilder(Provider p, String url) {
        Request.Builder b = new Request.Builder().url(url);
        String key = p.apiKey == null ? "" : p.apiKey.trim();
        if (Provider.AUTH_BEARER == p.authScheme) {
            b.addHeader(p.authHeader, "Bearer " + key);
        } else {
            b.addHeader(p.authHeader, key);
        }
        return b;
    }

    private static void fetchOpenAICompat(Provider p, Callback cb) {
        new Thread(() -> {
            try {
                String base = normalizeBaseUrl(p.baseUrl);
                String url = base + "/models";
                Request req = authedBuilder(p, url).get().build();
                Response resp = CLIENT.newCall(req).execute();
                String s = resp.body() != null ? resp.body().string() : "{}";
                resp.close();
                JSONObject json = new JSONObject(s);
                JSONArray data = json.optJSONArray("data");
                if (data == null) data = json.optJSONArray("models");
                List<ModelInfo> list = new ArrayList<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject o = data.optJSONObject(i);
                        if (o == null) continue;
                        String id = o.optString("id", null);
                        if (id == null) id = o.optString("name", null);
                        if (id == null) continue;
                        String name = o.optString("name", id);
                        ModelInfo m = new ModelInfo(id, name, p.id);
                        m.supportsImage = ModelInfo.looksLikeImageModel(id);
                        m.supportsText = !m.supportsImage || id.toLowerCase().contains("gpt") || id.toLowerCase().contains("chat");
                        list.add(m);
                    }
                }
                if (list.isEmpty()) {
                    Request req2 = authedBuilder(p, stripVersion(base) + "/v1/models").get().build();
                    Response resp2 = CLIENT.newCall(req2).execute();
                    String s2 = resp2.body() != null ? resp2.body().string() : "{}";
                    resp2.close();
                    JSONObject json2 = new JSONObject(s2);
                    JSONArray data2 = json2.optJSONArray("data");
                    if (data2 == null) data2 = json2.optJSONArray("models");
                    if (data2 != null) {
                        for (int i = 0; i < data2.length(); i++) {
                            JSONObject o = data2.optJSONObject(i);
                            if (o == null) continue;
                            String id = o.optString("id", null);
                            if (id == null) continue;
                            ModelInfo m = new ModelInfo(id, o.optString("name", id), p.id);
                            m.supportsImage = ModelInfo.looksLikeImageModel(id);
                            list.add(m);
                        }
                    }
                }
                if (cb != null) cb.onResult(list, null);
            } catch (Exception e) {
                Log.e(TAG, "fetchOpenAICompat failed", e);
                if (cb != null) cb.onResult(null, e);
            }
        }).start();
    }

    private static void fetchGemini(Provider p, Callback cb) {
        new Thread(() -> {
            try {
                String base = normalizeBaseUrl(p.baseUrl);
                if (!base.contains("/v1beta")) {
                    base = base + "/v1beta";
                }
                String url = base + "/models";
                Request req = authedBuilder(p, url).get().build();
                Response resp = CLIENT.newCall(req).execute();
                String s = resp.body() != null ? resp.body().string() : "{}";
                resp.close();
                JSONObject json = new JSONObject(s);
                JSONArray models = json.optJSONArray("models");
                List<ModelInfo> list = new ArrayList<>();
                if (models != null) {
                    for (int i = 0; i < models.length(); i++) {
                        JSONObject o = models.optJSONObject(i);
                        if (o == null) continue;
                        String name = o.optString("name", null);
                        if (name == null) continue;
                        if (name.startsWith("models/")) name = name.substring(7);
                        JSONArray methods = o.optJSONArray("supportedGenerationMethods");
                        boolean canGenerate = false;
                        if (methods != null) {
                            for (int j = 0; j < methods.length(); j++) {
                                if ("generateContent".equals(methods.optString(j, ""))) { canGenerate = true; break; }
                            }
                        }
                        if (!canGenerate) continue;
                        ModelInfo m = new ModelInfo(name, o.optString("displayName", name), p.id);
                        m.supportsImage = ModelInfo.looksLikeImageModel(name) || name.toLowerCase().contains("image");
                        m.supportsText = true;
                        list.add(m);
                    }
                }
                if (cb != null) cb.onResult(list, null);
            } catch (Exception e) {
                Log.e(TAG, "fetchGemini failed", e);
                if (cb != null) cb.onResult(null, e);
            }
        }).start();
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null) return "";
        String u = url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    private static String stripVersion(String base) {
        if (base.endsWith("/v1")) return base.substring(0, base.length() - 3);
        if (base.endsWith("/v1beta")) return base.substring(0, base.length() - 6);
        return base;
    }
}
