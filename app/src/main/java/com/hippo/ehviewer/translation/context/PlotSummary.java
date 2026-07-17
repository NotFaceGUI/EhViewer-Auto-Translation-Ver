package com.hippo.ehviewer.translation.context;

import com.hippo.ehviewer.Settings;

public class PlotSummary {

    public static String buildBatchPrompt(String basePrompt, Glossary glossary, int batchStart, int batchEnd) {
        StringBuilder sb = new StringBuilder();
        if (basePrompt != null && !basePrompt.isEmpty()) sb.append(basePrompt);

        String ctx = glossary != null ? glossary.buildContextText() : "";
        if (!ctx.isEmpty()) {
            sb.append("\n\n---\n").append(ctx);
        }

        sb.append("\n\n---\n");
        sb.append("目标语言：").append(getTargetLang()).append("\n");
        sb.append("【重要】请将图片中的所有文字翻译为").append(getTargetLang()).append("。不要保留原文，不要遗漏任何文字。\n");
        sb.append("当前批次：第").append(batchStart).append("页 至 第").append(batchEnd).append("页（共").append(batchEnd - batchStart + 1).append("页合并为一张大图）。\n");
        sb.append("图片按网格排列，请逐格处理后输出相同布局的大图。每格内仅替换文字，保持背景不变。\n\n");
        sb.append("【术语表更新】请在文本回复中列出本批次发现的新角色名/术语及其翻译，格式：\n");
        sb.append("原名1->译名1\n原名2->译名2\n");
        sb.append("已在上表出现的不要重复列出。没有新发现则回复\"无新术语\"。");

        return sb.toString();
    }

    public static String buildSinglePagePrompt(String basePrompt, Glossary glossary, int page) {
        StringBuilder sb = new StringBuilder();
        if (basePrompt != null && !basePrompt.isEmpty()) sb.append(basePrompt);

        String ctx = glossary != null ? glossary.buildContextText() : "";
        if (!ctx.isEmpty()) {
            sb.append("\n\n---\n").append(ctx);
        }

        sb.append("\n\n---\n");
        sb.append("【重要】请将图片中的所有文字翻译为").append(getTargetLang()).append("。不要保留原文。\n");
        sb.append("当前为第").append(String.valueOf(page)).append("页。");

        return sb.toString();
    }

    private static String getTargetLang() {
        return Settings.getString("target_language_setting", "简体中文");
    }
}
