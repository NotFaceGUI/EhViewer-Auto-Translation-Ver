package com.hippo.ehviewer.translation.provider;

public class ModelInfo {
    public String id;
    public String name;
    public String providerId;
    public boolean supportsImage = false;
    public boolean supportsText = false;

    public ModelInfo() {}

    public ModelInfo(String id, String name, String providerId) {
        this.id = id;
        this.name = name;
        this.providerId = providerId;
    }

    public static boolean looksLikeImageModel(String id) {
        if (id == null) return false;
        String lower = id.toLowerCase();
        return lower.contains("image") || lower.contains("imagine")
                || lower.contains("gpt-image") || lower.contains("dall-e")
                || lower.contains("imagen") || lower.contains("seedream");
    }
}
