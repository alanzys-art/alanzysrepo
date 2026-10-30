package com.adhkaritv.app;
import android.service.dreams.DreamService;
public class AdhkarDreamService extends DreamService {
    private AdhkarView view;
    @Override public void onAttachedToWindow() { super.onAttachedToWindow(); setInteractive(false); setFullscreen(true); setScreenBright(true); view = new AdhkarView(this); setContentView(view); }
    @Override public void onDetachedFromWindow() { if(view!=null) view.stop(); super.onDetachedFromWindow(); }
}
