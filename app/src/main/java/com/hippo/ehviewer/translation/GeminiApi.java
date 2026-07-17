package com.hippo.ehviewer.translation;

import android.util.Log;

import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.translation.provider.AiClient;
import com.hippo.ehviewer.translation.provider.Provider;
import com.hippo.ehviewer.translation.provider.ProviderStore;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class GeminiApi {
    private static final String TAG = "GeminiApi";

    public static String getModel() {
        Provider p = ProviderStore.getInstance().getDefaultImageProvider();
        if (p != null && p.selectedModel != null && !p.selectedModel.isEmpty()) return p.selectedModel;
        String sel = Settings.getString("gemini_model_select", "default");
        if ("custom".equals(sel)) {
            String cust = Settings.getString("gemini_model_custom", "");
            if (cust != null && cust.trim().length() > 0) return cust.trim();
        }
        return "gemini-3-pro-image-preview";
    }

    public static String getBaseUrl() {
        Provider p = ProviderStore.getInstance().getDefaultImageProvider();
        if (p != null && p.baseUrl != null && !p.baseUrl.isEmpty()) return p.baseUrl;
        return Settings.getString("gemini_base_url", "https://generativelanguage.googleapis.com");
    }

    public static String getCommonPrompt() {
        String lang = getTargetLang();
        String defaultPrompt =
                "你是一个专业的漫画翻译助手。请将图片中的所有文字翻译为" + lang + "，严格遵循以下规则：\n" +
                "1. 保留原图构图、画风、线条、颜色、阴影、网点和所有非文字内容，不做任何改动\n" +
                "2. 翻译所有文字：对话框、旁白、注释、拟声词、艺术字、标题、手写体、标志等全部文本\n" +
                "3. 拟声词和艺术字必须保持原风格（大小、颜色、倾斜度、变形、透视），用" + lang + "象声词或等效表达替换\n" +
                "4. 对话框内文字翻译为自然流畅的" + lang + "，符合角色性格和上下文语境\n" +
                "5. 译文长度无需与原文字数匹配，翻译本身就存在语言长度差异，适当调整字号适配即可，绝不能自行添加原文没有的句子或内容\n" +
                "6. 不要添加任何原图中没有的内容，不要添加解释或注释\n" +
                "7. 翻译后的文字放置在原文字位置，覆盖原文字，保持原文的书写方向（竖排、横排、换行）和排版方式\n" +
                "8. 输出图片分辨率与输入完全一致，不要缩放或裁剪\n" +
                "9. 如果原图没有文字，原样返回不做修改";
        String stored = Settings.getString("gemini_common_prompt", "");
        if (stored != null && stored.length() > 10) return stored;
        return defaultPrompt;
    }

    private static String getTargetLang() {
        return Settings.getString("target_language_setting", "简体中文");
    }

    public static String getApiKey() {
        Provider p = ProviderStore.getInstance().getDefaultImageProvider();
        if (p != null && p.apiKey != null && !p.apiKey.isEmpty()) return p.apiKey;
        return Settings.getString("gemini_api_key", "");
    }

    public interface GenerateCallback {
        void onResult(byte[] imagePng, Exception e);
    }

    public static void generateImageAsync(GenerateCallback cb) {
        generateImageAsync(getCommonPrompt(), cb);
    }

    public static void generateImageAsync(String prompt, GenerateCallback cb) {
        generateImageAsync(prompt, (List<File>) null, cb);
    }

    public static void generateImageAsync(String prompt, File image, GenerateCallback cb) {
        generateImageAsync(prompt, image != null ? Collections.singletonList(image) : null, cb);
    }

    public static void generateImageAsync(String prompt, List<File> images, GenerateCallback cb) {
        Provider p = ProviderStore.getInstance().getDefaultImageProvider();
        if (p == null) {
            if (cb != null) cb.onResult(null, new RuntimeException("no_provider_configured"));
            return;
        }
        String model = getModel();
        AiClient.generateImage(p, model, prompt, images, (png, e) -> {
            if (cb != null) cb.onResult(png, e);
        });
    }
}
