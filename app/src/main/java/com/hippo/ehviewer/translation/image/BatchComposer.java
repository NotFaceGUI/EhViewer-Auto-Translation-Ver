package com.hippo.ehviewer.translation.image;

import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class BatchComposer {

    public static class Batch {
        public int startPage;
        public int endPage;
        public List<File> files = new ArrayList<>();
        public ImageGrid.GridLayout layout;
    }

    public static List<Batch> plan(List<File> pageFiles, int[] pageNumbers, long maxImagePixels, int maxPagesPerBatch) {
        List<Batch> batches = new ArrayList<>();
        if (pageFiles == null || pageFiles.isEmpty() || pageNumbers == null) return batches;
        int n = Math.min(pageFiles.size(), pageNumbers.length);
        if (maxPagesPerBatch <= 0) maxPagesPerBatch = 6;

        List<Integer> widths = new ArrayList<>();
        List<Integer> heights = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int[] wh = getImageSize(pageFiles.get(i));
            widths.add(wh[0]);
            heights.add(wh[1]);
        }

        int start = 0;
        long maxDim = (long) Math.sqrt(maxImagePixels);
        if (maxDim <= 0) maxDim = 2048;
        while (start < n) {
            int bestSize = 1;
            int bestCols = 1;

            for (int trySize = 1; trySize <= maxPagesPerBatch && start + trySize <= n; trySize++) {
                int count = trySize;
                List<Integer> subW = new ArrayList<>(widths.subList(start, start + count));
                List<Integer> subH = new ArrayList<>(heights.subList(start, start + count));

                int foundCols = 1;
                for (int cols = 3; cols >= 1; cols--) {
                    if (cols > count) continue;
                    ImageGrid.GridLayout testLayout = ImageGrid.planLayout(subW, subH, count, cols);
                    if (testLayout.canvasW <= maxDim && testLayout.canvasH <= maxDim) {
                        foundCols = cols;
                        break;
                    }
                }

                ImageGrid.GridLayout finalLayout = ImageGrid.planLayout(subW, subH, count, foundCols);
                if (finalLayout.canvasW <= maxDim && finalLayout.canvasH <= maxDim && trySize > bestSize) {
                    bestSize = trySize;
                    bestCols = foundCols;
                } else if (finalLayout.canvasW > maxDim || finalLayout.canvasH > maxDim) {
                    break;
                }
            }

            if (bestSize <= 0) bestSize = 1;
            int end = Math.min(start + bestSize - 1, n - 1);
            int count = end - start + 1;
            List<Integer> subW = new ArrayList<>(widths.subList(start, end + 1));
            List<Integer> subH = new ArrayList<>(heights.subList(start, end + 1));
            ImageGrid.GridLayout layout = ImageGrid.planLayout(subW, subH, count, bestCols);

            Batch b = new Batch();
            b.startPage = pageNumbers[start];
            b.endPage = pageNumbers[end];
            for (int i = start; i <= end; i++) b.files.add(pageFiles.get(i));
            b.layout = layout;
            batches.add(b);

            start = end + 1;
        }
        return batches;
    }

    public static int[] getImageSize(File f) {
        int[] wh = new int[]{0, 0};
        if (f == null || !f.exists()) return wh;
        try (FileInputStream fis = new FileInputStream(f)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(fis, null, opts);
            wh[0] = opts.outWidth;
            wh[1] = opts.outHeight;
        } catch (Exception e) {
        }
        if (wh[0] <= 0) wh[0] = 1024;
        if (wh[1] <= 0) wh[1] = 1448;
        return wh;
    }
}
