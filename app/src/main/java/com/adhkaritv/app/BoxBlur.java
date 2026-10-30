package com.adhkaritv.app;

/** Separable box blur on ARGB pixels; several passes approximate a Gaussian. Pure Java (unit-testable). */
final class BoxBlur {
    private BoxBlur() {}

    static void blur(int[] px, int w, int h, int radius, int passes) {
        int[] tmp = new int[px.length];
        for (int p = 0; p < passes; p++) {
            pass(px, tmp, w, h, radius, true);
            pass(tmp, px, w, h, radius, false);
        }
    }

    /** One 1-D pass along rows (horizontal) or columns, with clamped edges. */
    private static void pass(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int lines = horizontal ? h : w, len = horizontal ? w : h;
        int div = 2 * r + 1;
        for (int line = 0; line < lines; line++) {
            int a = 0, rr = 0, g = 0, b = 0;
            for (int k = -r; k <= r; k++) {
                int c = in[idx(line, clamp(k, len), w, horizontal)];
                a += c >>> 24; rr += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255;
            }
            for (int i = 0; i < len; i++) {
                out[idx(line, i, w, horizontal)] = ((a / div) << 24) | ((rr / div) << 16) | ((g / div) << 8) | (b / div);
                int cOut = in[idx(line, clamp(i - r, len), w, horizontal)];
                int cIn = in[idx(line, clamp(i + r + 1, len), w, horizontal)];
                a += (cIn >>> 24) - (cOut >>> 24);
                rr += ((cIn >> 16) & 255) - ((cOut >> 16) & 255);
                g += ((cIn >> 8) & 255) - ((cOut >> 8) & 255);
                b += (cIn & 255) - (cOut & 255);
            }
        }
    }

    private static int clamp(int i, int len) { return i < 0 ? 0 : (i >= len ? len - 1 : i); }

    private static int idx(int line, int i, int w, boolean horizontal) { return horizontal ? line * w + i : i * w + line; }
}
