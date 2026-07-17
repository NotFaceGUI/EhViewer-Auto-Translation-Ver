package com.hippo.ehviewer.translation.context;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlossaryExtractor {

    private static final String TAG = "GlossaryExtractor";

    private static final Pattern MAPPING_PATTERN = Pattern.compile(
            "([A-Za-z][A-Za-z\\-'. ]{1,30})\\s*(?:->|→|=>)\\s*([\\u4e00-\\u9fa5·]{1,20})");

    private static final Pattern GLOSSARY_LINE = Pattern.compile(
            "([^\\s,;:：，；\\n]{1,30})\\s*[:：是]\\s*([\\u4e00-\\u9fa5·]{1,20})");

    public static List<Glossary.Entry> extractFromText(String text) {
        List<Glossary.Entry> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;
        Matcher m = MAPPING_PATTERN.matcher(text);
        while (m.find() && result.size() < 32) {
            String src = m.group(1);
            String dst = m.group(2);
            if (src != null && dst != null) {
                result.add(new Glossary.Entry(src.trim(), dst.trim()));
            }
        }
        if (result.isEmpty()) {
            Matcher m2 = GLOSSARY_LINE.matcher(text);
            while (m2.find() && result.size() < 16) {
                String src = m2.group(1);
                String dst = m2.group(2);
                if (src != null && dst != null && isLikelyName(src)) {
                    result.add(new Glossary.Entry(src.trim(), dst.trim()));
                }
            }
        }
        return result;
    }

    public static void updateGlossary(Glossary glossary, String modelOutputText) {
        if (glossary == null || modelOutputText == null || modelOutputText.isEmpty()) return;
        List<Glossary.Entry> extracted = extractFromText(modelOutputText);
        if (!extracted.isEmpty()) {
            glossary.addAll(extracted);
            glossary.save();
            Log.d(TAG, "Updated glossary with " + extracted.size() + " entries");
        }
    }

    private static boolean isLikelyName(String s) {
        if (s == null || s.length() < 2) return false;
        if (s.length() > 30) return false;
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) return true;
        return s.matches("[\\u4e00-\\u9fa5·]{2,10}");
    }
}
