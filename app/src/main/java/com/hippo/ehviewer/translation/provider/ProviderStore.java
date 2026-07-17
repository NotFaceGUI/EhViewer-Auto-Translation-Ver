package com.hippo.ehviewer.translation.provider;

import android.util.Log;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ProviderStore {
    private static final String TAG = "ProviderStore";
    private static final String FILE_NAME = "ai_providers.json";
    private static final int VERSION = 1;

    private static ProviderStore INSTANCE;

    private final List<Provider> providers = new ArrayList<>();
    private final List<ModelInfo> models = new ArrayList<>();
    private String defaultProviderId = "";
    private final File storageFile;

    private ProviderStore() {
        storageFile = new File(EhApplication.getInstance().getFilesDir(), FILE_NAME);
        load();
        migrateLegacyGeminiIfNeeded();
    }

    public static synchronized ProviderStore getInstance() {
        if (INSTANCE == null) INSTANCE = new ProviderStore();
        return INSTANCE;
    }

    public synchronized List<Provider> getProviders() {
        return new ArrayList<>(providers);
    }

    public synchronized Provider getProvider(String id) {
        for (Provider p : providers) {
            if (p.id != null && p.id.equals(id)) return p;
        }
        return null;
    }

    public synchronized Provider getDefaultProvider() {
        if (defaultProviderId != null && !defaultProviderId.isEmpty()) {
            Provider p = getProvider(defaultProviderId);
            if (p != null) return p;
        }
        for (Provider p : providers) {
            if (p.enabled && p.apiKey != null && !p.apiKey.trim().isEmpty()) return p;
        }
        if (!providers.isEmpty()) return providers.get(0);
        return null;
    }

    public synchronized String getDefaultProviderId() {
        if (defaultProviderId != null && !defaultProviderId.isEmpty()) {
            for (Provider p : providers) {
                if (p.id != null && p.id.equals(defaultProviderId)) return defaultProviderId;
            }
        }
        Provider dp = getDefaultProvider();
        return dp != null ? dp.id : "";
    }

    public synchronized void setDefaultProviderId(String id) {
        this.defaultProviderId = id;
        save();
    }

    public synchronized void addProvider(Provider p) {
        if (p == null || p.id == null) return;
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i).id != null && providers.get(i).id.equals(p.id)) {
                providers.set(i, p);
                save();
                return;
            }
        }
        providers.add(p);
        save();
    }

    public synchronized void removeProvider(String id) {
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i).id != null && providers.get(i).id.equals(id)) {
                providers.remove(i);
                break;
            }
        }
        for (int i = models.size() - 1; i >= 0; i--) {
            if (id != null && id.equals(models.get(i).providerId)) models.remove(i);
        }
        save();
    }

    public synchronized void updateProviderModels(String providerId, List<ModelInfo> newModels) {
        for (int i = models.size() - 1; i >= 0; i--) {
            if (providerId != null && providerId.equals(models.get(i).providerId)) models.remove(i);
        }
        if (newModels != null) models.addAll(newModels);
        save();
    }

    public synchronized List<ModelInfo> getModels(String providerId) {
        List<ModelInfo> result = new ArrayList<>();
        for (ModelInfo m : models) {
            if (providerId != null && providerId.equals(m.providerId)) result.add(m);
        }
        return result;
    }

    public synchronized List<ModelInfo> getImageModels(String providerId) {
        List<ModelInfo> result = new ArrayList<>();
        for (ModelInfo m : models) {
            if (providerId != null && providerId.equals(m.providerId) && m.supportsImage) result.add(m);
        }
        return result;
    }

    public synchronized Provider getDefaultImageProvider() {
        for (Provider p : providers) {
            if (!p.enabled) continue;
            if (p.apiKey == null || p.apiKey.trim().isEmpty()) continue;
            List<ModelInfo> ms = getModels(p.id);
            for (ModelInfo m : ms) {
                if (m.supportsImage) return p;
            }
        }
        return getDefaultProvider();
    }

    public synchronized ModelInfo getModel(String providerId, String modelId) {
        for (ModelInfo m : models) {
            if (providerId != null && providerId.equals(m.providerId) && modelId != null && modelId.equals(m.id)) {
                return m;
            }
        }
        if (modelId != null) return new ModelInfo(modelId, modelId, providerId);
        return null;
    }

    private void migrateLegacyGeminiIfNeeded() {
        String legacyKey = Settings.getString("gemini_api_key", "");
        if (legacyKey == null || legacyKey.trim().isEmpty()) return;
        boolean hasGemini = false;
        for (Provider p : providers) {
            if ("gemini".equals(p.id)) { hasGemini = true; break; }
        }
        if (hasGemini) return;
        Provider p = new Provider("gemini", "Gemini", Provider.PROTOCOL_GEMINI,
                "https://generativelanguage.googleapis.com/v1beta", true);
        p.apiKey = legacyKey.trim();
        p.maxImagePixels = 3840L * 2160L;
        String modelSel = Settings.getString("gemini_model_select", "default");
        if ("custom".equals(modelSel)) {
            String cust = Settings.getString("gemini_model_custom", "");
            if (cust != null && cust.trim().length() > 0) p.selectedModel = cust.trim();
        }
        if (p.selectedModel == null || p.selectedModel.isEmpty()) p.selectedModel = "gemini-3-pro-image-preview";
        p.selectedChatModel = "gemini-2.5-flash";
        providers.add(p);
        save();
        Log.i(TAG, "Migrated legacy Gemini config into ProviderStore");
    }

    private void ensureBuiltin() {
        boolean hasGemini = false, hasXai = false;
        for (Provider p : providers) {
            if ("gemini".equals(p.id)) hasGemini = true;
            if ("xai".equals(p.id)) hasXai = true;
        }
        if (!hasGemini) {
            Provider p = new Provider("gemini", "Gemini", Provider.PROTOCOL_GEMINI,
                    "https://generativelanguage.googleapis.com/v1beta", true);
        p.maxImagePixels = 3840L * 2160L;
        p.resolutionLevel = "4k";
            p.enabled = false;
            p.selectedModel = "gemini-3-pro-image-preview";
            p.selectedChatModel = "gemini-2.5-flash";
            providers.add(p);
        }
        if (!hasXai) {
            Provider p = new Provider("xai", "xAI", Provider.PROTOCOL_OPENAI,
                    "https://api.x.ai/v1", true);
            p.maxImagePixels = 2048L * 2048L;
            p.resolutionLevel = "2k";
            p.enabled = false;
            p.selectedModel = "grok-imagine-image-quality";
            p.selectedChatModel = "grok-4.5";
            providers.add(p);
        }
    }

    private void load() {
        try {
            if (!storageFile.exists()) {
                ensureBuiltin();
                save();
                return;
            }
            byte[] data = readAll(storageFile);
            if (data == null || data.length == 0) {
                ensureBuiltin();
                save();
                return;
            }
            JSONObject root = new JSONObject(new String(data, StandardCharsets.UTF_8));
            defaultProviderId = root.optString("defaultProviderId", "");
            JSONArray arr = root.optJSONArray("providers");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    Provider p = new Provider();
                    p.id = o.optString("id", null);
                    p.name = o.optString("name", "");
                    p.protocol = o.optInt("protocol", Provider.PROTOCOL_OPENAI);
                    p.baseUrl = o.optString("baseUrl", "");
                    p.apiKey = o.optString("apiKey", "");
                    p.authHeader = o.optString("authHeader", p.protocol == Provider.PROTOCOL_GEMINI ? "x-goog-api-key" : "Authorization");
                    p.authScheme = o.optInt("authScheme", p.protocol == Provider.PROTOCOL_GEMINI ? Provider.AUTH_RAW : Provider.AUTH_BEARER);
                    p.maxImagePixels = o.optLong("maxImagePixels", 3840L * 2160L);
                    p.isBuiltin = o.optBoolean("isBuiltin", false);
                    p.enabled = o.optBoolean("enabled", true);
                    p.selectedModel = o.optString("selectedModel", null);
                    p.selectedChatModel = o.optString("selectedChatModel", null);
                    p.resolutionLevel = o.optString("resolutionLevel", "2k");
                    providers.add(p);
                }
            }
            JSONArray marr = root.optJSONArray("models");
            if (marr != null) {
                for (int i = 0; i < marr.length(); i++) {
                    JSONObject o = marr.optJSONObject(i);
                    if (o == null) continue;
                    ModelInfo m = new ModelInfo();
                    m.id = o.optString("id", null);
                    m.name = o.optString("name", m.id);
                    m.providerId = o.optString("providerId", null);
                    m.supportsImage = o.optBoolean("supportsImage", false);
                    m.supportsText = o.optBoolean("supportsText", false);
                    models.add(m);
                }
            }
            ensureBuiltin();
        } catch (Exception e) {
            Log.e(TAG, "load failed", e);
            ensureBuiltin();
        }
    }

    public synchronized void save() {
        try {
            JSONObject root = new JSONObject();
            root.put("version", VERSION);
            root.put("defaultProviderId", defaultProviderId == null ? "" : defaultProviderId);
            JSONArray arr = new JSONArray();
            for (Provider p : providers) {
                JSONObject o = new JSONObject();
                o.put("id", p.id);
                o.put("name", p.name);
                o.put("protocol", p.protocol);
                o.put("baseUrl", p.baseUrl);
                o.put("apiKey", p.apiKey);
                o.put("authHeader", p.authHeader);
                o.put("authScheme", p.authScheme);
                o.put("maxImagePixels", p.maxImagePixels);
                o.put("isBuiltin", p.isBuiltin);
                o.put("enabled", p.enabled);
                o.put("selectedModel", p.selectedModel);
                o.put("selectedChatModel", p.selectedChatModel);
                o.put("resolutionLevel", p.resolutionLevel);
                arr.put(o);
            }
            root.put("providers", arr);
            JSONArray marr = new JSONArray();
            for (ModelInfo m : models) {
                JSONObject o = new JSONObject();
                o.put("id", m.id);
                o.put("name", m.name);
                o.put("providerId", m.providerId);
                o.put("supportsImage", m.supportsImage);
                o.put("supportsText", m.supportsText);
                marr.put(o);
            }
            root.put("models", marr);
            writeAll(storageFile, root.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "save failed", e);
        }
    }

    private static byte[] readAll(File f) {
        try (FileInputStream fis = new FileInputStream(f)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
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
