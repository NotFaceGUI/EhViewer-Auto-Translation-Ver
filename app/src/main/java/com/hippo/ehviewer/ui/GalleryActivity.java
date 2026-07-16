/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui;

import static com.hippo.ehviewer.ui.scene.download.DownloadsScene.LOCAL_GALLERY_INFO_CHANGE;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.hippo.android.resource.AttrResources;
import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.event.GalleryActivityEvent;
import com.hippo.ehviewer.gallery.ArchiveGalleryProvider;
import com.hippo.ehviewer.gallery.DirGalleryProvider;
import com.hippo.ehviewer.gallery.EhGalleryProvider;
import com.hippo.ehviewer.gallery.GalleryProvider2;
import com.hippo.ehviewer.widget.GalleryGuideView;
import com.hippo.ehviewer.widget.GalleryHeader;
import com.hippo.ehviewer.widget.ReversibleSeekBar;
import com.hippo.lib.glgallery.GalleryProvider;
import com.hippo.lib.glgallery.GalleryView;
import com.hippo.lib.glgallery.SimpleAdapter;
import com.hippo.lib.glview.view.GLRootView;
import com.hippo.unifile.UniFile;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.dao.DownloadInfo;
import com.hippo.ehviewer.translation.TranslationQueueManager;
import com.hippo.ehviewer.translation.GeminiApi;
import com.hippo.ehviewer.spider.SpiderDen;
import com.hippo.app.EditTextDialogBuilder;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.SystemUiHelper;
import com.hippo.widget.ColorView;
import com.hippo.lib.yorozuya.AnimationUtils;
import com.hippo.lib.yorozuya.ConcurrentPool;
import com.hippo.lib.yorozuya.IOUtils;
import com.hippo.lib.yorozuya.MathUtils;
import com.hippo.lib.yorozuya.ResourcesUtils;
import com.hippo.lib.yorozuya.SimpleAnimatorListener;
import com.hippo.lib.yorozuya.SimpleHandler;
import com.hippo.lib.yorozuya.ViewUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class GalleryActivity extends EhActivity implements SeekBar.OnSeekBarChangeListener, GalleryView.Listener {

    public static final String ACTION_DIR = "dir";
    public static final String ACTION_EH = "eh";

    public static final String KEY_ACTION = "action";
    public static final String KEY_FILENAME = "filename";
    public static final String KEY_URI = "uri";
    public static final String KEY_GALLERY_INFO = "gallery_info";
    public static final String DATA_IN_EVENT = "data_in_event";
    public static final String KEY_PAGE = "page";
    public static final String KEY_CURRENT_INDEX = "current_index";

    private static final long SLIDER_ANIMATION_DURING = 150;
    private static final long HIDE_SLIDER_DELAY = 3000;

    private static final int WRITE_REQUEST_CODE = 43;

    private String mAction;
    private String mFilename;
    private Uri mUri;
    private GalleryInfo mGalleryInfo;
    private int mPage;
    private String mCacheFileName;

    @Nullable
    private GLRootView mGLRootView;
    @Nullable
    private GalleryView mGalleryView;
    @Nullable
    private GalleryProvider2 mGalleryProvider;
    @Nullable
    private GalleryAdapter mGalleryAdapter;

    @Nullable
    private SystemUiHelper mSystemUiHelper;
    private boolean mShowSystemUi;

    @Nullable
    private ColorView mMaskView;
    @Nullable
    private View mClock;
    @Nullable
    private TextView mProgress;
    @Nullable
    private View mBattery;
    @Nullable
    private View mSeekBarPanel;
    @Nullable
    private ImageView mAutoTransferPanel;
    @Nullable
    private ImageView mTranslateToggle;
    @Nullable
    private TextView mLeftText;
    @Nullable
    private TextView mRightText;
    @Nullable
    private ReversibleSeekBar mSeekBar;

    private ObjectAnimator mSeekBarPanelAnimator;
    private ObjectAnimator mAutoTransferAnimator;
    private ObjectAnimator mTranslateAnimator;

    private java.util.HashMap<Integer, java.util.ArrayList<RectF>> mLlmMaskRegions = new java.util.HashMap<>();

    private int mLayoutMode;
    private int mSize;
    private int mCurrentIndex;

    private boolean canFinish = false;
    private boolean autoTransferring = false;

    private final ConcurrentPool<NotifyTask> mNotifyTaskPool = new ConcurrentPool<>(3);

    private ScheduledExecutorService transferService = Executors.newSingleThreadScheduledExecutor();
    private final Handler transHandle = new Handler(Looper.getMainLooper());

    private final ValueAnimator.AnimatorUpdateListener mUpdateSliderListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (null != mSeekBarPanel) {
                mSeekBarPanel.requestLayout();
            }
            if (null != mAutoTransferPanel) {
                mAutoTransferPanel.requestLayout();
            }
        }
    };

    private final SimpleAnimatorListener mShowSliderListener = new SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mSeekBarPanelAnimator = null;
            mAutoTransferAnimator = null;
        }
    };

    private final SimpleAnimatorListener mHideSliderListener = new SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mSeekBarPanelAnimator = null;
            if (mSeekBarPanel != null) {
                mSeekBarPanel.setVisibility(View.INVISIBLE);
            }
            mAutoTransferAnimator = null;
            if (mAutoTransferPanel != null) {
                mAutoTransferPanel.setVisibility(View.INVISIBLE);
            }
        }
    };

    private final Runnable mHideSliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSeekBarPanel != null) {
                hideSlider(mSeekBarPanel, mSeekBarPanelAnimator);
                hideSlider(mAutoTransferPanel, mAutoTransferAnimator);
                if (mTranslateToggle != null) mTranslateToggle.setVisibility(View.INVISIBLE);
            }
        }
    };

    @Override
    protected int getThemeResId(int theme) {
        switch (theme) {
            case Settings.THEME_LIGHT:
            default:
                return R.style.AppTheme_Gallery;
            case Settings.THEME_DARK:
                return R.style.AppTheme_Gallery_Dark;
            case Settings.THEME_BLACK:
                return R.style.AppTheme_Gallery_Black;
        }
    }

    private void buildProvider() {
        if (mGalleryProvider != null) {
            return;
        }

        if (ACTION_DIR.equals(mAction)) {
            if (mFilename != null) {
                mGalleryProvider = new DirGalleryProvider(UniFile.fromFile(new File(mFilename)));
            }
        } else if (ACTION_EH.equals(mAction)) {
            if (mGalleryInfo != null) {
                mGalleryProvider = new EhGalleryProvider(this, mGalleryInfo);
            }
        } else if (Intent.ACTION_VIEW.equals(mAction)) {
            if (mUri != null) {
                // Only support zip now
                mGalleryProvider = new ArchiveGalleryProvider(this, mUri);
            }
        }
    }

    /**
     * eventbus 通知，用于修复跳转奔溃的问题
     *
     * @param event 通知数据对象
     */
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onGalleryActivityEvent(GalleryActivityEvent event) {
        if (mGalleryProvider != null) {
            return;
        }
        mGalleryInfo = event.galleryInfo;
        mPage = event.pagePosition;
        buildProvider();
        onCreateView(null);
    }

    private void onInit() {
        Intent intent = getIntent();
        if (intent == null) {
            canFinish = true;
            return;
        }

        mAction = intent.getAction();
        mFilename = intent.getStringExtra(KEY_FILENAME);
        mUri = intent.getData();
        mGalleryInfo = intent.getParcelableExtra(KEY_GALLERY_INFO);
        boolean onEvent = intent.getBooleanExtra(DATA_IN_EVENT, false);
        if (!onEvent) {
            canFinish = true;
        }
        mPage = intent.getIntExtra(KEY_PAGE, -1);
        buildProvider();
    }

    private void onRestore(@NonNull Bundle savedInstanceState) {
        mAction = savedInstanceState.getString(KEY_ACTION);
        mFilename = savedInstanceState.getString(KEY_FILENAME);
        mUri = savedInstanceState.getParcelable(KEY_URI);
        mGalleryInfo = savedInstanceState.getParcelable(KEY_GALLERY_INFO);
        mPage = savedInstanceState.getInt(KEY_PAGE, -1);
        mCurrentIndex = savedInstanceState.getInt(KEY_CURRENT_INDEX);
        buildProvider();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ACTION, mAction);
        outState.putString(KEY_FILENAME, mFilename);
        outState.putParcelable(KEY_URI, mUri);
        if (mGalleryInfo != null) {
            outState.putParcelable(KEY_GALLERY_INFO, mGalleryInfo);
        }
        outState.putInt(KEY_PAGE, mPage);
        outState.putInt(KEY_CURRENT_INDEX, mCurrentIndex);
    }

    @Override
    @SuppressWarnings({"WrongConstant"})
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (Settings.getReadingFullscreen()) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        super.onCreate(savedInstanceState);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
        onCreateView(savedInstanceState);
        //注册事件
        EventBus.getDefault().register(this);
    }

    private void onCreateView(@Nullable Bundle savedInstanceState) {
        if (mGalleryProvider == null) {
            if (!canFinish) {
                return;
            }
            finish();
            return;
        }
        mGalleryProvider.start();

        // Get start page
        int startPage;
        if (savedInstanceState == null) {
            startPage = mPage >= 0 ? mPage : mGalleryProvider.getStartPage();
        } else {
            startPage = mCurrentIndex;
        }

        if (!isEglAvailable()) {
            mGalleryProvider.stop();
            showGlFallbackView();
            return;
        }

        setContentView(R.layout.activity_gallery);
        mGLRootView = (GLRootView) ViewUtils.$$(this, R.id.gl_root_view);
        mGalleryAdapter = new GalleryAdapter(mGLRootView, mGalleryProvider);
        Resources resources = getResources();
        mGalleryView = new GalleryView.Builder(this, mGalleryAdapter).setListener(this).setLayoutMode(Settings.getReadingDirection()).setScaleMode(Settings.getPageScaling()).setStartPosition(Settings.getStartPosition()).setStartPage(startPage).setBackgroundColor(AttrResources.getAttrColor(this, android.R.attr.colorBackground)).setEdgeColor(AttrResources.getAttrColor(this, R.attr.colorEdgeEffect) & 0xffffff | 0x33000000).setPagerInterval(Settings.getShowPageInterval() ? resources.getDimensionPixelOffset(R.dimen.gallery_pager_interval) : 0).setScrollInterval(Settings.getShowPageInterval() ? resources.getDimensionPixelOffset(R.dimen.gallery_scroll_interval) : 0).setPageMinHeight(resources.getDimensionPixelOffset(R.dimen.gallery_page_min_height)).setPageInfoInterval(resources.getDimensionPixelOffset(R.dimen.gallery_page_info_interval)).setProgressColor(ResourcesUtils.getAttrColor(this, androidx.appcompat.R.attr.colorPrimary)).setProgressSize(resources.getDimensionPixelOffset(R.dimen.gallery_progress_size)).setPageTextColor(AttrResources.getAttrColor(this, android.R.attr.textColorSecondary)).setPageTextSize(resources.getDimensionPixelOffset(R.dimen.gallery_page_text_size)).setPageTextTypeface(Typeface.DEFAULT).setErrorTextColor(resources.getColor(R.color.red_500, null)).setErrorTextSize(resources.getDimensionPixelOffset(R.dimen.gallery_error_text_size)).setDefaultErrorString(resources.getString(R.string.error_unknown)).setEmptyString(resources.getString(R.string.error_empty)).build();
        mGLRootView.setContentPane(mGalleryView);
        mGLRootView.setOnGenericMotionListener(this::onGenericMotion);
        mGalleryProvider.setListener(mGalleryAdapter);
        mGalleryProvider.setGLRoot(mGLRootView);

        // System UI helper
        if (Settings.getReadingFullscreen()) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            mSystemUiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE, SystemUiHelper.FLAG_LAYOUT_IN_SCREEN_OLDER_DEVICES | SystemUiHelper.FLAG_IMMERSIVE_STICKY);
            mSystemUiHelper.hide();
            mShowSystemUi = false;
        }

        mMaskView = (ColorView) ViewUtils.$$(this, R.id.mask);
        mClock = ViewUtils.$$(this, R.id.clock);
        mProgress = (TextView) ViewUtils.$$(this, R.id.progress);
        mBattery = ViewUtils.$$(this, R.id.battery);
        mClock.setVisibility(Settings.getShowClock() ? View.VISIBLE : View.GONE);
        mProgress.setVisibility(Settings.getShowProgress() ? View.VISIBLE : View.GONE);
        mBattery.setVisibility(Settings.getShowBattery() ? View.VISIBLE : View.GONE);

        mSeekBarPanel = ViewUtils.$$(this, R.id.seek_bar_panel);
        mAutoTransferPanel = (ImageView) ViewUtils.$$(this, R.id.auto_transfer);
        mTranslateToggle = (ImageView) ViewUtils.$$(this, R.id.translate_toggle);
        mLeftText = (TextView) ViewUtils.$$(mSeekBarPanel, R.id.left);
        mRightText = (TextView) ViewUtils.$$(mSeekBarPanel, R.id.right);
        mSeekBar = (ReversibleSeekBar) ViewUtils.$$(mSeekBarPanel, R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mAutoTransferPanel.setOnClickListener(this::autoRead);
        if (mTranslateToggle != null) {
            mTranslateToggle.setOnClickListener(v -> {
                boolean cur = Settings.getBoolean("read_translated_version", false);
                boolean next = !cur;
                if (mGalleryProvider != null && mCurrentIndex >= 0) {
                    if (mGalleryProvider instanceof EhGalleryProvider) {
                        EhGalleryProvider p = (EhGalleryProvider) mGalleryProvider;
                        boolean displayedTranslated = p.isTranslatedDisplayed(mCurrentIndex);
                        if ((next && displayedTranslated) || (!next && !displayedTranslated)) return;
                    }
                    Settings.putBoolean("read_translated_version", next);
                    mGalleryProvider.removeCache(mCurrentIndex);
                    if (!next) {
                        mGalleryProvider.request(mCurrentIndex);
                    } else {
                        if (mGalleryProvider instanceof EhGalleryProvider) {
                            boolean ok = ((EhGalleryProvider) mGalleryProvider).forceLoadTranslated(mCurrentIndex);
                            if (!ok) mGalleryProvider.forceRequest(mCurrentIndex);
                        } else {
                            mGalleryProvider.forceRequest(mCurrentIndex);
                        }
                    }
                }
            });
        }

        mSize = mGalleryProvider.size();
        mCurrentIndex = startPage;
        if (mGalleryView != null) {
            mLayoutMode = mGalleryView.getLayoutMode();
        }
        updateSlider();

        // Update keep screen on
        if (Settings.getKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Orientation
        int orientation;
        switch (Settings.getScreenRotation()) {
            default:
            case 0:
                orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                break;
            case 1:
                orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
                break;
            case 2:
                orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                break;
            case 3:
                orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
                break;
        }
        setRequestedOrientation(orientation);

        // Screen lightness
        setScreenLightness(Settings.getCustomScreenLightness(), Settings.getScreenLightness());

        // Cutout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

            GalleryHeader galleryHeader = findViewById(R.id.gallery_header);
            galleryHeader.setOnApplyWindowInsetsListener((v, insets) -> {
                galleryHeader.setDisplayCutout(insets.getDisplayCutout());
                return insets;
            });
        }

        if (Settings.getGuideGallery()) {
            FrameLayout mainLayout = (FrameLayout) ViewUtils.$$(this, R.id.main);
            mainLayout.addView(new GalleryGuideView(this));
        }
    }

    private boolean isEglAvailable() {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (display == null || display == EGL10.EGL_NO_DISPLAY) {
            return false;
        }

        int[] version = new int[2];
        if (!egl.eglInitialize(display, version)) {
            return false;
        }

        try {
            int[] numConfig = new int[1];
            return egl.eglChooseConfig(display, new int[]{EGL10.EGL_NONE}, null, 0, numConfig)
                    && numConfig[0] > 0;
        } catch (Throwable e) {
            return false;
        } finally {
            egl.eglTerminate(display);
        }
    }

    private void showGlFallbackView() {
        setContentView(R.layout.activity_gallery_fallback);
        View close = ViewUtils.$$(this, R.id.gl_fallback_close);
        close.setOnClickListener(v -> finish());
        Log.w("GalleryActivity", "EGL init failed, switch to non-GL fallback page");
        Toast.makeText(this, R.string.gallery_gl_fallback_toast, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (!transferService.isShutdown()) {
            transferService.shutdown();
            transferService = null;
        }
        mGLRootView = null;
        mGalleryView = null;
        if (mGalleryAdapter != null) {
            mGalleryAdapter.clearUploader();
            mGalleryAdapter = null;
        }
        if (mGalleryProvider != null) {
            mGalleryProvider.setListener(null);
            mGalleryProvider.stop();
            mGalleryProvider = null;
        }

        mMaskView = null;
        mClock = null;
        mProgress = null;
        mBattery = null;
        mSeekBarPanel = null;
        mAutoTransferPanel = null;
        mLeftText = null;
        mRightText = null;
        mSeekBar = null;

        if (transferService != null && !transferService.isShutdown()) {
            transferService.shutdown();
            transferService = null;
        }

        super.onDestroy();
        SimpleHandler.getInstance().removeCallbacks(mHideSliderRunnable);
        //销毁事件
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("info", mGalleryInfo);
        setResult(LOCAL_GALLERY_INFO_CHANGE, intent);
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mGLRootView != null) {
            mGLRootView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mGLRootView != null) {
            mGLRootView.onResume();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        SimpleHandler.getInstance().postDelayed(() -> {
            if (hasFocus && mSystemUiHelper != null) {
                if (mShowSystemUi) {
                    mSystemUiHelper.show();
                } else {
                    mSystemUiHelper.hide();
                }
            }
        }, 300);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mGalleryView == null) {
            return super.onKeyDown(keyCode, event);
        }
        boolean unReverse = !Settings.getReverseVolumePage();
        // Check volume
        if (Settings.getVolumePage()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT && unReverse) {
                    mGalleryView.pageRight();
                } else {
                    mGalleryView.pageLeft();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT && unReverse) {
                    mGalleryView.pageLeft();
                } else {
                    mGalleryView.pageRight();
                }
                return true;
            }
        }

        // Check keyboard and Dpad
        switch (keyCode) {
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_DPAD_UP:
                if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
                    mGalleryView.pageRight();
                } else {
                    mGalleryView.pageLeft();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mGalleryView.pageLeft();
                return true;
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
                    mGalleryView.pageLeft();
                } else {
                    mGalleryView.pageRight();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mGalleryView.pageRight();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_MENU:
                onTapMenuArea();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Check volume
        if (Settings.getVolumePage()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                return true;
            }
        }

        // Check keyboard and Dpad
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP || keyCode == KeyEvent.KEYCODE_PAGE_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

//    private GalleryPageView findPageByIndex(int index) {
//        if (mGalleryView != null) {
//            return mGalleryView.findPageByIndex(index);
//        } else {
//            return null;
//        }
//    }

    private void autoRead(View view) {
        autoTransferring = !autoTransferring;
        if (mAutoTransferPanel == null) {
            return;
        }

        if (!autoTransferring) {
            mAutoTransferPanel.setImageResource(R.drawable.ic_start_play_24);
            transferService.shutdown();
        } else {
            mAutoTransferPanel.setImageResource(R.drawable.ic_pause_circle);
            if (transferService.isShutdown()) {
                transferService = Executors.newSingleThreadScheduledExecutor();
            }
            long initialDelay = Settings.getStartTransferTime();
            long waitTime = initialDelay * 2L;
            try {
                transferService.scheduleWithFixedDelay(() -> transHandle.post(() -> {
                    if (mGalleryView == null) {
                        return;
                    }
                    if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
                        mGalleryView.pageLeft();
                    } else {
                        mGalleryView.pageRight();
                    }
                }), initialDelay, waitTime, TimeUnit.SECONDS);
            } catch (IllegalArgumentException ignore) {

            }
        }
    }

    public boolean onGenericMotion(View view, MotionEvent motionEvent) {
        if (mGalleryView == null) {
            return false;
        }

        if (motionEvent.isFromSource(InputDevice.SOURCE_CLASS_POINTER)) {
            if (motionEvent.getAction() == MotionEvent.ACTION_SCROLL) {
                float scrollY = motionEvent.getAxisValue(MotionEvent.AXIS_VSCROLL);
                if (scrollY == 0) return false;  // wrong input
                if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
                    if (scrollY > 0) {
                        mGalleryView.pageLeft();
                    } else {
                        mGalleryView.pageRight();
                    }
                } else {
                    if (scrollY < 0) {
                        mGalleryView.pageLeft();
                    } else {
                        mGalleryView.pageRight();
                    }
                }
                return true;
            }
        }
        return false;
    }

    @SuppressLint("SetTextI18n")
    private void updateProgress() {
        if (mProgress == null) {
            return;
        }
        if (mSize <= 0 || mCurrentIndex < 0) {
            mProgress.setText(null);
        } else {
            mProgress.setText((mCurrentIndex + 1) + "/" + mSize);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateSlider() {
        if (mSeekBar == null || mRightText == null || mLeftText == null || mSize <= 0 || mCurrentIndex < 0) {
            return;
        }

        TextView start;
        TextView end;
        if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
            start = mRightText;
            end = mLeftText;
            mSeekBar.setReverse(true);
        } else {
            start = mLeftText;
            end = mRightText;
            mSeekBar.setReverse(false);
        }
        start.setText(Integer.toString(mCurrentIndex + 1));
        end.setText(Integer.toString(mSize));
        mSeekBar.setMax(mSize - 1);
        mSeekBar.setProgress(mCurrentIndex);
        if (mTranslateToggle != null) {
            boolean translatedAvail = false;
            try {
                if (mGalleryInfo != null && mCurrentIndex >= 0) {
                    com.hippo.unifile.UniFile dir = com.hippo.ehviewer.spider.SpiderDen.getGalleryDownloadDir(mGalleryInfo);
                    if (dir != null && dir.exists()) {
                        com.hippo.unifile.UniFile translated = dir.findFile("translated");
                        if (translated != null && translated.exists()) {
                            String page = String.format(java.util.Locale.US, "%08d", mCurrentIndex + 1);
                            com.hippo.unifile.UniFile[] files = translated.listFiles();
                            if (files != null) {
                                for (com.hippo.unifile.UniFile f : files) {
                                    String n = f.getName();
                                    if (n != null && n.contains(page)) { translatedAvail = true; break; }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            // follow auto panel visibility
            int autoVis = mAutoTransferPanel != null ? mAutoTransferPanel.getVisibility() : View.INVISIBLE;
            mTranslateToggle.setVisibility(translatedAvail && autoVis == View.VISIBLE ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView start;
        if (mLayoutMode == GalleryView.LAYOUT_RIGHT_TO_LEFT) {
            start = mRightText;
        } else {
            start = mLeftText;
        }
        if (fromUser && null != start) {
            start.setText(Integer.toString(progress + 1));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        SimpleHandler.getInstance().removeCallbacks(mHideSliderRunnable);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        SimpleHandler.getInstance().postDelayed(mHideSliderRunnable, HIDE_SLIDER_DELAY);
        int progress = seekBar.getProgress();
        if (progress != mCurrentIndex && null != mGalleryView) {
            mGalleryView.setCurrentPage(progress);
        }
    }

    @Override
    public void onUpdateCurrentIndex(int index) {
        if (null != mGalleryProvider) {
            mGalleryProvider.putStartPage(index);
        }

        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_CURRENT_INDEX, index);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onTapSliderArea() {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_TAP_SLIDER_AREA, 0);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onTapMenuArea() {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_TAP_MENU_AREA, 0);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onTapErrorText(int index) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_TAP_ERROR_TEXT, index);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onLongPressPage(int index) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask();
        }
        task.setData(NotifyTask.KEY_LONG_PRESS_PAGE, index);
        SimpleHandler.getInstance().post(task);
    }

    @Override
    public void onAutoTransferDone() {
        if (autoTransferring) {
            autoRead(mAutoTransferPanel);
        }
    }

//    @Override
//    public boolean onGenericMotionEvent(MotionEvent event) {
//        //The input source is a pointing device associated with a display.
//        //输入源为可显示的指针设备，如：mouse pointing device(鼠标指针),stylus pointing device(尖笔设备)
//        if (0 != (event.getSource() & InputDevice.SOURCE_CLASS_POINTER)) {
//            switch (event.getAction()) {
//                // process the scroll wheel movement...处理滚轮事件
//                case MotionEvent.ACTION_SCROLL:
//                    //获得垂直坐标上的滚动方向,也就是滚轮向下滚
//                    if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
//                        Log.i("fortest::onGenericMotionEvent", "down");
//                    }
//                    //获得垂直坐标上的滚动方向,也就是滚轮向上滚
//                    else {
//                        Log.i("fortest::onGenericMotionEvent", "up");
//                    }
//                    return true;
//            }
//        }
//        return super.onGenericMotionEvent(event);
//    }


    private void showSlider(View sliderPanel, ObjectAnimator animator) {
        if (null != mSeekBarPanelAnimator) {
            animator.cancel();
        }
        if (sliderPanel == mAutoTransferPanel) {
            sliderPanel.setTranslationX(sliderPanel.getWidth());
            animator = ObjectAnimator.ofFloat(sliderPanel, "translationX", 0.0f);
        } else {
            sliderPanel.setTranslationY(sliderPanel.getHeight());
            animator = ObjectAnimator.ofFloat(sliderPanel, "translationY", 0.0f);
        }

        sliderPanel.setVisibility(View.VISIBLE);


        animator.setDuration(SLIDER_ANIMATION_DURING);
        animator.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
        animator.addUpdateListener(mUpdateSliderListener);
        animator.addListener(mShowSliderListener);
        animator.start();

        if (null != mSystemUiHelper) {
            mSystemUiHelper.show();
            mShowSystemUi = true;
        }
    }


    private void hideSlider(View sliderPanel, ObjectAnimator animator) {
        if (null != animator) {
            animator.cancel();
        }
        if (sliderPanel == mAutoTransferPanel) {
            animator = ObjectAnimator.ofFloat(sliderPanel, "translationX", sliderPanel.getWidth());
        } else {
            animator = ObjectAnimator.ofFloat(sliderPanel, "translationY", sliderPanel.getHeight());
        }

        animator.setDuration(SLIDER_ANIMATION_DURING);
        animator.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);
        animator.addUpdateListener(mUpdateSliderListener);
        animator.addListener(mHideSliderListener);
        animator.start();

        if (null != mSystemUiHelper) {
            mSystemUiHelper.hide();
            mShowSystemUi = false;
        }
    }

    /**
     * @param lightness 0 - 200
     */
    private void setScreenLightness(boolean enable, int lightness) {
        if (null == mMaskView) {
            return;
        }

        Window w = getWindow();
        WindowManager.LayoutParams lp = w.getAttributes();
        if (enable) {
            lightness = MathUtils.clamp(lightness, 0, 200);
            if (lightness > 100) {
                mMaskView.setColor(0);
                // Avoid BRIGHTNESS_OVERRIDE_OFF,
                // screen may be off when lp.screenBrightness is 0.0f
                lp.screenBrightness = Math.max((lightness - 100) / 100.0f, 0.01f);
            } else {
                mMaskView.setColor(MathUtils.lerp(0xde, 0x00, lightness / 100.0f) << 24);
                lp.screenBrightness = 0.01f;
            }
        } else {
            mMaskView.setColor(0);
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        }
        w.setAttributes(lp);
    }

    private void shareImage(int page) {
        if (null == mGalleryProvider) {
            return;
        }

        File dir = AppConfig.getExternalTempDir();
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_create_temp_file, Toast.LENGTH_SHORT).show();
            return;
        }
        UniFile file;
        if (null == (file = mGalleryProvider.save(page, UniFile.fromFile(dir), mGalleryProvider.getImageFilename(page)))) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }
        String filename = file.getName();
        if (filename == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }


        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(filename));
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "image/jpeg";
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, file.getUri());
        intent.setType(mimeType);

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_image)));
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            Toast.makeText(this, R.string.error_cant_find_activity, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImage(int page) {
        if (null == mGalleryProvider) {
            return;
        }

        File dir = AppConfig.getExternalImageDir();
        if (null == dir) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }
        UniFile file;
        if (null == (file = mGalleryProvider.save(page, UniFile.fromFile(dir), mGalleryProvider.getImageFilename(page)))) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, getString(R.string.image_saved, file.getUri()), Toast.LENGTH_SHORT).show();

        // Sync media store
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, file.getUri()));
    }

    private void saveImageTo(int page) {
        if (null == mGalleryProvider) {
            return;
        }
        File dir = getCacheDir();
        UniFile file;
        if (null == (file = mGalleryProvider.save(page, UniFile.fromFile(dir), mGalleryProvider.getImageFilename(page)))) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }
        String filename = file.getName();
        if (filename == null) {
            Toast.makeText(this, R.string.error_cant_save_image, Toast.LENGTH_SHORT).show();
            return;
        }
        mCacheFileName = filename;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        try {
            startActivityForResult(intent, WRITE_REQUEST_CODE);
//            registerForActivityResult(intent, WRITE_REQUEST_CODE);
//            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::saveImageDats)
//                    .launch(intent);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            Toast.makeText(this, R.string.error_cant_find_activity, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                String filepath = getCacheDir() + "/" + mCacheFileName;
                File cacheFile = new File(filepath);

                InputStream is = null;
                OutputStream os = null;
                ContentResolver resolver = getContentResolver();

                try {
                    is = new FileInputStream(cacheFile);
                    os = resolver.openOutputStream(uri);
                    IOUtils.copy(is, os);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(is);
                    IOUtils.closeQuietly(os);
                }

                cacheFile.delete();

                Toast.makeText(this, getString(R.string.image_saved, uri.getPath()), Toast.LENGTH_SHORT).show();
                // Sync media store
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            }
        }
    }

    private void saveImageDats(ActivityResult result) {
        if (result == null) {
            return;
        }
        if (result.getResultCode() != Activity.RESULT_OK) {
            return;
        }
        Intent resultData = result.getData();
        if (resultData != null) {
            Uri uri = resultData.getData();
            String filepath = getCacheDir() + "/" + mCacheFileName;
            File cacheFile = new File(filepath);

            InputStream is = null;
            OutputStream os = null;
            ContentResolver resolver = getContentResolver();

            try {
                is = new FileInputStream(cacheFile);
                os = resolver.openOutputStream(uri);
                IOUtils.copy(is, os);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
            }

            boolean deleted = cacheFile.delete();
            if (!deleted) {
                cacheFile.deleteOnExit();
            }

            Toast.makeText(this, getString(R.string.image_saved, uri.getPath()), Toast.LENGTH_SHORT).show();
            // Sync media store
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        }
    }


    private void showPageDialog(final int page) {
        Resources resources = GalleryActivity.this.getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(GalleryActivity.this);
        builder.setTitle(resources.getString(R.string.page_menu_title, page + 1));

        final CharSequence[] items;
        items = new CharSequence[]{getString(R.string.page_menu_refresh), getString(R.string.page_menu_share), getString(R.string.page_menu_save), getString(R.string.page_menu_save_to), getString(R.string.page_menu_translate_this_page), getString(R.string.page_menu_toggle_translation), getString(R.string.page_menu_force_translate_gallery), getString(R.string.page_menu_llm_translate_this_page)};
        pageDialogListener(builder, items, page);
        builder.show();
    }

    private void pageDialogListener(AlertDialog.Builder builder, CharSequence[] items, int page) {
        builder.setItems(items, (dialog, which) -> {
            if (mGalleryProvider == null) {
                return;
            }

            switch (which) {
                case 0: // Refresh
                    mGalleryProvider.removeCache(page);
                    mGalleryProvider.forceRequest(page);
                    break;
                case 1: // Share
                    shareImage(page);
                    break;
                case 2: // Save
                    saveImage(page);
                    break;
                case 3: // Save to
                    saveImageTo(page);
                    break;
                case 4:
                    translatePageAsyncForce(page);
                    break;
                case 5:
                    boolean cur = Settings.getBoolean("read_translated_version", false);
                    Settings.putBoolean("read_translated_version", !cur);
                    mGalleryProvider.removeCache(page);
                    mGalleryProvider.forceRequest(page);
                    break;
                case 6:
                    try {
                        DownloadManager dm = EhApplication.getDownloadManager(GalleryActivity.this);
                        DownloadInfo info = dm.getDownloadInfo(mGalleryInfo.gid);
                        if (info == null) info = new DownloadInfo(mGalleryInfo);
                        boolean queued = TranslationQueueManager.getInstance().enqueueForce(info);
                        Toast.makeText(GalleryActivity.this, getString(R.string.added_to_translation_queue), Toast.LENGTH_SHORT).show();
                    } catch (Exception ignored) {}
                    break;
                case 7: {
                    EditTextDialogBuilder promptBuilder = new EditTextDialogBuilder(GalleryActivity.this,
                            GeminiApi.getCommonPrompt(), getString(R.string.gemini_common_prompt));
                    promptBuilder.setTitle(R.string.gemini_common_prompt);
                    promptBuilder.setPositiveButton(android.R.string.ok, null);
                    promptBuilder.setNeutralButton("mask制作", null);
                    AlertDialog promptDialog = promptBuilder.show();
                    Button button = promptDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (button != null) {
                        button.setOnClickListener(v -> {
                            String prompt = promptBuilder.getText();
                            if (TextUtils.isEmpty(prompt)) {
                                promptBuilder.setError(getString(R.string.text_is_empty));
                            } else {
                                promptBuilder.setError(null);
                                promptDialog.dismiss();
                                llmTranslatePageAsync(page, prompt);
                            }
                        });
                    }
                    Button neutral = promptDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                    if (neutral != null) {
                        neutral.setOnClickListener(v -> {
                            showMaskEditor(page);
                        });
                    }
                    break;
                }
                }
            });
    }

    private void translatePageAsync(final int page) {
        new Thread(() -> {
            try {
                if (mGalleryInfo == null) return;
                DownloadManager dm = EhApplication.getDownloadManager(GalleryActivity.this);
                DownloadInfo info = dm.getDownloadInfo(mGalleryInfo.gid);
                if (info == null) {
                    info = new DownloadInfo(mGalleryInfo);
                }
                File tempBase = AppConfig.getExternalTempDir();
                if (tempBase == null) tempBase = AppConfig.getTempDir();
                UniFile tempDir = UniFile.fromFile(new File(tempBase, "TranslateTemp"));
                tempDir.ensureDir();
                String pageNum = String.format(Locale.US, "%08d", page + 1);
                String filename = "page_" + pageNum + ".png";
                UniFile saved = null;
                try {
                    saved = mGalleryProvider != null ? mGalleryProvider.save(page, tempDir, filename) : null;
                } catch (Exception ignored) {}
                String sourcePath = null;
                if (saved != null) {
                    Uri u = saved.getUri();
                    if (UniFile.isFileUri(u)) {
                        sourcePath = new File(u.getPath()).getAbsolutePath();
                    } else {
                        try {
                            File tmp = new File(new File(tempBase, "TranslateTemp"), "submit_" + mGalleryInfo.gid + "_" + (page+1) + ".img");
                            InputStream is = saved.openInputStream();
                            FileOutputStream fos = new FileOutputStream(tmp);
                            try {
                                byte[] buf = new byte[8192];
                                int r;
                                while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                                fos.flush();
                            } finally {
                                try { is.close(); } catch (Exception ignored) {}
                                try { fos.close(); } catch (Exception ignored) {}
                            }
                            sourcePath = tmp.getAbsolutePath();
                        } catch (Exception ignored) {}
                    }
                }
                boolean queued = TranslationQueueManager.getInstance().enqueueSinglePageWithPath(info, page, sourcePath);
                runOnUiThread(() -> {
                    Toast.makeText(GalleryActivity.this,
                            getString(queued ? R.string.added_to_translation_queue : R.string.already_in_translation_queue),
                            Toast.LENGTH_SHORT).show();
                    startTranslateIndicator();
                });
                watchTranslatedFile(page);
            } catch (Exception ignored) {}
        }).start();
    }

    private void translatePageAsyncForce(final int page) {
        new Thread(() -> {
            try {
                if (mGalleryInfo == null) return;
                DownloadManager dm = EhApplication.getDownloadManager(GalleryActivity.this);
                DownloadInfo info = dm.getDownloadInfo(mGalleryInfo.gid);
                if (info == null) {
                    info = new DownloadInfo(mGalleryInfo);
                }
                File tempBase = AppConfig.getExternalTempDir();
                if (tempBase == null) tempBase = AppConfig.getTempDir();
                UniFile tempDir = UniFile.fromFile(new File(tempBase, "TranslateTemp"));
                tempDir.ensureDir();
                String sourcePath = null;
                // Prefer original file name in download dir
                try {
                    UniFile dlDir = SpiderDen.getGalleryDownloadDir(mGalleryInfo);
                    if (dlDir != null && dlDir.exists()) {
                        String pageNum = String.format(Locale.US, "%08d", page + 1);
                        UniFile[] files = dlDir.listFiles();
                        if (files != null) {
                            for (UniFile f : files) {
                                String n = f.getName();
                                if (n == null) continue;
                                if ("translated".equals(n)) continue;
                                if (f.isDirectory()) continue;
                                if (n.contains(pageNum)) {
                                    Uri u = f.getUri();
                                    if (UniFile.isFileUri(u)) {
                                        sourcePath = new File(u.getPath()).getAbsolutePath();
                                    } else {
                                        // Copy to temp with original file name
                                        File tmp = new File(tempDir.getUri().getPath(), n);
                                        InputStream is = f.openInputStream();
                                        FileOutputStream fos = new FileOutputStream(tmp);
                                        try {
                                            byte[] buf = new byte[8192];
                                            int r;
                                            while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                                            fos.flush();
                                        } finally {
                                            try { is.close(); } catch (Exception ignored) {}
                                            try { fos.close(); } catch (Exception ignored) {}
                                        }
                                        sourcePath = tmp.getAbsolutePath();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
                // If not found, fallback to provider save using its own naming
                if (sourcePath == null) {
                    UniFile saved = null;
                    try {
                        String fname = (mGalleryProvider instanceof EhGalleryProvider)
                                ? ((EhGalleryProvider) mGalleryProvider).getImageFilename(page)
                                : String.format(Locale.US, "%08d", page + 1);
                        saved = mGalleryProvider != null ? mGalleryProvider.save(page, tempDir, fname) : null;
                    } catch (Exception ignored) {}
                    if (saved != null) {
                        Uri u = saved.getUri();
                        if (UniFile.isFileUri(u)) {
                            sourcePath = new File(u.getPath()).getAbsolutePath();
                        } else {
                            try {
                                String n = saved.getName();
                                File tmp = new File(tempDir.getUri().getPath(), n);
                                InputStream is = saved.openInputStream();
                                FileOutputStream fos = new FileOutputStream(tmp);
                                try {
                                    byte[] buf = new byte[8192];
                                    int r;
                                    while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                                    fos.flush();
                                } finally {
                                    try { is.close(); } catch (Exception ignored) {}
                                    try { fos.close(); } catch (Exception ignored) {}
                                }
                                sourcePath = tmp.getAbsolutePath();
                            } catch (Exception ignored) {}
                        }
                    }
                }
                TranslationQueueManager.getInstance().enqueueSinglePageForce(info, page, sourcePath);
                runOnUiThread(() -> {
                    Toast.makeText(GalleryActivity.this, getString(R.string.added_to_translation_queue), Toast.LENGTH_SHORT).show();
                    startTranslateIndicator();
                });
                watchTranslatedFile(page);
            } catch (Exception ignored) {}
        }).start();
    }

    private void llmTranslatePageAsync(final int page) {
        new Thread(() -> {
            try {
                if (mGalleryInfo == null) return;
                File tempBase = AppConfig.getExternalTempDir();
                if (tempBase == null) tempBase = AppConfig.getTempDir();
                UniFile tempDir = UniFile.fromFile(new File(tempBase, "TranslateTemp"));
                tempDir.ensureDir();
                String sourcePath = null;
                try {
                    UniFile dlDir = SpiderDen.getGalleryDownloadDir(mGalleryInfo);
                    if (dlDir != null && dlDir.exists()) {
                        String pageNum = String.format(Locale.US, "%08d", page + 1);
                        UniFile[] files = dlDir.listFiles();
                        if (files != null) {
                            for (UniFile f : files) {
                                String n = f.getName();
                                if (n == null) continue;
                                if ("translated".equals(n)) continue;
                                if (f.isDirectory()) continue;
                                if (n.contains(pageNum)) {
                                    Uri u = f.getUri();
                                    if (UniFile.isFileUri(u)) {
                                        sourcePath = new File(u.getPath()).getAbsolutePath();
                                    } else {
                                        File tmp = new File(tempDir.getUri().getPath(), n);
                                        InputStream is = f.openInputStream();
                                        FileOutputStream fos = new FileOutputStream(tmp);
                                        try {
                                            byte[] buf = new byte[8192];
                                            int r;
                                            while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                                            fos.flush();
                                        } finally {
                                            try { is.close(); } catch (Exception ignored) {}
                                            try { fos.close(); } catch (Exception ignored) {}
                                        }
                                        sourcePath = tmp.getAbsolutePath();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
                if (sourcePath == null) {
                    UniFile saved = null;
                    try {
                        String fname = (mGalleryProvider instanceof EhGalleryProvider)
                                ? ((EhGalleryProvider) mGalleryProvider).getImageFilename(page)
                                : String.format(Locale.US, "%08d", page + 1);
                        saved = mGalleryProvider != null ? mGalleryProvider.save(page, tempDir, fname) : null;
                    } catch (Exception ignored) {}
                    if (saved != null) {
                        Uri u = saved.getUri();
                        if (UniFile.isFileUri(u)) {
                            sourcePath = new File(u.getPath()).getAbsolutePath();
                        } else {
                            try {
                                String n = saved.getName();
                                File tmp = new File(tempDir.getUri().getPath(), n);
                                InputStream is = saved.openInputStream();
                                FileOutputStream fos = new FileOutputStream(tmp);
                                try {
                                    byte[] buf = new byte[8192];
                                    int r;
                                    while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                                    fos.flush();
                                } finally {
                                    try { is.close(); } catch (Exception ignored) {}
                                    try { fos.close(); } catch (Exception ignored) {}
                                }
                                sourcePath = tmp.getAbsolutePath();
                            } catch (Exception ignored) {}
                        }
                    }
                }
                if (sourcePath == null) {
                    Log.e("GalleryActivity", "LLM translate: source not found for page=" + (page+1));
                    runOnUiThread(() -> Toast.makeText(GalleryActivity.this, "Failed", Toast.LENGTH_SHORT).show());
                    return;
                }
                File src = new File(sourcePath);
                String targetName = src.getName();
                Log.d("GalleryActivity", "LLM translate: start, page=" + (page+1) + ", src=" + sourcePath + ", name=" + targetName);
                runOnUiThread(this::startTranslateIndicator);
                String prompt = GeminiApi.getCommonPrompt();
                java.util.ArrayList<RectF> regions = mLlmMaskRegions.get(page);
                File maskedSrc = null;
                if (regions != null && !regions.isEmpty()) {
                    maskedSrc = createMaskedSourceFile(src, regions, tempDir);
                    Log.d("GalleryActivity", "LLM translate: using masked input=" + (maskedSrc != null ? maskedSrc.getAbsolutePath() : "null"));
                }
                GeminiApi.generateImageAsync(prompt, maskedSrc != null ? maskedSrc : src, (png, err) -> {
                    if (png != null && err == null) {
                        try {
                            UniFile dir = SpiderDen.getGalleryDownloadDir(mGalleryInfo);
                            if (dir == null) {
                                Log.e("GalleryActivity", "LLM translate: download dir missing");
                                runOnUiThread(() -> { stopTranslateIndicator(); Toast.makeText(GalleryActivity.this, "Failed", Toast.LENGTH_SHORT).show(); });
                                return;
                            }
                            if (!dir.exists()) dir.ensureDir();
                            UniFile translatedDir = dir.findFile("translated");
                            if (translatedDir == null || !translatedDir.exists()) {
                                translatedDir = dir.createDirectory("translated");
                            }
                            String outName = targetName != null ? targetName : String.format(Locale.US, "%08d", page + 1) + ".png";
                            UniFile out = translatedDir.findFile(outName);
                            if (out == null || !out.exists()) out = translatedDir.createFile(outName);
                            // use existing regions variable declared earlier in method
                            if (regions != null && !regions.isEmpty()) {
                                try {
                                    Bitmap orig = BitmapFactory.decodeFile(src.getAbsolutePath());
                                    Bitmap trans = BitmapFactory.decodeByteArray(png, 0, png.length);
                                    if (orig != null && trans != null) {
                                        if (trans.getWidth() != orig.getWidth() || trans.getHeight() != orig.getHeight()) {
                                            trans = Bitmap.createScaledBitmap(trans, orig.getWidth(), orig.getHeight(), true);
                                        }
                                        Bitmap result = Bitmap.createBitmap(orig.getWidth(), orig.getHeight(), Bitmap.Config.ARGB_8888);
                                        Canvas canvas = new Canvas(result);
                                        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                        paint.setFilterBitmap(false);
                                        paint.setDither(false);
                                        canvas.drawBitmap(orig, 0f, 0f, paint);
                                        Paint paintTrans = new Paint(Paint.ANTI_ALIAS_FLAG);
                                        paintTrans.setFilterBitmap(false);
                                        paintTrans.setDither(false);
                                        for (RectF mask : regions) {
                                            int inset = 5;
                                            Rect innerSrcRect = new Rect((int) mask.left + inset, (int) mask.top + inset, (int) mask.right - inset, (int) mask.bottom - inset);
                                            Rect innerDstRect = new Rect((int) mask.left + inset, (int) mask.top + inset, (int) mask.right - inset, (int) mask.bottom - inset);
                                            if (innerSrcRect.width() <= 0 || innerSrcRect.height() <= 0) continue;
                                            Bitmap part = safeCrop(trans, innerSrcRect);
                                            if (part != null) {
                                                Bitmap cleared = makeBlackTransparent(part, 8);
                                                canvas.drawBitmap(cleared, null, innerDstRect, paintTrans);
                                            }
                                        }
                                        OutputStream os = out.openOutputStream();
                                        try {
                                            result.compress(Bitmap.CompressFormat.PNG, 100, os);
                                            os.flush();
                                        } finally {
                                            try { os.close(); } catch (Exception ignored) {}
                                        }
                                    } else {
                                        OutputStream os = out.openOutputStream();
                                        try {
                                            os.write(png);
                                            os.flush();
                                        } finally {
                                            try { os.close(); } catch (Exception ignored) {}
                                        }
                                    }
                                } catch (Exception ex) {
                                    OutputStream os = out.openOutputStream();
                                    try {
                                        os.write(png);
                                        os.flush();
                                    } finally {
                                        try { os.close(); } catch (Exception ignored) {}
                                    }
                                }
                            } else {
                                OutputStream os = out.openOutputStream();
                                try {
                                    os.write(png);
                                    os.flush();
                                } finally {
                                    try { os.close(); } catch (Exception ignored) {}
                                }
                            }
                            Log.d("GalleryActivity", "LLM translate: wrote translated file=" + outName + ", size=" + png.length);
                            runOnUiThread(() -> {
                                stopTranslateIndicator();
                                if (mGalleryProvider != null) {
                                    mGalleryProvider.removeCache(page);
                                    if (mGalleryProvider instanceof EhGalleryProvider) {
                                        ((EhGalleryProvider) mGalleryProvider).forceLoadTranslated(page);
                                    } else {
                                        mGalleryProvider.forceRequest(page);
                                    }
                                }
                                Toast.makeText(GalleryActivity.this, "OK", Toast.LENGTH_SHORT).show();
                                Log.d("GalleryActivity", "LLM translate: refreshed page=" + (page+1));
                            });
                        } catch (Exception ex) {
                            Log.e("GalleryActivity", "LLM translate: write failed page=" + (page+1) + ", ex=" + ex);
                            runOnUiThread(() -> { stopTranslateIndicator(); Toast.makeText(GalleryActivity.this, "Failed", Toast.LENGTH_SHORT).show(); });
                        }
                    } else {
                        Log.e("GalleryActivity", "LLM translate: API error page=" + (page+1) + ", err=" + err);
                        runOnUiThread(() -> { stopTranslateIndicator(); Toast.makeText(GalleryActivity.this, "Failed", Toast.LENGTH_SHORT).show(); });
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void llmTranslatePageAsync(final int page, final String prompt) {
        new Thread(() -> {
            try {
                if (mGalleryInfo == null) return;
                File tempBase = AppConfig.getExternalTempDir();
                if (tempBase == null) tempBase = AppConfig.getTempDir();
                UniFile tempDir = UniFile.fromFile(new File(tempBase, "TranslateTemp"));
                tempDir.ensureDir();
                String sourcePath = null;
                try {
                    UniFile dlDir = SpiderDen.getGalleryDownloadDir(mGalleryInfo);
                    if (dlDir != null && dlDir.exists()) {
                        String pageNum = String.format(Locale.US, "%08d", page + 1);
                        UniFile[] files = dlDir.listFiles();
                        if (files != null) {
                            for (UniFile f : files) {
                                String n = f.getName();
                                if (n == null) continue;
                                if ("translated".equals(n)) continue;
                                if (f.isDirectory()) continue;
                                if (n.contains(pageNum)) {
                                    Uri u = f.getUri();
                                    if (UniFile.isFileUri(u)) {
                                        sourcePath = new File(u.getPath()).getAbsolutePath();
                                    } else {
                                        File tmp = new File(tempDir.getUri().getPath(), n);
                                        InputStream is = f.openInputStream();
                                        FileOutputStream fos = new FileOutputStream(tmp);
                                        try {
                                            byte[] buf = new byte[8192];
                                            int r;
                                            while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                                            fos.flush();
                                        } finally {
                                            try { is.close(); } catch (Exception ignored) {}
                                            try { fos.close(); } catch (Exception ignored) {}
                                        }
                                        sourcePath = tmp.getAbsolutePath();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
                if (sourcePath == null) {
                    UniFile saved = null;
                    try {
                        String fname = (mGalleryProvider instanceof EhGalleryProvider)
                                ? ((EhGalleryProvider) mGalleryProvider).getImageFilename(page)
                                : String.format(Locale.US, "%08d", page + 1);
                        saved = mGalleryProvider != null ? mGalleryProvider.save(page, tempDir, fname) : null;
                    } catch (Exception ignored) {}
                    if (saved != null) {
                        Uri u = saved.getUri();
                        if (UniFile.isFileUri(u)) {
                            sourcePath = new File(u.getPath()).getAbsolutePath();
                        } else {
                            try {
                                String n = saved.getName();
                                File tmp = new File(tempDir.getUri().getPath(), n);
                                InputStream is = saved.openInputStream();
                                FileOutputStream fos = new FileOutputStream(tmp);
                                try {
                                    byte[] buf = new byte[8192];
                                    int r;
                                    while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                                    fos.flush();
                                } finally {
                                    try { is.close(); } catch (Exception ignored) {}
                                    try { fos.close(); } catch (Exception ignored) {}
                                }
                                sourcePath = tmp.getAbsolutePath();
                            } catch (Exception ignored) {}
                        }
                    }
                }
                if (sourcePath == null) {
                    Log.e("GalleryActivity", "LLM translate: source not found for page=" + (page+1));
                    runOnUiThread(() -> Toast.makeText(GalleryActivity.this, "Failed", Toast.LENGTH_SHORT).show());
                    return;
                }
                File src = new File(sourcePath);
                String targetName = src.getName();
                Log.d("GalleryActivity", "LLM translate: start, page=" + (page+1) + ", src=" + sourcePath + ", name=" + targetName);
                runOnUiThread(this::startTranslateIndicator);
                java.util.ArrayList<RectF> regions = mLlmMaskRegions.get(page);
                File maskedSrc = null;
                if (regions != null && !regions.isEmpty()) {
                    maskedSrc = createMaskedSourceFile(src, regions, tempDir);
                }
                GeminiApi.generateImageAsync(prompt, maskedSrc != null ? maskedSrc : src, (png, err) -> {
                    if (png != null && err == null) {
                        try {
                            UniFile dir = SpiderDen.getGalleryDownloadDir(mGalleryInfo);
                            if (dir == null) {
                                Log.e("GalleryActivity", "LLM translate: download dir missing");
                                runOnUiThread(() -> { stopTranslateIndicator(); Toast.makeText(GalleryActivity.this, "Failed", Toast.LENGTH_SHORT).show(); });
                                return;
                            }
                            if (!dir.exists()) dir.ensureDir();
                            UniFile translatedDir = dir.findFile("translated");
                            if (translatedDir == null || !translatedDir.exists()) {
                                translatedDir = dir.createDirectory("translated");
                            }
                            String outName = targetName != null ? targetName : String.format(Locale.US, "%08d", page + 1) + ".png";
                            UniFile out = translatedDir.findFile(outName);
                            if (out == null || !out.exists()) out = translatedDir.createFile(outName);
                            // use existing regions variable declared earlier in method
                            if (regions != null && !regions.isEmpty()) {
                                try {
                                    Bitmap orig = BitmapFactory.decodeFile(src.getAbsolutePath());
                                    Bitmap trans = BitmapFactory.decodeByteArray(png, 0, png.length);
                                    if (orig != null && trans != null) {
                                        if (trans.getWidth() != orig.getWidth() || trans.getHeight() != orig.getHeight()) {
                                            trans = Bitmap.createScaledBitmap(trans, orig.getWidth(), orig.getHeight(), true);
                                        }
                                        Bitmap result = Bitmap.createBitmap(orig.getWidth(), orig.getHeight(), Bitmap.Config.ARGB_8888);
                                        Canvas canvas = new Canvas(result);
                                        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                                        paint.setFilterBitmap(false);
                                        paint.setDither(false);
                                        canvas.drawBitmap(orig, 0f, 0f, paint);
                                        Paint paintTrans = new Paint(Paint.ANTI_ALIAS_FLAG);
                                        paintTrans.setFilterBitmap(false);
                                        paintTrans.setDither(false);
                                        for (RectF mask : regions) {
                                            int inset = 5;
                                            Rect innerSrcRect = new Rect((int) mask.left + inset, (int) mask.top + inset, (int) mask.right - inset, (int) mask.bottom - inset);
                                            Rect innerDstRect = new Rect((int) mask.left + inset, (int) mask.top + inset, (int) mask.right - inset, (int) mask.bottom - inset);
                                            if (innerSrcRect.width() <= 0 || innerSrcRect.height() <= 0) continue;
                                            Bitmap part = safeCrop(trans, innerSrcRect);
                                            if (part != null) {
                                                Bitmap cleared = makeBlackTransparent(part, 8);
                                                canvas.drawBitmap(cleared, null, innerDstRect, paintTrans);
                                            }
                                        }
                                        OutputStream os = out.openOutputStream();
                                        try {
                                            result.compress(Bitmap.CompressFormat.PNG, 100, os);
                                            os.flush();
                                        } finally {
                                            try { os.close(); } catch (Exception ignored) {}
                                        }
                                    } else {
                                        OutputStream os = out.openOutputStream();
                                        try {
                                            os.write(png);
                                            os.flush();
                                        } finally {
                                            try { os.close(); } catch (Exception ignored) {}
                                        }
                                    }
                                } catch (Exception ex) {
                                    OutputStream os = out.openOutputStream();
                                    try {
                                        os.write(png);
                                        os.flush();
                                    } finally {
                                        try { os.close(); } catch (Exception ignored) {}
                                    }
                                }
                            } else {
                                OutputStream os = out.openOutputStream();
                                try {
                                    os.write(png);
                                    os.flush();
                                } finally {
                                    try { os.close(); } catch (Exception ignored) {}
                                }
                            }
                            Log.d("GalleryActivity", "LLM translate: wrote translated file=" + outName + ", size=" + png.length);
                            runOnUiThread(() -> {
                                stopTranslateIndicator();
                                if (mGalleryProvider != null) {
                                    mGalleryProvider.removeCache(page);
                                    if (mGalleryProvider instanceof EhGalleryProvider) {
                                        ((EhGalleryProvider) mGalleryProvider).forceLoadTranslated(page);
                                    } else {
                                        mGalleryProvider.forceRequest(page);
                                    }
                                }
                                Toast.makeText(GalleryActivity.this, "OK", Toast.LENGTH_SHORT).show();
                                Log.d("GalleryActivity", "LLM translate: refreshed page=" + (page+1));
                            });
                        } catch (Exception ex) {
                            Log.e("GalleryActivity", "LLM translate: write failed page=" + (page+1) + ", ex=" + ex);
                            runOnUiThread(() -> { stopTranslateIndicator(); Toast.makeText(GalleryActivity.this, "Failed", Toast.LENGTH_SHORT).show(); });
                        }
                    } else {
                        Log.e("GalleryActivity", "LLM translate: API error page=" + (page+1) + ", err=" + err);
                        runOnUiThread(() -> { stopTranslateIndicator(); Toast.makeText(GalleryActivity.this, "Failed", Toast.LENGTH_SHORT).show(); });
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void startTranslateIndicator() {
        if (mTranslateToggle == null) return;
        SimpleHandler.getInstance().removeCallbacks(mHideSliderRunnable);
        mTranslateToggle.setVisibility(View.VISIBLE);
        if (mSeekBarPanel != null) {
            showSlider(mSeekBarPanel, mSeekBarPanelAnimator);
        }
        if (mAutoTransferPanel != null) {
            showSlider(mAutoTransferPanel, mAutoTransferAnimator);
        }
        if (mTranslateAnimator != null) {
            try { mTranslateAnimator.cancel(); } catch (Exception ignored) {}
        }
        mTranslateAnimator = ObjectAnimator.ofFloat(mTranslateToggle, View.ROTATION, 0f, 360f);
        mTranslateAnimator.setDuration(800);
        mTranslateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        mTranslateAnimator.start();
    }

    private void stopTranslateIndicator() {
        if (mTranslateAnimator != null) {
            try { mTranslateAnimator.cancel(); } catch (Exception ignored) {}
            mTranslateAnimator = null;
        }
        if (mTranslateToggle != null) {
            mTranslateToggle.setRotation(0f);
        }
    }

    private void watchTranslatedFile(final int page) {
        new Thread(() -> {
            try {
                if (mGalleryInfo == null) return;
                UniFile dir = SpiderDen.getGalleryDownloadDir(mGalleryInfo);
                if (dir == null || !dir.exists()) return;
                String pageNum = String.format(Locale.US, "%08d", page + 1);
                for (int i = 0; i < 900; i++) {
                    UniFile translated = dir.findFile("translated");
                    if (translated != null && translated.exists()) {
                        UniFile[] files = translated.listFiles();
                        if (files != null) {
                            for (UniFile f : files) {
                                String n = f.getName();
                                if (n != null && n.contains(pageNum)) {
                                    runOnUiThread(() -> {
                                        stopTranslateIndicator();
                                        if (mGalleryProvider != null) {
                                            mGalleryProvider.removeCache(page);
                                            if (mGalleryProvider instanceof EhGalleryProvider) {
                                                ((EhGalleryProvider) mGalleryProvider).forceLoadTranslated(page);
                                            } else {
                                                mGalleryProvider.forceRequest(page);
                                            }
                                        }
                                        SimpleHandler.getInstance().postDelayed(mHideSliderRunnable, HIDE_SLIDER_DELAY);
                                    });
                                    return;
                                }
                            }
                        }
                    }
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void showMaskEditor(final int page) {
        try {
            if (mGalleryInfo == null) return;
            File tempBase = AppConfig.getExternalTempDir();
            if (tempBase == null) tempBase = AppConfig.getTempDir();
            UniFile tempDir = UniFile.fromFile(new File(tempBase, "TranslateTemp"));
            tempDir.ensureDir();
            String sourcePath = null;
            try {
                UniFile dlDir = SpiderDen.getGalleryDownloadDir(mGalleryInfo);
                if (dlDir != null && dlDir.exists()) {
                    String pageNum = String.format(Locale.US, "%08d", page + 1);
                    UniFile[] files = dlDir.listFiles();
                    if (files != null) {
                        for (UniFile f : files) {
                            String n = f.getName();
                            if (n == null) continue;
                            if ("translated".equals(n)) continue;
                            if (f.isDirectory()) continue;
                            if (n.contains(pageNum)) {
                                Uri u = f.getUri();
                                if (UniFile.isFileUri(u)) {
                                    sourcePath = new File(u.getPath()).getAbsolutePath();
                                } else {
                                    File tmp = new File(tempDir.getUri().getPath(), n);
                                    InputStream is = f.openInputStream();
                                    FileOutputStream fos = new FileOutputStream(tmp);
                                    try {
                                        byte[] buf = new byte[8192];
                                        int r;
                                        while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                                        fos.flush();
                                    } finally {
                                        try { is.close(); } catch (Exception ignored) {}
                                        try { fos.close(); } catch (Exception ignored) {}
                                    }
                                    sourcePath = tmp.getAbsolutePath();
                                }
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            if (sourcePath == null) {
                UniFile saved = null;
                try {
                    String fname = (mGalleryProvider instanceof EhGalleryProvider)
                            ? ((EhGalleryProvider) mGalleryProvider).getImageFilename(page)
                            : String.format(Locale.US, "%08d", page + 1);
                    saved = mGalleryProvider != null ? mGalleryProvider.save(page, tempDir, fname) : null;
                } catch (Exception ignored) {}
                if (saved != null) {
                    Uri u = saved.getUri();
                    if (UniFile.isFileUri(u)) {
                        sourcePath = new File(u.getPath()).getAbsolutePath();
                    } else {
                        try {
                            String n = saved.getName();
                            File tmp = new File(tempDir.getUri().getPath(), n);
                            InputStream is = saved.openInputStream();
                            FileOutputStream fos = new FileOutputStream(tmp);
                            try {
                                byte[] buf = new byte[8192];
                                int r;
                                while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                                fos.flush();
                            } finally {
                                try { is.close(); } catch (Exception ignored) {}
                                try { fos.close(); } catch (Exception ignored) {}
                            }
                            sourcePath = tmp.getAbsolutePath();
                        } catch (Exception ignored) {}
                    }
                }
            }
            if (sourcePath == null) {
                Toast.makeText(GalleryActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeFile(sourcePath);
            if (bitmap == null) {
                Toast.makeText(GalleryActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                return;
            }
            final MaskSelectionView view = new MaskSelectionView(GalleryActivity.this, bitmap);
            java.util.ArrayList<RectF> pre = mLlmMaskRegions.get(page);
            if (pre != null) view.setInitialSelectionsImage(pre);
            AlertDialog.Builder b = new AlertDialog.Builder(GalleryActivity.this);
            b.setTitle("mask制作");
            FrameLayout fl = new FrameLayout(GalleryActivity.this);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            int pad = (int) (getResources().getDisplayMetrics().density * 12);
            fl.setPadding(pad, pad, pad, pad);
            fl.addView(view, lp);
            b.setView(fl);
            b.setPositiveButton(android.R.string.ok, null);
            b.setNegativeButton(android.R.string.cancel, null);
            b.setNeutralButton("清除", null);
            AlertDialog d = b.create();
            d.setOnShowListener(di -> {
                Button ok = d.getButton(DialogInterface.BUTTON_POSITIVE);
                Button clear = d.getButton(DialogInterface.BUTTON_NEUTRAL);
                if (ok != null) {
                    ok.setOnClickListener(v -> {
                        java.util.ArrayList<RectF> selImgs = view.getSelectionsImage();
                        if (selImgs == null || selImgs.isEmpty()) {
                            Toast.makeText(GalleryActivity.this, getString(R.string.text_is_empty), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        mLlmMaskRegions.put(page, selImgs);
                        d.dismiss();
                        showMaskPreview(page);
                    });
                }
                if (clear != null) {
                    clear.setOnClickListener(v -> {
                        mLlmMaskRegions.remove(page);
                        view.clearSelections();
                    });
                }
            });
            d.show();
        } catch (Exception ignored) {}
    }

    private void showMaskPreview(final int page) {
        try {
            File tempBase = AppConfig.getExternalTempDir();
            if (tempBase == null) tempBase = AppConfig.getTempDir();
            UniFile tempDir = UniFile.fromFile(new File(tempBase, "TranslateTemp"));
            tempDir.ensureDir();
            String sourcePath = null;
            try {
                UniFile dlDir = SpiderDen.getGalleryDownloadDir(mGalleryInfo);
                if (dlDir != null && dlDir.exists()) {
                    String pageNum = String.format(Locale.US, "%08d", page + 1);
                    UniFile[] files = dlDir.listFiles();
                    if (files != null) {
                        for (UniFile f : files) {
                            String n = f.getName();
                            if (n == null) continue;
                            if ("translated".equals(n)) continue;
                            if (f.isDirectory()) continue;
                            if (n.contains(pageNum)) {
                                Uri u = f.getUri();
                                if (UniFile.isFileUri(u)) {
                                    sourcePath = new File(u.getPath()).getAbsolutePath();
                                } else {
                                    File tmp = new File(tempDir.getUri().getPath(), n);
                                    InputStream is = f.openInputStream();
                                    FileOutputStream fos = new FileOutputStream(tmp);
                                    try {
                                        byte[] buf = new byte[8192];
                                        int r;
                                        while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                                        fos.flush();
                                    } finally {
                                        try { is.close(); } catch (Exception ignored) {}
                                        try { fos.close(); } catch (Exception ignored) {}
                                    }
                                    sourcePath = tmp.getAbsolutePath();
                                }
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            if (sourcePath == null) return;
            Bitmap srcBmp = BitmapFactory.decodeFile(sourcePath);
            if (srcBmp == null) return;
            java.util.ArrayList<RectF> regions = mLlmMaskRegions.get(page);
            if (regions == null || regions.isEmpty()) return;
            Bitmap preview = Bitmap.createBitmap(srcBmp.getWidth(), srcBmp.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(preview);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            canvas.drawBitmap(srcBmp, 0f, 0f, paint);
            android.graphics.Path overlayPath = new android.graphics.Path();
            overlayPath.addRect(0f, 0f, srcBmp.getWidth(), srcBmp.getHeight(), android.graphics.Path.Direction.CW);
            for (RectF r : regions) {
                android.graphics.Path p = new android.graphics.Path();
                p.addRect(r, android.graphics.Path.Direction.CW);
                overlayPath.op(p, android.graphics.Path.Op.DIFFERENCE);
            }
            Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG);
            overlay.setColor(Color.BLACK);
            overlay.setAlpha(255);
            canvas.drawPath(overlayPath, overlay);

            ImageView iv = new ImageView(GalleryActivity.this);
            iv.setAdjustViewBounds(true);
            iv.setImageBitmap(preview);
            AlertDialog.Builder b = new AlertDialog.Builder(GalleryActivity.this);
            b.setTitle("mask预览");
            b.setView(iv);
            b.setPositiveButton(android.R.string.ok, null);
            b.show();
        } catch (Exception ignored) {}
    }

    private File createOpaqueMaskFile(File src, java.util.ArrayList<RectF> regions, UniFile tempDir) {
        try {
            Bitmap srcBmp = BitmapFactory.decodeFile(src.getAbsolutePath());
            if (srcBmp == null) return null;
            Bitmap mask = Bitmap.createBitmap(srcBmp.getWidth(), srcBmp.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mask);
            Paint black = new Paint(Paint.ANTI_ALIAS_FLAG);
            black.setColor(Color.BLACK);
            black.setAlpha(255);
            canvas.drawRect(0f, 0f, srcBmp.getWidth(), srcBmp.getHeight(), black);
            Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
            white.setColor(Color.WHITE);
            white.setAlpha(255);
            for (RectF r : regions) {
                canvas.drawRect(r, white);
            }
            File out = new File(tempDir.getUri().getPath(), "mask_" + src.getName() + ".png");
            OutputStream os = new java.io.FileOutputStream(out);
            try {
                mask.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
            } finally {
                try { os.close(); } catch (Exception ignored) {}
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private File createMaskedSourceFile(File src, java.util.ArrayList<RectF> regions, UniFile tempDir) {
        try {
            Bitmap srcBmp = BitmapFactory.decodeFile(src.getAbsolutePath());
            if (srcBmp == null) return null;
            Bitmap masked = Bitmap.createBitmap(srcBmp.getWidth(), srcBmp.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(masked);
            Paint black = new Paint(Paint.ANTI_ALIAS_FLAG);
            black.setColor(Color.BLACK);
            black.setAlpha(255);
            canvas.drawRect(0f, 0f, srcBmp.getWidth(), srcBmp.getHeight(), black);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setFilterBitmap(false);
            paint.setDither(false);
            for (RectF r : regions) {
                Rect srcRect = new Rect((int) r.left, (int) r.top, (int) r.right, (int) r.bottom);
                Rect dstRect = new Rect((int) r.left, (int) r.top, (int) r.right, (int) r.bottom);
                canvas.drawBitmap(srcBmp, srcRect, dstRect, paint);
            }
            File out = new File(tempDir.getUri().getPath(), "masked_" + src.getName() + ".png");
            OutputStream os = new java.io.FileOutputStream(out);
            try {
                masked.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
            } finally {
                try { os.close(); } catch (Exception ignored) {}
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap safeCrop(Bitmap bmp, Rect rect) {
        if (bmp == null || rect == null) return null;
        int l = Math.max(0, Math.min(rect.left, bmp.getWidth()));
        int t = Math.max(0, Math.min(rect.top, bmp.getHeight()));
        int r = Math.max(l, Math.min(rect.right, bmp.getWidth()));
        int b = Math.max(t, Math.min(rect.bottom, bmp.getHeight()));
        int w = r - l;
        int h = b - t;
        if (w <= 0 || h <= 0) return null;
        try {
            return Bitmap.createBitmap(bmp, l, t, w, h);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Bitmap makeBlackTransparent(Bitmap bmp, int threshold) {
        if (bmp == null) return null;
        Bitmap out = bmp.getConfig() == Bitmap.Config.ARGB_8888 ? bmp.copy(Bitmap.Config.ARGB_8888, true) : bmp.copy(Bitmap.Config.ARGB_8888, true);
        int w = out.getWidth();
        int h = out.getHeight();
        int[] px = new int[w * h];
        out.getPixels(px, 0, w, 0, 0, w, h);
        int thr = Math.max(0, Math.min(255, threshold));
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int a = (c >>> 24) & 0xff;
            int r = (c >>> 16) & 0xff;
            int g = (c >>> 8) & 0xff;
            int b = c & 0xff;
            if (r <= thr && g <= thr && b <= thr) {
                px[i] = (0 << 24) | (c & 0x00ffffff);
            } else {
                px[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }
    private static class MaskSelectionView extends View {
        private final Bitmap bitmap;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final java.util.ArrayList<RectF> sels = new java.util.ArrayList<>();
        private RectF imageRect = new RectF();
        private RectF baseRect = new RectF();
        private float downX;
        private float downY;
        private int mode = 0;
        private int currentIndex = -1;
        private float scale = 1f;
        private static final float MIN_SCALE = 0.25f;
        private static final float MAX_SCALE = 4f;
        private static final int MODE_CREATE = 1;
        private static final int MODE_MOVE = 2;
        private static final int MODE_RESIZE_LT = 3;
        private static final int MODE_RESIZE_RT = 4;
        private static final int MODE_RESIZE_LB = 5;
        private static final int MODE_RESIZE_RB = 6;
        private static final float HANDLE = 12f;

        public MaskSelectionView(Context ctx, Bitmap bmp) {
            super(ctx);
            this.bitmap = bmp;
            paint.setFilterBitmap(true);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            float bw = bitmap.getWidth();
            float bh = bitmap.getHeight();
            float sw = w / bw;
            float sh = h / bh;
            float s = Math.min(sw, sh);
            float dw = bw * s;
            float dh = bh * s;
            float l = (w - dw) / 2f;
            float t = (h - dh) / 2f;
            baseRect.set(l, t, l + dw, t + dh);
            applyTransform();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Rect dst = new Rect((int) imageRect.left, (int) imageRect.top, (int) imageRect.right, (int) imageRect.bottom);
            canvas.drawBitmap(bitmap, null, dst, paint);
            android.graphics.Path overlayPath = new android.graphics.Path();
            overlayPath.addRect(imageRect, android.graphics.Path.Direction.CW);
            for (RectF r : sels) {
                android.graphics.Path p = new android.graphics.Path();
                p.addRect(r, android.graphics.Path.Direction.CW);
                overlayPath.op(p, android.graphics.Path.Op.DIFFERENCE);
            }
            Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG);
            overlay.setColor(Color.BLACK);
            overlay.setAlpha(160);
            canvas.drawPath(overlayPath, overlay);

            int accent = ResourcesUtils.getAttrColor(getContext(), androidx.appcompat.R.attr.colorPrimary);
            Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
            border.setStyle(Paint.Style.STROKE);
            border.setColor(accent);
            border.setStrokeWidth(2f);
            Paint handle = new Paint(Paint.ANTI_ALIAS_FLAG);
            handle.setStyle(Paint.Style.FILL);
            handle.setColor(accent);
            for (RectF r : sels) {
                canvas.drawRect(r, border);
                RectF inner = new RectF(r.left + 5f, r.top + 5f, r.right - 5f, r.bottom - 5f);
                if (inner.right > inner.left && inner.bottom > inner.top) {
                    canvas.drawRect(inner, border);
                }
                canvas.drawRect(r.left - HANDLE, r.top - HANDLE, r.left + HANDLE, r.top + HANDLE, handle);
                canvas.drawRect(r.right - HANDLE, r.top - HANDLE, r.right + HANDLE, r.top + HANDLE, handle);
                canvas.drawRect(r.left - HANDLE, r.bottom - HANDLE, r.left + HANDLE, r.bottom + HANDLE, handle);
                canvas.drawRect(r.right - HANDLE, r.bottom - HANDLE, r.right + HANDLE, r.bottom + HANDLE, handle);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = x; downY = y;
                    mode = pickMode(x, y);
                    if (mode == 0 && imageRect.contains(x, y)) {
                        RectF r = new RectF(x, y, x, y);
                        sels.add(r);
                        currentIndex = sels.size() - 1;
                        mode = MODE_CREATE;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (currentIndex >= 0 && currentIndex < sels.size()) {
                        RectF sel = sels.get(currentIndex);
                        if (mode == MODE_CREATE) {
                            sel.set(Math.min(downX, x), Math.min(downY, y), Math.max(downX, x), Math.max(downY, y));
                            clampSel(sel);
                            snapRect(sel);
                            invalidate();
                        } else if (mode == MODE_MOVE) {
                            float dx = x - downX;
                            float dy = y - downY;
                            sel.offset(dx, dy);
                            clampSel(sel);
                            snapRect(sel);
                            downX = x; downY = y;
                            invalidate();
                        } else {
                            if (mode == MODE_RESIZE_LT) {
                                sel.left = x; sel.top = y;
                            } else if (mode == MODE_RESIZE_RT) {
                                sel.right = x; sel.top = y;
                            } else if (mode == MODE_RESIZE_LB) {
                                sel.left = x; sel.bottom = y;
                            } else if (mode == MODE_RESIZE_RB) {
                                sel.right = x; sel.bottom = y;
                            }
                            clampSel(sel);
                            snapRect(sel);
                            invalidate();
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mode = 0;
                    break;
            }
            return true;
        }

        private int pickMode(float x, float y) {
            for (int i = sels.size() - 1; i >= 0; i--) {
                RectF sel = sels.get(i);
                RectF lt = new RectF(sel.left - HANDLE, sel.top - HANDLE, sel.left + HANDLE, sel.top + HANDLE);
                RectF rt = new RectF(sel.right - HANDLE, sel.top - HANDLE, sel.right + HANDLE, sel.top + HANDLE);
                RectF lb = new RectF(sel.left - HANDLE, sel.bottom - HANDLE, sel.left + HANDLE, sel.bottom + HANDLE);
                RectF rb = new RectF(sel.right - HANDLE, sel.bottom - HANDLE, sel.right + HANDLE, sel.bottom + HANDLE);
                if (lt.contains(x, y)) { currentIndex = i; return MODE_RESIZE_LT; }
                if (rt.contains(x, y)) { currentIndex = i; return MODE_RESIZE_RT; }
                if (lb.contains(x, y)) { currentIndex = i; return MODE_RESIZE_LB; }
                if (rb.contains(x, y)) { currentIndex = i; return MODE_RESIZE_RB; }
                if (sel.contains(x, y)) { currentIndex = i; return MODE_MOVE; }
            }
            currentIndex = -1;
            return 0;
        }

        private void clampSel(RectF sel) {
            if (sel.left < imageRect.left) sel.left = imageRect.left;
            if (sel.top < imageRect.top) sel.top = imageRect.top;
            if (sel.right > imageRect.right) sel.right = imageRect.right;
            if (sel.bottom > imageRect.bottom) sel.bottom = imageRect.bottom;
        }

        public java.util.ArrayList<RectF> getSelectionsImage() {
            java.util.ArrayList<RectF> list = new java.util.ArrayList<>();
            float bw = bitmap.getWidth();
            float bh = bitmap.getHeight();
            float sx = bw / imageRect.width();
            float sy = bh / imageRect.height();
            for (RectF sel : sels) {
                int l = Math.round((sel.left - imageRect.left) * sx);
                int t = Math.round((sel.top - imageRect.top) * sy);
                int r = Math.round((sel.right - imageRect.left) * sx);
                int b = Math.round((sel.bottom - imageRect.top) * sy);
                list.add(new RectF(l, t, r, b));
            }
            return list;
        }

        public void setInitialSelectionsImage(java.util.ArrayList<RectF> imgRects) {
            float bw = bitmap.getWidth();
            float bh = bitmap.getHeight();
            float sx = imageRect.width() / bw;
            float sy = imageRect.height() / bh;
            sels.clear();
            if (imgRects != null) {
                for (RectF imgRect : imgRects) {
                    float l = imageRect.left + imgRect.left * sx;
                    float t = imageRect.top + imgRect.top * sy;
                    float r = imageRect.left + imgRect.right * sx;
                    float b = imageRect.top + imgRect.bottom * sy;
                    RectF vf = new RectF(l, t, r, b);
                    snapRect(vf);
                    sels.add(vf);
                }
            }
            invalidate();
        }

        public void clearSelections() {
            sels.clear();
            invalidate();
        }

        private void applyTransform() {
            float cx = baseRect.centerX();
            float cy = baseRect.centerY();
            float w = baseRect.width() * scale;
            float h = baseRect.height() * scale;
            imageRect.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
        }

        private void snapRect(RectF sel) {
            sel.left = Math.round(sel.left);
            sel.top = Math.round(sel.top);
            sel.right = Math.round(sel.right);
            sel.bottom = Math.round(sel.bottom);
        }

        @Override
        public boolean onGenericMotionEvent(MotionEvent event) {
            if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0 && event.getAction() == MotionEvent.ACTION_SCROLL) {
                float v = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                if (v != 0f) {
                    float old = scale;
                    scale *= (1f + (v > 0 ? -0.1f : 0.1f));
                    if (scale < MIN_SCALE) scale = MIN_SCALE;
                    if (scale > MAX_SCALE) scale = MAX_SCALE;
                    if (Math.abs(scale - old) > 0.0001f) {
                        applyTransform();
                        invalidate();
                        return true;
                    }
                }
            }
            return super.onGenericMotionEvent(event);
        }
    }

    private class GalleryMenuHelper implements DialogInterface.OnClickListener {

        private final View mView;
        private final Spinner mScreenRotation;
        private final Spinner mReadingDirection;
        private final Spinner mScaleMode;
        private final Spinner mStartPosition;
        private final SeekBar mStartTransferTime;
        private final SwitchCompat mKeepScreenOn;
        private final SwitchCompat mShowClock;
        private final SwitchCompat mShowProgress;
        private final SwitchCompat mShowBattery;
        private final SwitchCompat mShowPageInterval;
        private final SwitchCompat mVolumePage;
        private final SwitchCompat mReverseVolumePage;
        private final SwitchCompat mReadingFullscreen;
        private final SwitchCompat mCustomScreenLightness;
        private final SeekBar mScreenLightness;

        @SuppressLint("InflateParams")
        public GalleryMenuHelper(Context context) {
            mView = LayoutInflater.from(context).inflate(R.layout.dialog_gallery_menu, null);
            mScreenRotation = mView.findViewById(R.id.screen_rotation);
            mReadingDirection = mView.findViewById(R.id.reading_direction);
            mScaleMode = mView.findViewById(R.id.page_scaling);
            mStartPosition = mView.findViewById(R.id.start_position);
            mStartTransferTime = mView.findViewById(R.id.start_transfer_time);
            mKeepScreenOn = mView.findViewById(R.id.keep_screen_on);
            mShowClock = mView.findViewById(R.id.show_clock);
            mShowProgress = mView.findViewById(R.id.show_progress);
            mShowBattery = mView.findViewById(R.id.show_battery);
            mShowPageInterval = mView.findViewById(R.id.show_page_interval);
            mVolumePage = mView.findViewById(R.id.volume_page);
            mReverseVolumePage = mView.findViewById(R.id.reverse_volume_page);
            mReadingFullscreen = mView.findViewById(R.id.reading_fullscreen);
            mCustomScreenLightness = mView.findViewById(R.id.custom_screen_lightness);
            mScreenLightness = mView.findViewById(R.id.screen_lightness);

            mScreenRotation.setSelection(Settings.getScreenRotation());
            mReadingDirection.setSelection(Settings.getReadingDirection());
            mScaleMode.setSelection(Settings.getPageScaling());
            mStartPosition.setSelection(Settings.getStartPosition());
            mStartTransferTime.setProgress(Settings.getStartTransferTime());
            mKeepScreenOn.setChecked(Settings.getKeepScreenOn());
            mShowClock.setChecked(Settings.getShowClock());
            mShowProgress.setChecked(Settings.getShowProgress());
            mShowBattery.setChecked(Settings.getShowBattery());
            mShowPageInterval.setChecked(Settings.getShowPageInterval());
            mVolumePage.setChecked(Settings.getVolumePage());
            mReverseVolumePage.setChecked(Settings.getReverseVolumePage());
            mReadingFullscreen.setChecked(Settings.getReadingFullscreen());
            mCustomScreenLightness.setChecked(Settings.getCustomScreenLightness());
            mScreenLightness.setProgress(Settings.getScreenLightness());
            mScreenLightness.setEnabled(Settings.getCustomScreenLightness());

            mVolumePage.setOnCheckedChangeListener(this::onVolumePageChange);

            if (Settings.getVolumePage()) {
                mReverseVolumePage.setVisibility(View.VISIBLE);

            } else {
                mReverseVolumePage.setVisibility(View.GONE);
            }

            mCustomScreenLightness.setOnCheckedChangeListener((buttonView, isChecked) -> mScreenLightness.setEnabled(isChecked));
        }

        private void onVolumePageChange(CompoundButton compoundButton, boolean b) {
            if (compoundButton.isChecked()) {
                mReverseVolumePage.setVisibility(View.VISIBLE);
            } else {
                mReverseVolumePage.setVisibility(View.GONE);
            }
        }

        public View getView() {
            return mView;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mGalleryView == null) {
                return;
            }

            int screenRotation = mScreenRotation.getSelectedItemPosition();
            int layoutMode = GalleryView.sanitizeLayoutMode(mReadingDirection.getSelectedItemPosition());
            int scaleMode = GalleryView.sanitizeScaleMode(mScaleMode.getSelectedItemPosition());
            int startPosition = GalleryView.sanitizeStartPosition(mStartPosition.getSelectedItemPosition());
            boolean keepScreenOn = mKeepScreenOn.isChecked();
            boolean showClock = mShowClock.isChecked();
            boolean showProgress = mShowProgress.isChecked();
            boolean showBattery = mShowBattery.isChecked();
            boolean showPageInterval = mShowPageInterval.isChecked();
            boolean volumePage = mVolumePage.isChecked();
            boolean reverseVolumePage = mReverseVolumePage.isChecked();
            boolean readingFullscreen = mReadingFullscreen.isChecked();
            boolean customScreenLightness = mCustomScreenLightness.isChecked();

            int screenLightness = mScreenLightness.getProgress();
            int transferTime = mStartTransferTime.getProgress();

            boolean oldReadingFullscreen = Settings.getReadingFullscreen();

            Settings.putScreenRotation(screenRotation);
            Settings.putReadingDirection(layoutMode);
            Settings.putPageScaling(scaleMode);
            Settings.putStartPosition(startPosition);
            Settings.putStartTransferTime(transferTime);
            Settings.putKeepScreenOn(keepScreenOn);
            Settings.putShowClock(showClock);
            Settings.putShowProgress(showProgress);
            Settings.putShowBattery(showBattery);
            Settings.putShowPageInterval(showPageInterval);
            Settings.putVolumePage(volumePage);
            Settings.putReadingFullscreen(readingFullscreen);
            Settings.putCustomScreenLightness(customScreenLightness);
            Settings.putScreenLightness(screenLightness);
            Settings.putReverseVolumePage(reverseVolumePage);
            if (!volumePage) {
                mReverseVolumePage.setVisibility(View.GONE);
            } else {
                mReverseVolumePage.setVisibility(View.VISIBLE);
            }

            int orientation;
            switch (screenRotation) {
                default:
                case 0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                    break;
                case 1:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
                    break;
                case 2:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                    break;
                case 3:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
                    break;
            }
            setRequestedOrientation(orientation);
            mGalleryView.setLayoutMode(layoutMode);
            mGalleryView.setScaleMode(scaleMode);
            mGalleryView.setStartPosition(startPosition);
            if (keepScreenOn) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            if (mClock != null) {
                mClock.setVisibility(showClock ? View.VISIBLE : View.GONE);
            }
            if (mProgress != null) {
                mProgress.setVisibility(showProgress ? View.VISIBLE : View.GONE);
            }
            if (mBattery != null) {
                mBattery.setVisibility(showBattery ? View.VISIBLE : View.GONE);
            }
            mGalleryView.setPagerInterval(showPageInterval ? getResources().getDimensionPixelOffset(R.dimen.gallery_pager_interval) : 0);
            mGalleryView.setScrollInterval(showPageInterval ? getResources().getDimensionPixelOffset(R.dimen.gallery_scroll_interval) : 0);
            setScreenLightness(customScreenLightness, screenLightness);

            // Update slider
            mLayoutMode = layoutMode;
            updateSlider();

            if (oldReadingFullscreen != readingFullscreen) {
                recreate();
            }
        }
    }

    private class NotifyTask implements Runnable {

        public static final int KEY_LAYOUT_MODE = 0;
        public static final int KEY_SIZE = 1;
        public static final int KEY_CURRENT_INDEX = 2;
        public static final int KEY_TAP_SLIDER_AREA = 3;
        public static final int KEY_TAP_MENU_AREA = 4;
        public static final int KEY_TAP_ERROR_TEXT = 5;
        public static final int KEY_LONG_PRESS_PAGE = 6;

        private int mKey;
        private int mValue;

        public void setData(int key, int value) {
            mKey = key;
            mValue = value;
        }

        private void onTapMenuArea() {
            AlertDialog.Builder builder = new AlertDialog.Builder(GalleryActivity.this);
            GalleryMenuHelper helper = new GalleryMenuHelper(builder.getContext());
            builder.setTitle(R.string.gallery_menu_title).setView(helper.getView()).setPositiveButton(android.R.string.ok, helper).show();
        }

        private void onTapSliderArea() {
            if (mSeekBarPanel == null || mSize <= 0 || mCurrentIndex < 0 || mAutoTransferPanel == null) {
                return;
            }

            SimpleHandler.getInstance().removeCallbacks(mHideSliderRunnable);

            if (mSeekBarPanel.getVisibility() == View.VISIBLE) {
                hideSlider(mSeekBarPanel, mSeekBarPanelAnimator);
                hideSlider(mAutoTransferPanel, mAutoTransferAnimator);
            } else {
                showSlider(mSeekBarPanel, mSeekBarPanelAnimator);
                showSlider(mAutoTransferPanel, mAutoTransferAnimator);
                SimpleHandler.getInstance().postDelayed(mHideSliderRunnable, HIDE_SLIDER_DELAY);
            }
        }

        private void onTapErrorText(int index) {
            if (mGalleryProvider != null) {
                mGalleryProvider.forceRequest(index);
            }
        }

        private void onLongPressPage(final int index) {
            showPageDialog(index);
        }

        @Override
        public void run() {
            switch (mKey) {
                case KEY_LAYOUT_MODE:
                    GalleryActivity.this.mLayoutMode = mValue;
                    updateSlider();
                    break;
                case KEY_SIZE:
                    GalleryActivity.this.mSize = mValue;
                    updateSlider();
                    updateProgress();
                    break;
                case KEY_CURRENT_INDEX:
                    GalleryActivity.this.mCurrentIndex = mValue;
                    updateSlider();
                    updateProgress();
                    break;
                case KEY_TAP_MENU_AREA:
                    onTapMenuArea();
                    break;
                case KEY_TAP_SLIDER_AREA:
                    onTapSliderArea();
                    break;
                case KEY_TAP_ERROR_TEXT:
                    onTapErrorText(mValue);
                    break;
                case KEY_LONG_PRESS_PAGE:
                    onLongPressPage(mValue);
                    break;
            }
            mNotifyTaskPool.push(this);
        }
    }

    private class GalleryAdapter extends SimpleAdapter {

        public GalleryAdapter(@NonNull GLRootView glRootView, @NonNull GalleryProvider provider) {
            super(glRootView, provider);
        }

        @Override
        public void onDataChanged() {
            super.onDataChanged();

            if (mGalleryProvider != null) {
                int size = mGalleryProvider.size();
                NotifyTask task = mNotifyTaskPool.pop();
                if (task == null) {
                    task = new NotifyTask();
                }
                task.setData(NotifyTask.KEY_SIZE, size);
                SimpleHandler.getInstance().post(task);
            }
        }
    }

}
