package com.hippo.ehviewer.translation.provider;

public class Provider {
    public static final int PROTOCOL_OPENAI = 0;
    public static final int PROTOCOL_GEMINI = 1;

    public static final int AUTH_BEARER = 0;
    public static final int AUTH_RAW = 1;

    public String id;
    public String name;
    public int protocol = PROTOCOL_OPENAI;
    public String baseUrl;
    public String apiKey;
    public String authHeader = "Authorization";
    public int authScheme = AUTH_BEARER;
    public long maxImagePixels = 3840L * 2160L;
    public boolean isBuiltin = false;
    public boolean enabled = true;
    public String selectedModel;
    public String selectedChatModel;
    public String resolutionLevel = "2k";

    public Provider() {}

    public Provider(String id, String name, int protocol, String baseUrl, boolean isBuiltin) {
        this.id = id;
        this.name = name;
        this.protocol = protocol;
        this.baseUrl = baseUrl;
        this.isBuiltin = isBuiltin;
        if (protocol == PROTOCOL_GEMINI) {
            this.authHeader = "x-goog-api-key";
            this.authScheme = AUTH_RAW;
        } else {
            this.authHeader = "Authorization";
            this.authScheme = AUTH_BEARER;
        }
    }

    public String protocolName() {
        return protocol == PROTOCOL_GEMINI ? "gemini" : "openai";
    }

    public long getMaxImagePixels() {
        if (maxImagePixels > 0) return maxImagePixels;
        return resolutionToPixels(resolutionLevel);
    }

    public static long resolutionToPixels(String level) {
        if ("1k".equals(level)) return 1024L * 1024L;
        if ("2k".equals(level)) return 2048L * 2048L;
        if ("4k".equals(level)) return 4096L * 4096L;
        return 2048L * 2048L;
    }

    public static int parseProtocol(String s) {
        if ("gemini".equalsIgnoreCase(s)) return PROTOCOL_GEMINI;
        return PROTOCOL_OPENAI;
    }
}
