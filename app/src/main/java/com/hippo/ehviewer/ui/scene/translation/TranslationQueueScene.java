package com.hippo.ehviewer.ui.scene.translation;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.scene.ToolbarScene;
import com.hippo.ehviewer.ui.scene.translation.part.TranslationAdapter;
import com.hippo.ehviewer.translation.TranslationQueueManager;
import com.hippo.lib.yorozuya.ViewUtils;

public class TranslationQueueScene extends ToolbarScene {

    private RecyclerView recyclerView;
    private TranslationAdapter adapter;
    private com.hippo.ehviewer.translation.TranslationQueuePoller poller;

    @Override
    public int getNavCheckedItem() {
        return R.id.nav_translation_queue;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView3(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_translation_queue, container, false);
        View content = ViewUtils.$$(view, R.id.content);
        recyclerView = (RecyclerView) ViewUtils.$$(content, R.id.recycler_view);
        adapter = new TranslationAdapter(this, TranslationQueueManager.getInstance());
        recyclerView.setLayoutManager(new LinearLayoutManager(getEHContext()));
        recyclerView.setAdapter(adapter);
        setTitle(getString(R.string.translation_queue));
        TranslationQueueManager.getInstance().addListener(onQueueChanged);
        return view;
    }

    private final TranslationQueueManager.Listener onQueueChanged = new TranslationQueueManager.Listener() {
        @Override public void onChanged() {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (adapter != null) adapter.notifyDataSetChanged();
            });
        }
    };

    @Override
    public void onDestroyView() {
        TranslationQueueManager.getInstance().removeListener(onQueueChanged);
        if (poller != null) poller.stop();
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24);
        poller = new com.hippo.ehviewer.translation.TranslationQueuePoller();
        poller.start();
    }

    @Override
    public int getMenuResId() {
        return R.menu.scene_translation_queue;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete_all_records) {
            android.content.Context context = getEHContext();
            if (context == null) return false;
            new android.app.AlertDialog.Builder(context)
                    .setMessage(R.string.translation_delete_all_records_confirm)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        TranslationQueueManager.getInstance().clearAll();
                    })
                    .show();
            return true;
        }
        return false;
    }

    @Override
    public void onNavigationClick(View view) {
        onBackPressed();
    }
}
