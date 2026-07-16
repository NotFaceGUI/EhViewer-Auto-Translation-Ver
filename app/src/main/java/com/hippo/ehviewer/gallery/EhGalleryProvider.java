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

package com.hippo.ehviewer.gallery;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.spider.SpiderQueen;
import com.hippo.ehviewer.spider.SpiderDen;
import com.hippo.lib.glgallery.GalleryProvider;
import android.util.SparseBooleanArray;
import java.util.HashMap;
import com.hippo.lib.image.Image;
//import com.hippo.lib.image.Image1;
import com.hippo.unifile.UniFile;
import com.hippo.lib.yorozuya.SimpleHandler;
import java.util.Locale;
import java.io.InputStream;
import java.io.FileInputStream;

public class EhGalleryProvider extends GalleryProvider2 implements SpiderQueen.OnSpiderListener {

    private final Context mContext;
    private final GalleryInfo mGalleryInfo;
    @Nullable
    private SpiderQueen mSpiderQueen;
    private final SparseBooleanArray mIsTranslated = new SparseBooleanArray();
    private final HashMap<Integer, String> mLastPath = new HashMap<>();

    public EhGalleryProvider(Context context, GalleryInfo galleryInfo) {
        mContext = context;
        mGalleryInfo = galleryInfo;
    }

    @Override
    public void start() {
        super.start();

        mSpiderQueen = SpiderQueen.obtainSpiderQueen(mContext, mGalleryInfo, SpiderQueen.MODE_READ);
        mSpiderQueen.addOnSpiderListener(this);
        mIsTranslated.clear();
        mLastPath.clear();
    }

    @Override
    public void stop() {
        super.stop();

        if (mSpiderQueen != null) {
            mSpiderQueen.removeOnSpiderListener(this);
            // Activity recreate may called, so wait 3000s
            SimpleHandler.getInstance().postDelayed(new ReleaseTask(mSpiderQueen), 3000);
            mSpiderQueen = null;
        }
    }

    @Override
    public int getStartPage() {
        if (mSpiderQueen != null) {
            return mSpiderQueen.getStartPage();
        } else {
            return super.getStartPage();
        }
    }

    @NonNull
    @Override
    public String getImageFilename(int index) {
        return String.format(Locale.US, "%d-%s-%08d", mGalleryInfo.gid, mGalleryInfo.token, index + 1);
    }

    @Override
    public boolean save(int index, @NonNull UniFile file) {
        if (null != mSpiderQueen) {
            return mSpiderQueen.save(index, file);
        } else {
            return false;
        }
    }

    @Nullable
    @Override
    public UniFile save(int index, @NonNull UniFile dir, @NonNull String filename) {
        if (null != mSpiderQueen) {
            return mSpiderQueen.save(index, dir, filename);
        } else {
            return null;
        }
    }

    @Override
    public void putStartPage(int page) {
        if (mSpiderQueen != null) {
            mSpiderQueen.putStartPage(page);
        }
    }

    @Override
    public int size() {
        if (mSpiderQueen != null) {
            return mSpiderQueen.size();
        } else {
            return GalleryProvider.STATE_ERROR;
        }
    }

    @Override
    protected void onRequest(int index) {
        if (Settings.getBoolean("read_translated_version", true)) {
            if (tryLoadTranslated(index)) return;
        } else {
            if (tryLoadOriginal(index)) return;
        }
        if (mSpiderQueen != null) {
            Object object = mSpiderQueen.request(index);
            if (object instanceof Float) {
                notifyPagePercent(index, (Float) object);
            } else if (object instanceof String) {
                notifyPageFailed(index, (String) object);
            } else if (object == null) {
                notifyPageWait(index);
            }
        }
    }

    @Override
    protected void onForceRequest(int index) {
        if (Settings.getBoolean("read_translated_version", true)) {
            if (tryLoadTranslated(index)) return;
        } else {
            if (tryLoadOriginal(index)) return;
        }
        if (mSpiderQueen != null) {
            Object object = mSpiderQueen.forceRequest(index);
            if (object instanceof Float) {
                notifyPagePercent(index, (Float) object);
            } else if (object instanceof String) {
                notifyPageFailed(index, (String) object);
            } else if (object == null) {
                notifyPageWait(index);
            }
        }
    }

    public boolean forceLoadTranslated(int index) {
        return tryLoadTranslated(index);
    }

    private boolean tryLoadTranslated(int index) {
        InputStream is = null;
        try {
            UniFile dir = SpiderDen.getGalleryDownloadDir(mGalleryInfo);
            if (dir == null || !dir.exists()) return false;
            UniFile translated = dir.findFile("translated");
            if (translated == null || !translated.exists()) return false;
            String pageNumber = String.format(Locale.US, "%08d", index + 1);
            UniFile[] files = translated.listFiles();
            if (files == null) return false;
            UniFile target = null;
            for (UniFile f : files) {
                String n = f.getName();
                if (n != null && n.contains(pageNumber)) { target = f; break; }
            }
            if (target == null) return false;
            String p = target.getUri() != null ? target.getUri().getPath() : null;
            is = target.openInputStream();
            Image image;
            if (is instanceof FileInputStream) {
                image = Image.decode((FileInputStream) is, false);
            } else {
                java.io.File tmp = java.io.File.createTempFile("translated_" + mGalleryInfo.gid + "_" + (index+1) + "_", ".img");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                    fos.flush();
                } finally {
                    try { fos.close(); } catch (Exception ignored) {}
                    try { if (is != null) is.close(); } catch (Exception ignored) {}
                }
                java.io.FileInputStream fis = new java.io.FileInputStream(tmp);
                try {
                    image = Image.decode(fis, false);
                } finally {
                    try { fis.close(); } catch (Exception ignored) {}
                    try { tmp.delete(); } catch (Exception ignored) {}
                }
            }
            if (image != null) {
                mIsTranslated.put(index, true);
                if (p != null) mLastPath.put(index, p);
                notifyPageSucceed(index, image);
                return true;
            }
        } catch (Throwable ignored) {
        } finally {
            try { if (is != null) is.close(); } catch (Exception ignored) {}
        }
        return false;
    }

    private boolean tryLoadOriginal(int index) {
        InputStream is = null;
        try {
            UniFile dir = SpiderDen.getGalleryDownloadDir(mGalleryInfo);
            if (dir == null || !dir.exists()) return false;
            String pageNumber = String.format(Locale.US, "%08d", index + 1);
            UniFile[] files = dir.listFiles();
            if (files == null) return false;
            UniFile target = null;
            for (UniFile f : files) {
                String n = f.getName();
                if (n == null) continue;
                if ("translated".equals(n)) continue;
                if (f.isDirectory()) continue;
                if (n.contains(pageNumber)) { target = f; break; }
            }
            if (target == null) return false;
            String p = target.getUri() != null ? target.getUri().getPath() : null;
            is = target.openInputStream();
            Image image;
            if (is instanceof FileInputStream) {
                image = Image.decode((FileInputStream) is, false);
            } else {
                java.io.File tmp = java.io.File.createTempFile("original_" + mGalleryInfo.gid + "_" + (index+1) + "_", ".img");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
                try {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                    fos.flush();
                } finally {
                    try { fos.close(); } catch (Exception ignored) {}
                    try { if (is != null) is.close(); } catch (Exception ignored) {}
                }
                java.io.FileInputStream fis = new java.io.FileInputStream(tmp);
                try {
                    image = Image.decode(fis, false);
                } finally {
                    try { fis.close(); } catch (Exception ignored) {}
                    try { tmp.delete(); } catch (Exception ignored) {}
                }
            }
            if (image != null) {
                mIsTranslated.put(index, false);
                if (p != null) mLastPath.put(index, p);
                notifyPageSucceed(index, image);
                return true;
            }
        } catch (Throwable ignored) {
        } finally {
            try { if (is != null) is.close(); } catch (Exception ignored) {}
        }
        return false;
    }

    @Override
    protected void onCancelRequest(int index) {
        if (mSpiderQueen != null) {
            mSpiderQueen.cancelRequest(index);
        }
    }

    @Override
    public String getError() {
        if (mSpiderQueen != null) {
            return mSpiderQueen.getError();
        } else {
            return "Error"; // TODO
        }
    }

    @Override
    public void onGetPages(int pages) {
        notifyDataChanged();
    }

    @Override
    public void onGet509(int index) {
        // TODO
    }

    @Override
    public void onPageDownload(int index, long contentLength, long receivedSize, int bytesRead) {
        if (contentLength > 0) {
            notifyPagePercent(index, (float) receivedSize / contentLength);
        }
    }

    @Override
    public void onPageSuccess(int index, int finished, int downloaded, int total) {
        notifyDataChanged(index);
    }

    @Override
    public void onPageFailure(int index, String error, int finished, int downloaded, int total) {
        notifyPageFailed(index, error);
    }

    @Override
    public void onFinish(int finished, int downloaded, int total) {
    }

    @Override
    public void onGetImageSuccess(int index, Image image) {
        mIsTranslated.put(index, false);
        mLastPath.remove(index);
        notifyPageSucceed(index, image);
    }

    @Override
    public void onGetImageFailure(int index, String error) {
        notifyPageFailed(index, error);
    }

    public boolean isTranslatedDisplayed(int index) {
        return mIsTranslated.get(index, false);
    }

    public String getLastLoadedPath(int index) {
        return mLastPath.get(index);
    }

    private static class ReleaseTask implements Runnable {

        private SpiderQueen mSpiderQueen;

        public ReleaseTask(SpiderQueen spiderQueen) {
            mSpiderQueen = spiderQueen;
        }

        @Override
        public void run() {
            if (null != mSpiderQueen) {
                SpiderQueen.releaseSpiderQueen(mSpiderQueen, SpiderQueen.MODE_READ);
                mSpiderQueen = null;
            }
        }
    }
}
