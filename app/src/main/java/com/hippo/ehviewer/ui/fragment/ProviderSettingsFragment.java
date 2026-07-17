package com.hippo.ehviewer.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.hippo.app.EditTextDialogBuilder;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.translation.provider.ModelFetcher;
import com.hippo.ehviewer.translation.provider.ModelInfo;
import com.hippo.ehviewer.translation.provider.Provider;
import com.hippo.ehviewer.translation.provider.ProviderStore;
import com.hippo.preference.ListPreference;

import java.util.List;

public class ProviderSettingsFragment extends BasePreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String KEY_ADD = "ai_provider_add";
    private static final String KEY_LIST_CATEGORY = "ai_provider_list_category";
    private static final String KEY_BATCH_MODE = "ai_batch_mode";
    private static final String KEY_DEFAULT_PROVIDER = "ai_default_provider";
    private static final String KEY_ANALYSIS_MODE = "ai_analysis_mode";

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.provider_settings);

        Preference addPref = findPreference(KEY_ADD);
        if (addPref != null) addPref.setOnPreferenceClickListener(this);

        ListPreference batchPref = findPreference(KEY_BATCH_MODE);
        if (batchPref != null) {
            batchPref.setOnPreferenceChangeListener(this);
            String v = Settings.getString(KEY_BATCH_MODE, "true");
            batchPref.setValue(v);
            batchPref.setSummary("true".equals(v) ? getString(R.string.ai_batch_mode_summary) : getString(R.string.ai_batch_mode_single));
        }

        ListPreference defaultProviderPref = findPreference(KEY_DEFAULT_PROVIDER);
        if (defaultProviderPref != null) {
            defaultProviderPref.setOnPreferenceChangeListener(this);
            refreshDefaultProviderPref();
        }

        ListPreference analysisModePref = findPreference(KEY_ANALYSIS_MODE);
        if (analysisModePref != null) {
            analysisModePref.setOnPreferenceChangeListener(this);
            String am = Settings.getString(KEY_ANALYSIS_MODE, "chunk_one");
            analysisModePref.setValue(am);
            analysisModePref.setSummary(getAnalysisModeSummary(am));
        }

        rebuildProviderList();
    }

    private void refreshDefaultProviderPref() {
        ListPreference defaultProviderPref = findPreference(KEY_DEFAULT_PROVIDER);
        if (defaultProviderPref == null) return;
        List<Provider> providers = ProviderStore.getInstance().getProviders();
        CharSequence[] entries = new CharSequence[providers.size()];
        CharSequence[] values = new CharSequence[providers.size()];
        for (int i = 0; i < providers.size(); i++) {
            entries[i] = providers.get(i).name;
            values[i] = providers.get(i).id;
        }
        defaultProviderPref.setEntries(entries);
        defaultProviderPref.setEntryValues(values);
        String current = ProviderStore.getInstance().getDefaultProviderId();
        defaultProviderPref.setValue(current);
        Provider dp = ProviderStore.getInstance().getProvider(current);
        defaultProviderPref.setSummary(dp != null ? dp.name : "");
    }

    private void rebuildProviderList() {
        PreferenceCategory cat = findPreference(KEY_LIST_CATEGORY);
        if (cat == null) return;
        for (int i = cat.getPreferenceCount() - 1; i >= 0; i--) {
            Preference p = cat.getPreference(i);
            if (!KEY_ADD.equals(p.getKey())) cat.removePreference(p);
        }

        List<Provider> providers = ProviderStore.getInstance().getProviders();
        for (Provider p : providers) {
            Preference pref = new Preference(getContext());
            pref.setKey("provider_" + p.id);
            pref.setTitle(p.name + (p.enabled ? "" : " (" + getString(R.string.ai_provider_disabled) + ")"));
            String summary = buildProviderSummary(p);
            pref.setSummary(summary);
            pref.setOnPreferenceClickListener(this);
            cat.addPreference(pref);
        }
        refreshDefaultProviderPref();
    }

    private String buildProviderSummary(Provider p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.protocolName());
        if (p.isBuiltin) sb.append(" | ").append(getString(R.string.ai_provider_builtin));
        else sb.append(" | ").append(getString(R.string.ai_provider_custom));
        if (p.selectedModel != null && !p.selectedModel.isEmpty()) {
            sb.append("\n").append(getString(R.string.ai_provider_current_model, p.selectedModel));
        } else {
            sb.append("\n").append(getString(R.string.ai_provider_no_model));
        }
        return sb.toString();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (key == null) return false;
        if (KEY_ADD.equals(key)) {
            showAddProviderDialog();
            return true;
        }
        if (key.startsWith("provider_")) {
            String id = key.substring("provider_".length());
            Provider p = ProviderStore.getInstance().getProvider(id);
            if (p != null) {
                showEditProviderDialog(p);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_BATCH_MODE.equals(key)) {
            String v = String.valueOf(newValue);
            Settings.putString(KEY_BATCH_MODE, v);
            if (preference instanceof ListPreference) {
                ((ListPreference) preference).setValue(v);
                preference.setSummary("true".equals(v) ? getString(R.string.ai_batch_mode_summary) : getString(R.string.ai_batch_mode_single));
            }
            return true;
        }
        if (KEY_DEFAULT_PROVIDER.equals(key)) {
            String id = String.valueOf(newValue);
            ProviderStore.getInstance().setDefaultProviderId(id);
            if (preference instanceof ListPreference) {
                ((ListPreference) preference).setValue(id);
                Provider dp = ProviderStore.getInstance().getProvider(id);
                preference.setSummary(dp != null ? dp.name : "");
            }
            return true;
        }
        if (KEY_ANALYSIS_MODE.equals(key)) {
            String v = String.valueOf(newValue);
            Settings.putString(KEY_ANALYSIS_MODE, v);
            if (preference instanceof ListPreference) {
                ((ListPreference) preference).setValue(v);
                preference.setSummary(getAnalysisModeSummary(v));
            }
            return true;
        }
        return false;
    }

    private void showAddProviderDialog() {
        Provider p = new Provider("custom_" + System.currentTimeMillis(), getString(R.string.ai_provider_default_name), Provider.PROTOCOL_OPENAI, "", false);
        showEditProviderDialog(p, true);
    }

    private void showEditProviderDialog(Provider p) {
        showEditProviderDialog(p, false);
    }

    private void showEditProviderDialog(Provider p, boolean isNew) {
        if (getContext() == null) return;
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_provider_edit, null);
        EditText nameEt = view.findViewById(R.id.et_name);
        EditText baseUrlEt = view.findViewById(R.id.et_base_url);
        EditText apiKeyEt = view.findViewById(R.id.et_api_key);
        Spinner protocolSp = view.findViewById(R.id.sp_protocol);
        Spinner resolutionSp = view.findViewById(R.id.sp_resolution);
        Spinner modelSp = view.findViewById(R.id.sp_model);
        Spinner chatModelSp = view.findViewById(R.id.sp_chat_model);

        String[] protocols = {getString(R.string.ai_provider_protocol_openai), getString(R.string.ai_provider_protocol_gemini)};
        ArrayAdapter<String> protoAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, protocols);
        protoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        protocolSp.setAdapter(protoAdapter);
        protocolSp.setSelection(p.protocol);

        nameEt.setText(p.name);
        baseUrlEt.setText(p.baseUrl);
        apiKeyEt.setText(p.apiKey);

        String[] resolutions = {"1k (1024×1024)", "2k (2048×2048)", "4k (4096×4096)"};
        String[] resolutionValues = {"1k", "2k", "4k"};
        ArrayAdapter<String> resAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, resolutions);
        resAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolutionSp.setAdapter(resAdapter);
        for (int i = 0; i < resolutionValues.length; i++) {
            if (resolutionValues[i].equals(p.resolutionLevel)) { resolutionSp.setSelection(i); break; }
        }

        List<ModelInfo> models = ProviderStore.getInstance().getModels(p.id);
        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelAdapter.add(getString(R.string.ai_no_models));
        for (ModelInfo m : models) {
            modelAdapter.add(m.id + (m.supportsImage ? " [img]" : ""));
        }
        modelSp.setAdapter(modelAdapter);
        if (p.selectedModel != null) {
            for (int i = 0; i < models.size(); i++) {
                if (models.get(i).id.equals(p.selectedModel)) {
                    modelSp.setSelection(i + 1);
                    break;
                }
            }
        }

        ArrayAdapter<String> chatAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item);
        chatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chatAdapter.add(getString(R.string.ai_no_models));
        for (ModelInfo m : models) chatAdapter.add(m.id);
        chatModelSp.setAdapter(chatAdapter);
        if (p.selectedChatModel != null) {
            for (int i = 0; i < models.size(); i++) {
                if (models.get(i).id.equals(p.selectedChatModel)) {
                    chatModelSp.setSelection(i + 1);
                    break;
                }
            }
        }

        Button fetchBtn = view.findViewById(R.id.btn_fetch_models);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(isNew ? R.string.ai_provider_add : R.string.ai_provider_edit)
                .setView(view)
                .setPositiveButton(R.string.ai_provider_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        fetchBtn.setOnClickListener(v -> {
            String baseUrl = baseUrlEt.getText().toString().trim();
            String apiKey = apiKeyEt.getText().toString().trim();
            int proto = protocolSp.getSelectedItemPosition();
            Provider tmp = new Provider(p.id, p.name, proto, baseUrl, false);
            tmp.apiKey = apiKey;
            if (proto == Provider.PROTOCOL_GEMINI) {
                tmp.authHeader = "x-goog-api-key";
                tmp.authScheme = Provider.AUTH_RAW;
            } else {
                tmp.authHeader = "Authorization";
                tmp.authScheme = Provider.AUTH_BEARER;
            }
            fetchBtn.setEnabled(false);
            fetchBtn.setText(R.string.ai_fetching_models);
            ModelFetcher.fetchModels(tmp, (modelList, e) -> {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    fetchBtn.setEnabled(true);
                    fetchBtn.setText(R.string.ai_provider_fetch_models);
                    if (e != null || modelList == null) {
                        Toast.makeText(getContext(), getString(R.string.ai_fetch_fail, e == null ? "null" : e.getMessage()), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ProviderStore.getInstance().updateProviderModels(p.id, modelList);
                    modelAdapter.clear();
                    chatAdapter.clear();
                    List<ModelInfo> fresh = ProviderStore.getInstance().getModels(p.id);
                    for (ModelInfo m : fresh) {
                        modelAdapter.add(m.id + (m.supportsImage ? " [img]" : ""));
                        chatAdapter.add(m.id);
                    }
                    if (fresh.isEmpty()) {
                        modelAdapter.add(getString(R.string.ai_no_models));
                        chatAdapter.add(getString(R.string.ai_no_models));
                        modelSp.setSelection(0);
                        chatModelSp.setSelection(0);
                    } else {
                        int imgSel = 0, txtSel = 0;
                        for (int i = 0; i < fresh.size(); i++) {
                            if (fresh.get(i).supportsImage && imgSel == 0) imgSel = i + 1;
                            if (!fresh.get(i).supportsImage && txtSel == 0) txtSel = i + 1;
                        }
                        if (imgSel == 0) imgSel = 1;
                        if (txtSel == 0) txtSel = 1;
                        modelSp.setSelection(imgSel);
                        chatModelSp.setSelection(txtSel);
                    }
                    modelAdapter.notifyDataSetChanged();
                    chatAdapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), getString(R.string.ai_fetch_ok, fresh.size()), Toast.LENGTH_SHORT).show();
                });
            });
        });

        dialog.show();
        Button positive = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (positive != null) {
            positive.setOnClickListener(v -> {
                String name = nameEt.getText().toString().trim();
                String baseUrl = baseUrlEt.getText().toString().trim();
                String apiKey = apiKeyEt.getText().toString().trim();
                int proto = protocolSp.getSelectedItemPosition();
                if (name.isEmpty()) { Toast.makeText(getContext(), getString(R.string.ai_provider_name_required), Toast.LENGTH_SHORT).show(); return; }
                if (baseUrl.isEmpty()) { Toast.makeText(getContext(), getString(R.string.ai_provider_url_required), Toast.LENGTH_SHORT).show(); return; }

                p.name = name;
                p.protocol = proto;
                p.baseUrl = baseUrl;
                p.apiKey = apiKey;
                int resIdx = resolutionSp.getSelectedItemPosition();
                p.resolutionLevel = resIdx >= 0 && resIdx < resolutionValues.length ? resolutionValues[resIdx] : "2k";
                p.maxImagePixels = Provider.resolutionToPixels(p.resolutionLevel);
                if (proto == Provider.PROTOCOL_GEMINI) {
                    p.authHeader = "x-goog-api-key";
                    p.authScheme = Provider.AUTH_RAW;
                } else {
                    p.authHeader = "Authorization";
                    p.authScheme = Provider.AUTH_BEARER;
                }
                p.enabled = !apiKey.isEmpty();

                int modelIdx = modelSp.getSelectedItemPosition();
                List<ModelInfo> fresh = ProviderStore.getInstance().getModels(p.id);
                if (!fresh.isEmpty()) {
                    if (modelIdx > 0 && modelIdx - 1 < fresh.size()) {
                        p.selectedModel = fresh.get(modelIdx - 1).id;
                    } else {
                        String sel = p.selectedModel;
                        boolean found = false;
                        for (ModelInfo m : fresh) {
                            if (m.id != null && m.id.equals(sel)) { found = true; break; }
                        }
                        if (!found) {
                            for (ModelInfo m : fresh) {
                                if (m.supportsImage) { p.selectedModel = m.id; break; }
                            }
                            if (p.selectedModel == null || !found && !fresh.isEmpty() && p.selectedModel == null) {
                                p.selectedModel = fresh.get(0).id;
                            }
                        }
                    }
                }

                int chatIdx = chatModelSp.getSelectedItemPosition();
                if (!fresh.isEmpty()) {
                    if (chatIdx > 0 && chatIdx - 1 < fresh.size()) {
                        p.selectedChatModel = fresh.get(chatIdx - 1).id;
                    } else if (chatIdx == 0 && p.selectedChatModel == null) {
                        for (ModelInfo m : fresh) {
                            if (!m.supportsImage) { p.selectedChatModel = m.id; break; }
                        }
                        if (p.selectedChatModel == null && !fresh.isEmpty()) p.selectedChatModel = fresh.get(0).id;
                    }
                }

                ProviderStore.getInstance().addProvider(p);
                rebuildProviderList();
                dialog.dismiss();
            });
        }

        if (!isNew && !p.isBuiltin) {
            Button delBtn = view.findViewById(R.id.btn_delete);
            if (delBtn != null) {
                delBtn.setVisibility(View.VISIBLE);
                delBtn.setOnClickListener(v -> {
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.ai_provider_delete)
                            .setMessage(R.string.ai_provider_delete_confirm)
                            .setPositiveButton(android.R.string.ok, (d, w) -> {
                                ProviderStore.getInstance().removeProvider(p.id);
                                rebuildProviderList();
                                dialog.dismiss();
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                });
            }
        }
    }

    private String getAnalysisModeSummary(String mode) {
        switch (mode) {
            case "chunk_one": return getString(R.string.ai_analysis_mode_chunk_one);
            case "random": return getString(R.string.ai_analysis_mode_random);
            case "all": return getString(R.string.ai_analysis_mode_all);
            case "none": return getString(R.string.ai_analysis_mode_none);
            default: return mode;
        }
    }
}
