package com.adhkaritv.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Frosted-glass panel. Android 10 has no live blur, so the card shows a pre-blurred copy of the current
 * background photo, positioned exactly behind the card, under a soft tint and a thin light border.
 * It must be a direct child of the full-screen view so its position maps onto the photo.
 */
final class GlassCard extends FrameLayout {

    final LinearLayout content;
    private final ImageView[] layers = new ImageView[2];
    private int front = 0;
    private boolean hasBackdrop;

    GlassCard(Context c, final float radius, int strokePx, int tintColor) {
        super(c);
        for (int i = 0; i < 2; i++) {
            ImageView iv = new ImageView(c);
            iv.setScaleType(ImageView.ScaleType.MATRIX);
            iv.setAlpha(0f);
            layers[i] = iv;
            addView(iv, new LayoutParams(-1, -1));
        }
        View tint = new View(c);
        GradientDrawable g = new GradientDrawable();
        g.setColor(tintColor);
        g.setCornerRadius(radius);
        g.setStroke(strokePx, 0x4DFFFFFF);
        tint.setBackground(g);
        addView(tint, new LayoutParams(-1, -1));

        content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);
        addView(content, new LayoutParams(-2, -2));

        setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius); }
        });
        setClipToOutline(true);
    }

    /** Cross-fades to a new blurred backdrop (same timing as the photo cross-fade). */
    void setBackdrop(Bitmap blurred) {
        final ImageView incoming = layers[1 - front];
        final ImageView outgoing = layers[front];
        incoming.animate().cancel();
        outgoing.animate().cancel();
        incoming.setImageBitmap(blurred);
        alignLayer(incoming);
        if (!hasBackdrop || indexOfChild(incoming) > indexOfChild(outgoing)) {
            incoming.setAlpha(0f);
            outgoing.setAlpha(hasBackdrop ? 1f : 0f);
            incoming.animate().alpha(1f).setDuration(2000).withEndAction(() -> {
                outgoing.setAlpha(0f);
                outgoing.setImageDrawable(null);
            }).start();
        } else {
            incoming.setAlpha(1f);
            outgoing.animate().alpha(0f).setDuration(2000).withEndAction(() -> outgoing.setImageDrawable(null)).start();
        }
        front = 1 - front;
        hasBackdrop = true;
    }

    void clear() { for (ImageView iv : layers) iv.setImageDrawable(null); }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        for (ImageView iv : layers) alignLayer(iv);
    }

    /** Same geometry as CENTER_CROP on the full screen, shifted by this card's position. */
    private void alignLayer(ImageView iv) {
        Drawable d = iv.getDrawable();
        View parent = (View) getParent();
        if (d == null || parent == null || parent.getWidth() == 0) return;
        float bw = d.getIntrinsicWidth(), bh = d.getIntrinsicHeight();
        if (bw <= 0 || bh <= 0) return;
        float W = parent.getWidth(), H = parent.getHeight();
        float s = Math.max(W / bw, H / bh);
        Matrix m = new Matrix();
        m.setScale(s, s);
        m.postTranslate((W - bw * s) / 2f - getLeft(), (H - bh * s) / 2f - getTop());
        iv.setImageMatrix(m);
    }
}
