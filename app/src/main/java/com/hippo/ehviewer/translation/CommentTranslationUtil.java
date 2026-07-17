package com.hippo.ehviewer.translation;

import com.hippo.ehviewer.translation.provider.AiClient;
import com.hippo.ehviewer.translation.provider.Provider;
import com.hippo.ehviewer.translation.provider.ProviderStore;

/**
 * 评论/标题文本翻译工具。
 * 调用 OpenAI 兼容 API（通过已有的 AiClient.generateText）。
 * 翻译结果通过 Callback 异步返回。
 */
public class CommentTranslationUtil {

    public interface TranslateCallback {
        void onResult(String translatedText, Exception e);
    }

    /**
     * 获取用于文本翻译的 Provider。
     * 优先使用设置中指定的 provider，否则回退到第一个启用的 OpenAI provider。
     */
    public static Provider getTextProvider() {
        String pid = com.hippo.ehviewer.Settings.getString("text_translation_provider", "");
        ProviderStore store = ProviderStore.getInstance();
        if (!pid.isEmpty()) {
            Provider p = store.getProvider(pid);
            if (p != null && p.enabled && !isEmpty(p.apiKey)) return p;
        }
        for (Provider p : store.getProviders()) {
            if (p.enabled && p.protocol == Provider.PROTOCOL_OPENAI && !isEmpty(p.apiKey)) {
                return p;
            }
        }
        return null;
    }

    public static String getChatModel(Provider p) {
        if (p == null) return null;
        if (p.selectedChatModel != null && !p.selectedChatModel.trim().isEmpty()) {
            return p.selectedChatModel.trim();
        }
        return "gpt-4o-mini";
    }

    /**
     * 翻译 HTML 评论，原样保留所有标签和链接，只翻译文字。
     */
    public static void translatePreserveHtml(String htmlText, String targetLang, TranslateCallback cb) {
        Provider p = getTextProvider();
        if (p == null) {
            if (cb != null) cb.onResult(null, new IllegalStateException("No text translation provider configured"));
            return;
        }
        String model = getChatModel(p);
        if (htmlText == null || htmlText.trim().isEmpty()) {
            if (cb != null) cb.onResult("", null);
            return;
        }
        String prompt = "You are translating a user comment that may contain HTML tags. "
                + "Translate ALL visible text to " + targetLang + ". "
                + "Keep ALL HTML tags, links (<a href=\"...\">), images, and structure EXACTLY as-is. "
                + "Only change the text between/inside tags. "
                + "Output the complete HTML with translations applied. No explanations.\n\n"
                + htmlText;
        AiClient.generateText(p, model, prompt, (text, e) -> {
            if (cb != null) {
                if (e != null) cb.onResult(null, e);
                else cb.onResult(text != null ? text.trim() : "", null);
            }
        });
    }
    public static void translate(String htmlText, String targetLang, TranslateCallback cb) {
        translate(htmlText, targetLang,
                "Translate only the user comment text to " + targetLang
                + ". Do NOT translate or include HTML tags, scores, "
                + "timestamps, usernames, or any markup. "
                + "Only output the translated comment, nothing else.",
                cb);
    }

    /**
     * 翻译文本（自定义系统提示词）。
     */
    public static void translate(String htmlText, String targetLang, String systemPrompt, TranslateCallback cb) {
        Provider p = getTextProvider();
        if (p == null) {
            if (cb != null) cb.onResult(null, new IllegalStateException("No text translation provider configured"));
            return;
        }
        String model = getChatModel(p);
        String plain = stripHtml(htmlText);
        if (plain.isEmpty()) {
            if (cb != null) cb.onResult("", null);
            return;
        }
        String prompt = systemPrompt + "\n\n" + plain;
        AiClient.generateText(p, model, prompt, (text, e) -> {
            if (cb != null) {
                if (e != null) cb.onResult(null, e);
                else cb.onResult(text != null ? text.trim() : "", null);
            }
        });
    }

    static String stripHtml(String html) {
        if (html == null) return "";
        return android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY)
                .toString().trim();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
