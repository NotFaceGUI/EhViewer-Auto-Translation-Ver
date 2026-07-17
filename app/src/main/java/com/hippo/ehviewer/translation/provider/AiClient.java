package com.hippo.ehviewer.translation.provider;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiClient {
    private static final String TAG = "AiClient";
    private static final MediaType MT_JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build();

    public interface GenerateCallback {
        void onResult(byte[] imageBytes, Exception e);
    }

    public interface ImageTextCallback {
        void onResult(byte[] imageBytes, String responseText, Exception e);
    }

    public interface TextCallback {
        void onResult(String text, Exception e);
    }

    public static void generateImage(Provider p, String model, String prompt, List<File> images, GenerateCallback cb) {
        if (p == null) { if (cb != null) cb.onResult(null, new IllegalArgumentException("provider null")); return; }
        if (model == null || model.isEmpty()) {
            if (p.selectedModel != null) model = p.selectedModel;
            else { if (cb != null) cb.onResult(null, new IllegalArgumentException("model empty")); return; }
        }
        if (p.protocol == Provider.PROTOCOL_GEMINI) {
            generateImageGemini(p, model, prompt, images, (png, text, e) -> {
                if (cb != null) cb.onResult(png, e);
            });
        } else {
            generateImageOpenAI(p, model, prompt, images, cb);
        }
    }

    public static void generateImageWithText(Provider p, String model, String prompt, List<File> images, ImageTextCallback cb) {
        if (p == null) { if (cb != null) cb.onResult(null, null, new IllegalArgumentException("provider null")); return; }
        if (model == null || model.isEmpty()) {
            if (p.selectedModel != null) model = p.selectedModel;
            else { if (cb != null) cb.onResult(null, null, new IllegalArgumentException("model empty")); return; }
        }
        if (p.protocol == Provider.PROTOCOL_GEMINI) {
            generateImageGemini(p, model, prompt, images, cb);
        } else {
            generateImageOpenAI(p, model, prompt, images, (png, e) -> {
                if (cb != null) cb.onResult(png, null, e);
            });
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

    private static void generateImageGemini(Provider p, String model, String prompt, List<File> images, ImageTextCallback cb) {
        try {
            String base = normalizeBaseUrl(p.baseUrl);
            if (!base.contains("/v1beta")) base = base + "/v1beta";
            String url = base + "/models/" + model + ":generateContent";

            JSONArray parts = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt == null ? "" : prompt);
            parts.put(textPart);
            if (images != null) {
                for (File f : images) {
                    if (f == null) continue;
                    JSONObject inline = new JSONObject();
                    inline.put("mime_type", guessMimeType(f.getName()));
                    inline.put("data", readBase64(f));
                    JSONObject inlinePart = new JSONObject();
                    inlinePart.put("inline_data", inline);
                    parts.put(inlinePart);
                }
            }

            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", parts);
            JSONArray contents = new JSONArray();
            contents.put(content);
            JSONObject body = new JSONObject();
            body.put("contents", contents);

            RequestBody reqBody = RequestBody.create(MT_JSON, body.toString().getBytes(StandardCharsets.UTF_8));
            Request req = authedBuilder(p, url).post(reqBody).build();
            Log.i(TAG, "Gemini generateImage -> " + url + " model=" + model + " imgs=" + (images != null ? images.size() : 0));
            CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    Log.e(TAG, "Gemini generateImage fail", e);
                    if (cb != null) cb.onResult(null, null, e);
                }
                @Override public void onResponse(okhttp3.Call call, Response response) {
                    try {
                        String s = response.body() != null ? response.body().string() : "{}";
                        byte[] png = extractGeminiImage(s);
                        String text = extractGeminiText(s);
                        Log.i(TAG, "Gemini generateImage resp code=" + response.code() + " imgLen=" + (png != null ? png.length : 0) + " textLen=" + (text != null ? text.length() : 0));
                        if (png != null) { if (cb != null) cb.onResult(png, text, null); }
                        else { if (cb != null) cb.onResult(null, text, new RuntimeException("no_image:" + response.code())); }
                    } catch (Exception ex) {
                        if (cb != null) cb.onResult(null, null, ex);
                    } finally {
                        try { response.close(); } catch (Exception ignored) {}
                    }
                }
            });
        } catch (Exception e) {
            if (cb != null) cb.onResult(null, null, e);
        }
    }

    private static byte[] extractGeminiImage(String s) {
        try {
            JSONObject json = new JSONObject(s);
            JSONArray candidates = json.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject c0 = candidates.optJSONObject(0);
                JSONObject cont = c0 != null ? c0.optJSONObject("content") : null;
                JSONArray rparts = cont != null ? cont.optJSONArray("parts") : null;
                if (rparts != null) {
                    for (int i = 0; i < rparts.length(); i++) {
                        JSONObject part = rparts.optJSONObject(i);
                        JSONObject inline = part != null ? part.optJSONObject("inlineData") : null;
                        if (inline == null) inline = part != null ? part.optJSONObject("inline_data") : null;
                        if (inline != null) {
                            String data = inline.optString("data", null);
                            if (data != null) return Base64.decode(data, Base64.DEFAULT);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "extractGeminiImage", e);
        }
        return null;
    }

    private static void generateImageOpenAI(Provider p, String model, String prompt, List<File> images, GenerateCallback cb) {
        try {
            String base = normalizeBaseUrl(p.baseUrl);
            boolean hasImages = images != null && !images.isEmpty();
            String url = hasImages ? base + "/images/edits" : base + "/images/generations";

            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("prompt", prompt == null ? "" : prompt);
            body.put("n", 1);
            body.put("response_format", "b64_json");
            if (p.resolutionLevel != null && !p.resolutionLevel.isEmpty()) {
                body.put("resolution", p.resolutionLevel);
            }

            if (hasImages) {
                if (images.size() == 1) {
                    JSONObject img = new JSONObject();
                    img.put("type", "image_url");
                    img.put("url", "data:" + guessMimeType(images.get(0).getName()) + ";base64," + readBase64(images.get(0)));
                    body.put("image", img);
                } else {
                    JSONArray imageArr = new JSONArray();
                    for (File f : images) {
                        if (f == null) continue;
                        JSONObject img = new JSONObject();
                        img.put("type", "image_url");
                        img.put("url", "data:" + guessMimeType(f.getName()) + ";base64," + readBase64(f));
                        imageArr.put(img);
                    }
                    body.put("image", imageArr);
                }
            }

            RequestBody reqBody = RequestBody.create(MT_JSON, body.toString().getBytes(StandardCharsets.UTF_8));
            Request req = authedBuilder(p, url).post(reqBody).build();
            Log.i(TAG, "OpenAI edits -> " + url + " model=" + model + " hasImages=" + hasImages + " resolution=" + p.resolutionLevel);
            CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    if (cb != null) cb.onResult(null, e);
                }
                @Override public void onResponse(okhttp3.Call call, Response response) {
                    try {
                        String s = response.body() != null ? response.body().string() : "{}";
                        byte[] png = extractOpenAIImage(s);
                        Log.i(TAG, "OpenAI edits resp code=" + response.code() + " imgLen=" + (png != null ? png.length : 0));
                        if (png != null) { if (cb != null) cb.onResult(png, null); }
                        else { if (cb != null) cb.onResult(null, new RuntimeException("no_image:" + response.code() + ":" + snippet(s))); }
                    } catch (Exception ex) {
                        if (cb != null) cb.onResult(null, ex);
                    } finally {
                        try { response.close(); } catch (Exception ignored) {}
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "openai generate ex", e);
            if (cb != null) cb.onResult(null, e);
        }
    }

    private static byte[] extractOpenAIImage(String s) {
        try {
            JSONObject json = new JSONObject(s);
            JSONArray data = json.optJSONArray("data");
            if (data != null && data.length() > 0) {
                JSONObject d0 = data.optJSONObject(0);
                if (d0 != null) {
                    String b64 = d0.optString("b64_json", null);
                    if (b64 == null) b64 = d0.optString("base64", null);
                    if (b64 != null) return Base64.decode(b64, Base64.DEFAULT);
                    JSONObject urlObj = d0.optJSONObject("url");
                    if (urlObj != null) return null;
                    String url = d0.optString("url", null);
                    if (url != null && url.startsWith("data:")) {
                        int idx = url.indexOf(",");
                        if (idx > 0) return Base64.decode(url.substring(idx + 1), Base64.DEFAULT);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "extractOpenAIImage", e);
        }
        return null;
    }

    public static void generateText(Provider p, String model, String prompt, TextCallback cb) {
        if (p == null) { if (cb != null) cb.onResult(null, new IllegalArgumentException("provider null")); return; }
        if (model == null || model.isEmpty()) { if (cb != null) cb.onResult(null, new IllegalArgumentException("model empty")); return; }
        if (p.protocol == Provider.PROTOCOL_GEMINI) {
            generateTextGemini(p, model, prompt, cb);
        } else {
            generateTextOpenAI(p, model, prompt, cb);
        }
    }

    private static void generateTextGemini(Provider p, String model, String prompt, TextCallback cb) {
        try {
            String base = normalizeBaseUrl(p.baseUrl);
            if (!base.contains("/v1beta")) base = base + "/v1beta";
            String url = base + "/models/" + model + ":generateContent";
            JSONArray parts = new JSONArray();
            JSONObject text = new JSONObject();
            text.put("text", prompt == null ? "" : prompt);
            parts.put(text);
            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", parts);
            JSONArray contents = new JSONArray();
            contents.put(content);
            JSONObject body = new JSONObject();
            body.put("contents", contents);
            RequestBody reqBody = RequestBody.create(MT_JSON, body.toString().getBytes(StandardCharsets.UTF_8));
            Request req = authedBuilder(p, url).post(reqBody).build();
            CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    if (cb != null) cb.onResult(null, e);
                }
                @Override public void onResponse(okhttp3.Call call, Response response) {
                    try {
                        String s = response.body() != null ? response.body().string() : "{}";
                        String text = extractGeminiText(s);
                        if (cb != null) cb.onResult(text, text == null ? new RuntimeException("no_text:" + response.code()) : null);
                    } catch (Exception ex) {
                        if (cb != null) cb.onResult(null, ex);
                    } finally {
                        try { response.close(); } catch (Exception ignored) {}
                    }
                }
            });
        } catch (Exception e) {
            if (cb != null) cb.onResult(null, e);
        }
    }

    private static String extractGeminiText(String s) {
        try {
            JSONObject json = new JSONObject(s);
            JSONArray candidates = json.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject c0 = candidates.optJSONObject(0);
                JSONObject cont = c0 != null ? c0.optJSONObject("content") : null;
                JSONArray rparts = cont != null ? cont.optJSONArray("parts") : null;
                if (rparts != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < rparts.length(); i++) {
                        JSONObject part = rparts.optJSONObject(i);
                        if (part != null) {
                            String t = part.optString("text", null);
                            if (t != null) sb.append(t);
                        }
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) { Log.e(TAG, "extractGeminiText", e); }
        return null;
    }

    private static void generateTextOpenAI(Provider p, String model, String prompt, TextCallback cb) {
        try {
            String base = normalizeBaseUrl(p.baseUrl);
            String url = base + "/chat/completions";
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt == null ? "" : prompt);
            JSONArray messages = new JSONArray();
            messages.put(userMsg);
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("messages", messages);
            RequestBody reqBody = RequestBody.create(MT_JSON, body.toString().getBytes(StandardCharsets.UTF_8));
            Request req = authedBuilder(p, url).post(reqBody).build();
            CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    if (cb != null) cb.onResult(null, e);
                }
                @Override public void onResponse(okhttp3.Call call, Response response) {
                    try {
                        String s = response.body() != null ? response.body().string() : "{}";
                        JSONObject json = new JSONObject(s);
                        JSONArray choices = json.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            JSONObject c0 = choices.optJSONObject(0);
                            JSONObject msg = c0 != null ? c0.optJSONObject("message") : null;
                            String t = msg != null ? msg.optString("content", null) : null;
                            if (cb != null) cb.onResult(t, t == null ? new RuntimeException("no_text") : null);
                            return;
                        }
                        if (cb != null) cb.onResult(null, new RuntimeException("no_text:" + response.code()));
                    } catch (Exception ex) {
                        if (cb != null) cb.onResult(null, ex);
                    } finally {
                        try { response.close(); } catch (Exception ignored) {}
                    }
                }
            });
        } catch (Exception e) {
            if (cb != null) cb.onResult(null, e);
        }
    }

    public static void analyzeImages(Provider p, String chatModel, List<File> images, String analysisPrompt, TextCallback cb) {
        if (p == null || chatModel == null) { if (cb != null) cb.onResult(null, new IllegalArgumentException("missing provider/model")); return; }
        if (p.protocol == Provider.PROTOCOL_GEMINI) {
            analyzeImagesGemini(p, chatModel, images, analysisPrompt, cb);
        } else {
            analyzeImagesOpenAI(p, chatModel, images, analysisPrompt, cb);
        }
    }

    private static void analyzeImagesGemini(Provider p, String model, List<File> images, String prompt, TextCallback cb) {
        try {
            String base = normalizeBaseUrl(p.baseUrl);
            if (!base.contains("/v1beta")) base = base + "/v1beta";
            String url = base + "/models/" + model + ":generateContent";
            JSONArray parts = new JSONArray();
            JSONObject tp = new JSONObject();
            tp.put("text", prompt == null ? "" : prompt);
            parts.put(tp);
            if (images != null) {
                for (File f : images) {
                    if (f == null) continue;
                    JSONObject inline = new JSONObject();
                    inline.put("mime_type", guessMimeType(f.getName()));
                    inline.put("data", readBase64(f));
                    JSONObject ip = new JSONObject();
                    ip.put("inline_data", inline);
                    parts.put(ip);
                }
            }
            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", parts);
            JSONArray contents = new JSONArray();
            contents.put(content);
            JSONObject body = new JSONObject();
            body.put("contents", contents);
            RequestBody rq = RequestBody.create(MT_JSON, body.toString().getBytes(StandardCharsets.UTF_8));
            Request req = authedBuilder(p, url).post(rq).build();
            CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) { if (cb != null) cb.onResult(null, e); }
                @Override public void onResponse(okhttp3.Call call, Response response) {
                    try {
                        String s = response.body() != null ? response.body().string() : "{}";
                        String t = extractGeminiText(s);
                        if (cb != null) cb.onResult(t, null);
                    } catch (Exception ex) { if (cb != null) cb.onResult(null, ex); }
                    finally { try { response.close(); } catch (Exception ignored) {} }
                }
            });
        } catch (Exception e) { if (cb != null) cb.onResult(null, e); }
    }

    private static void analyzeImagesOpenAI(Provider p, String model, List<File> images, String prompt, TextCallback cb) {
        try {
            String base = normalizeBaseUrl(p.baseUrl);
            JSONArray content = new JSONArray();
            JSONObject tc = new JSONObject();
            tc.put("type", "text");
            tc.put("text", prompt == null ? "" : prompt);
            content.put(tc);
            if (images != null) {
                for (File f : images) {
                    if (f == null) continue;
                    JSONObject ic = new JSONObject();
                    ic.put("type", "image_url");
                    JSONObject iu = new JSONObject();
                    iu.put("url", "data:" + guessMimeType(f.getName()) + ";base64," + readBase64(f));
                    ic.put("image_url", iu);
                    content.put(ic);
                }
            }
            JSONObject um = new JSONObject();
            um.put("role", "user");
            um.put("content", content);
            JSONArray msgs = new JSONArray();
            msgs.put(um);
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("messages", msgs);
            body.put("max_tokens", 512);
            RequestBody rq = RequestBody.create(MT_JSON, body.toString().getBytes(StandardCharsets.UTF_8));
            Request req = authedBuilder(p, base + "/chat/completions").post(rq).build();
            CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) { if (cb != null) cb.onResult(null, e); }
                @Override public void onResponse(okhttp3.Call call, Response response) {
                    try {
                        String s = response.body() != null ? response.body().string() : "{}";
                        JSONObject json = new JSONObject(s);
                        JSONArray choices = json.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            String t = choices.optJSONObject(0).optJSONObject("message").optString("content", null);
                            if (cb != null) cb.onResult(t, null);
                            return;
                        }
                        if (cb != null) cb.onResult(null, new RuntimeException("no_text:" + response.code()));
                    } catch (Exception ex) { if (cb != null) cb.onResult(null, ex); }
                    finally { try { response.close(); } catch (Exception ignored) {} }
                }
            });
        } catch (Exception e) { if (cb != null) cb.onResult(null, e); }
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null) return "";
        String u = url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    private static String guessMimeType(String name) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".webp")) return "image/webp";
        return "image/png";
    }

    private static String readBase64(File f) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = fis.read(buf)) != -1) bos.write(buf, 0, r);
        }
        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
    }

    private static String snippet(String s) {
        return s == null ? "" : (s.length() > 256 ? s.substring(0, 256) + "..." : s);
    }
}
