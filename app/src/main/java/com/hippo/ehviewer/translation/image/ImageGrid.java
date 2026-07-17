package com.hippo.ehviewer.translation.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageGrid {

    private static final int GAP = 2;

    public static class GridLayout {
        public int canvasW;
        public int canvasH;
        public List<Rect> cellRects = new ArrayList<>();
    }

    public static GridLayout planLayout(List<Integer> widths, List<Integer> heights, int count, int cols) {
        GridLayout layout = new GridLayout();
        if (count <= 0) return layout;
        if (cols <= 0) cols = 1;
        if (cols > count) cols = count;

        int[] colWidths = new int[cols];
        int[] colHeights = new int[cols];
        List<List<Integer>> colPages = new ArrayList<>();
        for (int c = 0; c < cols; c++) colPages.add(new ArrayList<>());

        int[] colH = new int[cols];
        for (int i = 0; i < count; i++) {
            int w = i < widths.size() ? Math.max(1, widths.get(i)) : 1024;
            int h = i < heights.size() ? Math.max(1, heights.get(i)) : 1448;
            int bestCol = 0;
            for (int c = 1; c < cols; c++) {
                if (colH[c] < colH[bestCol]) bestCol = c;
            }
            colPages.get(bestCol).add(i);
            if (w > colWidths[bestCol]) colWidths[bestCol] = w;
            colH[bestCol] += h + GAP;
        }

        for (int c = 0; c < cols; c++) colHeights[c] = Math.max(0, colH[c] - GAP);

        int canvasW = GAP;
        for (int c = 0; c < cols; c++) canvasW += colWidths[c] + GAP;
        int canvasH = GAP;
        int maxCH = 0;
        for (int c = 0; c < cols; c++) if (colHeights[c] > maxCH) maxCH = colHeights[c];
        canvasH += maxCH + GAP;

        layout.canvasW = canvasW;
        layout.canvasH = canvasH;
        layout.cellRects = new ArrayList<>(count);
        for (int i = 0; i < count; i++) layout.cellRects.add(null);

        int[] curY = new int[cols];
        for (int c = 0; c < cols; c++) curY[c] = GAP;
        int[] colX = new int[cols];
        colX[0] = GAP;
        for (int c = 1; c < cols; c++) colX[c] = colX[c-1] + colWidths[c-1] + GAP;

        for (int c = 0; c < cols; c++) {
            for (int pageIdx : colPages.get(c)) {
                int w = pageIdx < widths.size() ? Math.max(1, widths.get(pageIdx)) : 1024;
                int h = pageIdx < heights.size() ? Math.max(1, heights.get(pageIdx)) : 1448;
                int x = colX[c];
                int y = curY[c];
                Rect rect = new Rect(x, y, x + w, y + h);
                layout.cellRects.set(pageIdx, rect);
                curY[c] += h + GAP;
            }
        }

        return layout;
    }

    public static byte[] compose(List<File> images, GridLayout layout) {
        int n = Math.min(images.size(), layout.cellRects.size());
        Bitmap canvas = Bitmap.createBitmap(layout.canvasW, layout.canvasH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(canvas);
        Paint bg = new Paint();
        bg.setColor(Color.WHITE);
        c.drawRect(0, 0, layout.canvasW, layout.canvasH, bg);

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setFilterBitmap(true);

        for (int i = 0; i < n; i++) {
            Rect dst = layout.cellRects.get(i);
            if (dst == null) continue;
            Bitmap bmp = decodeBitmap(images.get(i), dst.width(), dst.height());
            if (bmp == null) continue;
            int bw = bmp.getWidth();
            int bh = bmp.getHeight();
            int dx = dst.left + (dst.width() - bw) / 2;
            int dy = dst.top + (dst.height() - bh) / 2;
            c.drawBitmap(bmp, new Rect(0, 0, bw, bh), new Rect(dx, dy, dx + bw, dy + bh), paint);
            bmp.recycle();
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        canvas.compress(Bitmap.CompressFormat.PNG, 100, bos);
        canvas.recycle();
        return bos.toByteArray();
    }

    public static List<byte[]> split(byte[] bigImage, GridLayout layout) {
        List<byte[]> result = new ArrayList<>();
        Bitmap big = BitmapFactory.decodeByteArray(bigImage, 0, bigImage.length);
        if (big == null) return result;
        for (int i = 0; i < layout.cellRects.size(); i++) {
            Rect cell = layout.cellRects.get(i);
            if (cell == null) { result.add(null); continue; }
            Rect src = new Rect(
                    Math.max(0, cell.left), Math.max(0, cell.top),
                    Math.min(big.getWidth(), cell.right), Math.min(big.getHeight(), cell.bottom));
            if (src.width() <= 0 || src.height() <= 0) { result.add(null); continue; }
            Bitmap cellBmp = Bitmap.createBitmap(big, src.left, src.top, src.width(), src.height());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            cellBmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
            result.add(bos.toByteArray());
            cellBmp.recycle();
        }
        big.recycle();
        return result;
    }

    private static Bitmap decodeBitmap(File f, int reqW, int reqH) {
        try (FileInputStream fis = new FileInputStream(f)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(fis, null, opts);
            int sample = calcSampleSize(opts.outWidth, opts.outHeight, reqW, reqH);
            BitmapFactory.Options dec = new BitmapFactory.Options();
            dec.inSampleSize = sample;
            dec.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (FileInputStream fis2 = new FileInputStream(f)) {
                return BitmapFactory.decodeStream(fis2, null, dec);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static int calcSampleSize(int w, int h, int reqW, int reqH) {
        if (reqW <= 0 || reqH <= 0) return 1;
        int sample = 1;
        while ((w / sample) > reqW * 2 && (h / sample) > reqH * 2) sample *= 2;
        return sample;
    }
}
