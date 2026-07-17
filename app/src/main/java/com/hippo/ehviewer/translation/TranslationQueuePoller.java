package com.hippo.ehviewer.translation;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;

public class TranslationQueuePoller implements Runnable {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;
    private int intervalMs = 2000;
    private static final String TAG = "TranslationPoller";

    public void start() {
        if (running) return;
        running = true;
        handler.post(this);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(this);
    }

    @Override
    public void run() {
        if (!running) return;
//        Log.d(TAG, "tick");
        String jid = TranslationQueueManager.getInstance().getCurrentJobId();
        if (jid == null || TranslationQueueManager.getInstance().isCurrentLlm()) {
            handler.postDelayed(this, intervalMs);
            return;
        }
        TranslationApi.getStatusAsync(jid, (status, err2) -> {
            if (!running) return;
            Integer pp = null, tp = null;
            String statusText = null;
            if (err2 == null && status != null) {
                statusText = status.optString("status", null);
                if (status.has("process_progress")) pp = (int)(status.optDouble("process_progress") * 100);
                if (status.has("translate_progress")) tp = (int)(status.optDouble("translate_progress") * 100);
                if (pp == null || tp == null) {
                    JSONObject prog = status.optJSONObject("progress");
                    if (prog != null) {
                        double d = prog.optDouble("detect", 0), o = prog.optDouble("ocr", 0), i = prog.optDouble("inpaint", 0);
                        double t = prog.optDouble("translate", 0);
                        pp = pp == null ? (int)(Math.max(Math.max(d, o), i) * 100) : pp;
                        tp = tp == null ? (int)(t * 100) : tp;
                    }
                }
            }
            final Integer fpp = pp, ftp = tp;
            final String fst = statusText;
            handler.post(() -> TranslationQueueManager.getInstance().updateCurrentDetailed(jid, fpp, ftp, fst));
            handler.postDelayed(this, intervalMs);
        });
    }
}
