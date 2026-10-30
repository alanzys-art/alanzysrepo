package com.adhkaritv.app;

import android.service.dreams.DreamService;

/**
 * Screensaver (Daydream). Renders the same AdhkarView as the app. Not interactive: any remote key
 * wakes the TV, as Android expects of a screensaver. Work runs only while dreaming and every
 * resource is released when the dream is detached.
 */
public class AdhkarDreamService extends DreamService {
    private AdhkarView view;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        view = new AdhkarView(this);
        setContentView(view);
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        if (view != null) view.start();
    }

    @Override public void onDreamingStopped() {
        if (view != null) view.stop();
        super.onDreamingStopped();
    }

    @Override public void onDetachedFromWindow() {
        if (view != null) { view.destroy(); view = null; }
        super.onDetachedFromWindow();
    }
}
