package com.adhkaritv.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

/** Launcher entry point. OK / Enter / Menu on the remote opens the settings; Back exits. */
public class MainActivity extends Activity {
    private AdhkarView view;
    private boolean hintShown;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        view = new AdhkarView(this);
        setContentView(view);
        hideSystemUi();
    }

    @Override protected void onStart() {
        super.onStart();
        view.start();
        if (!hintShown) { hintShown = true; view.showHint("اضغط زر OK في جهاز التحكم لفتح الإعدادات", 7000); }
    }

    @Override protected void onResume() { super.onResume(); hideSystemUi(); }

    @Override protected void onStop() { view.stop(); super.onStop(); }

    @Override protected void onDestroy() { if (view != null) view.destroy(); super.onDestroy(); }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_SETTINGS:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
