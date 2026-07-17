package com.hippo.ehviewer.translation;

import org.json.JSONObject;

public class TranslationMeta {
    public int page;
    public String providerId;
    public String providerName;
    public String imageModel;
    public String chatModel;
    public String mode;
    public long timestamp;
    public String prompt;

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("page", page);
            o.put("providerId", providerId);
            o.put("providerName", providerName);
            o.put("imageModel", imageModel);
            o.put("chatModel", chatModel);
            o.put("mode", mode);
            o.put("timestamp", timestamp);
            o.put("prompt", prompt);
        } catch (Exception ignored) {}
        return o;
    }

    public static TranslationMeta fromJson(JSONObject o) {
        TranslationMeta m = new TranslationMeta();
        m.page = o.optInt("page", 0);
        m.providerId = o.optString("providerId", "");
        m.providerName = o.optString("providerName", "");
        m.imageModel = o.optString("imageModel", "");
        m.chatModel = o.optString("chatModel", "");
        m.mode = o.optString("mode", "");
        m.timestamp = o.optLong("timestamp", 0);
        m.prompt = o.optString("prompt", "");
        return m;
    }
}
