package com.adhkaritv.app;
import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
public class MainActivity extends Activity {
    private AdhkarView view;
    @Override public void onCreate(Bundle state) { super.onCreate(state); getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512); view = new AdhkarView(this); setContentView(view); }
    @Override protected void onDestroy() { if(view!=null) view.stop(); super.onDestroy(); }
}
