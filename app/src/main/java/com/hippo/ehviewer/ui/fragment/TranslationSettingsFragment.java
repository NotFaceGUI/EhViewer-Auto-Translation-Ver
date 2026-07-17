package com.hippo.ehviewer.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.hippo.app.EditTextDialogBuilder;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.translation.GeminiApi;
import com.hippo.ehviewer.translation.TranslationApi;
import com.hippo.ehviewer.translation.provider.Provider;
import com.hippo.ehviewer.translation.provider.ProviderStore;

import java.util.ArrayList;
import java.util.List;

public class TranslationSettingsFragment extends BasePreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener {

    private static final String KEY_TRANSLATION_BASE_HOST = "translation_base_host";
    private static final String KEY_TRANSLATION_BASE_PORT = "translation_base_port";
    private static final String KEY_TRANSLATION_TEST = "translation_test_connection";
    private static final String KEY_GEMINI_COMMON_PROMPT = "gemini_common_prompt";
    private static final String KEY_AI_MODEL = "ai_model_setting";
    private static final String KEY_TARGET_LANG = "target_language_setting";
    private static final String KEY_DEFAULT_METHOD = "translation_default_method";
    private static final String KEY_TEXT_PROVIDER = "text_translation_provider";

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.translation_settings);

        Preference transHost = findPreference(KEY_TRANSLATION_BASE_HOST);
        Preference transPort = findPreference(KEY_TRANSLATION_BASE_PORT);
        Preference transTest = findPreference(KEY_TRANSLATION_TEST);
        Preference geminiCommonPrompt = findPreference(KEY_GEMINI_COMMON_PROMPT);
        Preference aiModel = findPreference(KEY_AI_MODEL);
        Preference targetLang = findPreference(KEY_TARGET_LANG);
        Preference defaultMethod = findPreference(KEY_DEFAULT_METHOD);
        Preference textProvider = findPreference(KEY_TEXT_PROVIDER);

        if (transHost != null) transHost.setOnPreferenceClickListener(this);
        if (transPort != null) transPort.setOnPreferenceClickListener(this);
        if (transTest != null) transTest.setOnPreferenceClickListener(this);
        if (geminiCommonPrompt != null) geminiCommonPrompt.setOnPreferenceClickListener(this);
        if (aiModel != null) aiModel.setOnPreferenceClickListener(this);
        if (targetLang != null) targetLang.setOnPreferenceClickListener(this);
        if (textProvider != null) textProvider.setOnPreferenceClickListener(this);

        if (transHost != null) {
            String host = Settings.getString(KEY_TRANSLATION_BASE_HOST, "127.0.0.1");
            transHost.setSummary(host);
        }
        if (transPort != null) {
            int port = Settings.getIntFromStr(KEY_TRANSLATION_BASE_PORT, 8000);
            transPort.setSummary(Integer.toString(port));
        }
        if (geminiCommonPrompt != null) {
            String p = Settings.getString(KEY_GEMINI_COMMON_PROMPT, "");
            if (p == null || p.trim().isEmpty()) p = GeminiApi.getCommonPrompt();
            geminiCommonPrompt.setSummary(summarize(p.trim()));
        }
        if (targetLang != null) {
            targetLang.setSummary(getTargetLanguage());
        }
        refreshAiModelSummary();
        refreshTextProviderSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAiModelSummary();
        refreshTextProviderSummary();
    }

    private void refreshAiModelSummary() {
        Preference aiModel = findPreference(KEY_AI_MODEL);
        if (aiModel == null) return;
        Provider dp = ProviderStore.getInstance().getDefaultProvider();
        if (dp != null && dp.apiKey != null && !dp.apiKey.trim().isEmpty()) {
            String model = dp.selectedModel != null ? dp.selectedModel : getString(R.string.ai_provider_no_model);
            aiModel.setSummary(dp.name + " / " + model);
        } else {
            aiModel.setSummary(getString(R.string.ai_provider_no_model));
        }
    }

    private void refreshTextProviderSummary() {
        Preference pref = findPreference(KEY_TEXT_PROVIDER);
        if (pref == null) return;
        String pid = Settings.getString(KEY_TEXT_PROVIDER, "");
        if (!pid.isEmpty()) {
            Provider p = ProviderStore.getInstance().getProvider(pid);
            if (p != null) {
                String model = p.selectedChatModel != null && !p.selectedChatModel.trim().isEmpty()
                        ? p.selectedChatModel.trim() : "chat";
                pref.setSummary(p.name + " / " + model);
                return;
            }
        }
        Provider p = ProviderStore.getInstance().getDefaultProvider();
        if (p != null && p.protocol == Provider.PROTOCOL_OPENAI && p.apiKey != null && !p.apiKey.trim().isEmpty()) {
            pref.setSummary("auto: " + p.name);
        } else {
            pref.setSummary(getString(R.string.ai_provider_no_model));
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_AI_MODEL.equals(key)) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.settings, new ProviderSettingsFragment())
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        if (KEY_TEXT_PROVIDER.equals(key)) {
            showTextProviderDialog(preference);
            return true;
        }
        if (KEY_TARGET_LANG.equals(key)) {
            EditTextDialogBuilder builder = new EditTextDialogBuilder(getContext(), null, getTargetLanguage());
            builder.setTitle(R.string.target_language_title);
            builder.setPositiveButton(android.R.string.ok, null);
            AlertDialog dlg = builder.show();
            Button btn = dlg.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    String t = builder.getText();
                    String lang = t == null ? "简体中文" : t.trim();
                    if (lang.isEmpty()) lang = "简体中文";
                    Settings.putString(KEY_TARGET_LANG, lang);
                    preference.setSummary(lang);
                    dlg.dismiss();
                });
            }
            return true;
        }
        switch (key) {
            case KEY_TRANSLATION_BASE_HOST: {
                EditTextDialogBuilder builder2 = new EditTextDialogBuilder(getContext(), null, getString(R.string.translation_base_host));
                builder2.setTitle(R.string.translation_base_host);
                builder2.setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder2.show();
                Button button = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setOnClickListener(v -> {
                        String text = builder2.getText();
                        if (text == null || text.trim().isEmpty()) { dialog.dismiss(); return; }
                        Settings.putString(KEY_TRANSLATION_BASE_HOST, text.trim());
                        preference.setSummary(text.trim());
                        dialog.dismiss();
                    });
                }
                return true;
            }
            case KEY_TRANSLATION_BASE_PORT: {
                EditTextDialogBuilder builder3 = new EditTextDialogBuilder(getContext(), null, getString(R.string.translation_base_port));
                builder3.setTitle(R.string.translation_base_port);
                builder3.setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder3.show();
                Button button = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setOnClickListener(v -> {
                        String text = builder3.getText();
                        int port;
                        try { port = Integer.parseInt(text.trim()); } catch (Exception e) { port = 8000; }
                        if (port <= 0) port = 8000;
                        Settings.putIntToStr(KEY_TRANSLATION_BASE_PORT, port);
                        preference.setSummary(Integer.toString(port));
                        dialog.dismiss();
                    });
                }
                return true;
            }
            case KEY_TRANSLATION_TEST: {
                final Activity act = getActivity();
                TranslationApi.testConnectionAsync((ok, code, e) -> {
                    if (act == null) return;
                    act.runOnUiThread(() -> Toast.makeText(act, ok ? "OK" : ("Failed(" + code + ")"), Toast.LENGTH_SHORT).show());
                });
                return true;
            }
            case KEY_GEMINI_COMMON_PROMPT: {
                String cur = Settings.getString(KEY_GEMINI_COMMON_PROMPT, "");
                if (cur.isEmpty()) cur = GeminiApi.getCommonPrompt();
                EditTextDialogBuilder builder4 = new EditTextDialogBuilder(getContext(), cur, getString(R.string.gemini_common_prompt));
                builder4.setTitle(R.string.gemini_common_prompt);
                builder4.setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder4.show();
                Button button = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setOnClickListener(v -> {
                        String text = builder4.getText();
                        String t = text == null ? "" : text.trim();
                        if (t.isEmpty()) t = GeminiApi.getCommonPrompt();
                        Settings.putString(KEY_GEMINI_COMMON_PROMPT, t);
                        preference.setSummary(summarize(t));
                        dialog.dismiss();
                    });
                }
                return true;
            }
            default:
                return false;
        }
    }

    private void showTextProviderDialog(Preference preference) {
        List<Provider> openaiProviders = new ArrayList<>();
        for (Provider p : ProviderStore.getInstance().getProviders()) {
            if (p.protocol == Provider.PROTOCOL_OPENAI && p.enabled && p.apiKey != null && !p.apiKey.trim().isEmpty()) {
                openaiProviders.add(p);
            }
        }
        if (openaiProviders.isEmpty()) {
            Toast.makeText(getContext(), R.string.ai_provider_no_model, Toast.LENGTH_SHORT).show();
            return;
        }
        String currentId = Settings.getString(KEY_TEXT_PROVIDER, "");
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        RadioGroup rg = new RadioGroup(getContext());
        rg.setOrientation(LinearLayout.VERTICAL);
        int checkedId = -1;
        for (int i = 0; i < openaiProviders.size(); i++) {
            Provider p = openaiProviders.get(i);
            RadioButton rb = new RadioButton(getContext());
            rb.setId(android.view.View.generateViewId());
            String model = p.selectedChatModel != null && !p.selectedChatModel.trim().isEmpty()
                    ? p.selectedChatModel.trim() : "chat";
            rb.setText(p.name + " (" + model + ")");
            rg.addView(rb);
            if (i == 0) checkedId = rb.getId();
            if (!currentId.isEmpty() && p.id.equals(currentId)) {
                checkedId = rb.getId();
            }
        }
        if (checkedId >= 0) rg.check(checkedId);
        layout.addView(rg);

        final List<Provider> finalProviders = openaiProviders;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.text_translation_provider_title)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, (d, which) -> {
                    int sel = rg.getCheckedRadioButtonId();
                    for (int i = 0; i < rg.getChildCount(); i++) {
                        if (rg.getChildAt(i).getId() == sel) {
                            Settings.putString(KEY_TEXT_PROVIDER, finalProviders.get(i).id);
                            refreshTextProviderSummary();
                            break;
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static String summarize(String s) {
        if (s == null) return "";
        if (s.length() <= 80) return s;
        return s.substring(0, 77) + "...";
    }

    public static String getTargetLanguage() {
        return com.hippo.ehviewer.Settings.getString("target_language_setting", "简体中文");
    }
}
