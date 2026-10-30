package com.adhkaritv.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdhkarView extends FrameLayout {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ImageView background;
    private final TextView dhikrText, sourceText, categoryText;
    private int index = 0;
    private boolean stopped = false;
    private final Runnable next = new Runnable() { @Override public void run() { showNext(); } };

    // Photographic images hosted by Unsplash. Network access is required to load these.
    // Photographers and individual image licenses should be reviewed before redistributing a bundled photo collection.
    private static final String[] PHOTOS = {
        "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1920&q=85",
        "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?w=1920&q=85",
        "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1920&q=85",
        "https://images.unsplash.com/photo-1500534623283-312aade485b7?w=1920&q=85",
        "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1920&q=85",
        "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?w=1920&q=85"
    };
    // Short Quranic remembrance texts. References refer to the Quran, not fabricated hadith attributions.
    private static final String[][] ADHKAR = {
        {"سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "ذكر عام، صحيح البخاري 6405"},
        {"لَا إِلَٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ", "سورة الأنبياء، الآية 87"},
        {"رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "سورة البقرة، الآية 201"},
        {"رَبِّ زِدْنِي عِلْمًا", "سورة طه، الآية 114"},
        {"حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "سورة آل عمران، الآية 173"},
        {"رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي", "سورة طه، الآيتان 25 و26"},
        {"رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِنْ لَدُنْكَ رَحْمَةً إِنَّكَ أَنْتَ الْوَهَّابُ", "سورة آل عمران، الآية 8"},
        {"وَقُلْ رَبِّ اغْفِرْ وَارْحَمْ وَأَنْتَ خَيْرُ الرَّاحِمِينَ", "سورة المؤمنون، الآية 118"}
    };

    public AdhkarView(Context context) {
        super(context);
        setBackgroundColor(Color.rgb(14,31,33));
        background = new ImageView(context); background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(background, new LayoutParams(-1,-1));
        View scrim = new View(context);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x99071117,0x77071117,0xB8071117}));
        addView(scrim,new LayoutParams(-1,-1));
        LinearLayout column = new LinearLayout(context); column.setOrientation(LinearLayout.VERTICAL); column.setGravity(Gravity.CENTER);
        column.setPadding(dp(70),dp(40),dp(70),dp(45));
        addView(column,new LayoutParams(-1,-1));
        categoryText = text(context,21,0xFFECDFC5); categoryText.setLetterSpacing(0.04f);
        column.addView(categoryText);
        View gap = new View(context); column.addView(gap,new LinearLayout.LayoutParams(1,dp(30)));
        dhikrText = text(context,43,Color.WHITE); dhikrText.setTypeface(Typeface.create("serif",Typeface.NORMAL));
        dhikrText.setLineSpacing(dp(8),1.1f); dhikrText.setMaxLines(5);
        column.addView(dhikrText,new LinearLayout.LayoutParams(-1,-2));
        View gap2 = new View(context); column.addView(gap2,new LinearLayout.LayoutParams(1,dp(24)));
        sourceText = text(context,18,0xFFE5DCCC); column.addView(sourceText);
        showNext();
    }
    private TextView text(Context c, float sp, int color) {
        TextView t = new TextView(c); t.setTextColor(color); t.setTextSize(sp); t.setGravity(Gravity.CENTER); t.setTextDirection(TEXT_DIRECTION_RTL); t.setShadowLayer(3,1,2,Color.BLACK); return t;
    }
    private int dp(float v) { return (int)(v*getResources().getDisplayMetrics().density); }
    private void showNext() {
        if(stopped) return;
        int current=index++;
        String[] item=ADHKAR[current%ADHKAR.length];
        int hour=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        categoryText.setText(hour>=4 && hour<12 ? "أَذْكَارٌ وَأَدْعِيَةٌ • صَبَاحًا" : hour>=17 ? "أَذْكَارٌ وَأَدْعِيَةٌ • مَسَاءً" : "أَذْكَارٌ وَأَدْعِيَةٌ");
        dhikrText.setText(item[0]); sourceText.setText(item[1]);
        final String url=PHOTOS[current%PHOTOS.length];
        executor.execute(()->{
            Bitmap bitmap=null; HttpURLConnection conn=null;
            try { conn=(HttpURLConnection)new URL(url).openConnection(); conn.setConnectTimeout(7000); conn.setReadTimeout(9000); conn.setInstanceFollowRedirects(true); conn.connect();
                if(conn.getResponseCode()==200) bitmap=BitmapFactory.decodeStream(conn.getInputStream());
            } catch(Exception ignored) {} finally { if(conn!=null) conn.disconnect(); }
            final Bitmap result=bitmap;
            handler.post(()->{ if(!stopped && result!=null) { background.setAlpha(0f); background.setImageBitmap(result); background.animate().alpha(1f).setDuration(1800).start(); } });
        });
        handler.removeCallbacks(next); handler.postDelayed(next,30000);
    }
    public void stop() { stopped=true; handler.removeCallbacksAndMessages(null); executor.shutdownNow(); }
}
