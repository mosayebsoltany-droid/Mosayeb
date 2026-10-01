package com.mose.assistant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class LawyerActivity extends AppCompatActivity {
    private static final int NAVY=Color.rgb(5,17,32), CARD=Color.rgb(16,35,55);
    private static final int GOLD=Color.rgb(232,190,92), CYAN=Color.rgb(42,222,193);
    private SharedPreferences store;
    private LinearLayout caseList;
    private TextToSpeech speaker;

    private final ActivityResultLauncher<String[]> documentPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if(uri==null) return;
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch(Exception ignored) {}
                addValue("documents", uri.toString());
                refresh();
                showMessage("سند با موفقیت به میز وکیل افزوده شد.");
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        store=getSharedPreferences("lawyer_private",MODE_PRIVATE);
        buildUi();
        speaker=new TextToSpeech(this, result -> {
            if(result==TextToSpeech.SUCCESS) {
                int fa=speaker.setLanguage(new Locale("fa","IR"));
                boolean supported=fa!=TextToSpeech.LANG_MISSING_DATA && fa!=TextToSpeech.LANG_NOT_SUPPORTED;
                if(!supported) speaker.setLanguage(Locale.US);
                speaker.setSpeechRate(.9f);
                speaker.speak(supported?"سلطان، چه کنم؟":"Soltan, che konam?",
                        TextToSpeech.QUEUE_FLUSH,null,"lawyer_greeting");
            }
        });
    }

    private void buildUi() {
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(NAVY);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(22),dp(20),dp(30));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root);

        TextView title=text("⚖  وکیل MOSE",26,GOLD,Typeface.BOLD);
        title.setGravity(Gravity.RIGHT);
        root.addView(title);

        TextView greeting=text("سلطان، چه کنم؟",24,Color.WHITE,Typeface.BOLD);
        greeting.setGravity(Gravity.RIGHT);
        greeting.setPadding(0,dp(18),0,dp(6));
        root.addView(greeting);

        TextView privacy=text("🔒 پرونده‌ها و یادداشت‌ها فقط روی همین گوشی نگهداری می‌شوند.",12,Color.rgb(146,170,185),Typeface.NORMAL);
        privacy.setGravity(Gravity.RIGHT);
        root.addView(privacy);

        root.addView(action("＋ پرونده جدید","نام یک پرونده خصوصی را ثبت کنید",v -> newCase()));
        root.addView(action("📎 افزودن سند","PDF، تصویر یا فایل را از گوشی انتخاب کنید",v -> documentPicker.launch(new String[]{"application/pdf","image/*","text/*"})));
        root.addView(action("✍ یادداشت جدید","یادداشت و پیگیری حقوقی را ثبت کنید",v -> newNote()));
        root.addView(action("⌕ جست‌وجوی پرونده","در پرونده‌ها و یادداشت‌های ثبت‌شده بگردید",v -> search()));

        TextView saved=text("پرونده‌های ذخیره‌شده",18,Color.WHITE,Typeface.BOLD);
        saved.setGravity(Gravity.RIGHT);
        saved.setPadding(0,dp(22),0,dp(10));
        root.addView(saved);

        caseList=new LinearLayout(this);
        caseList.setOrientation(LinearLayout.VERTICAL);
        root.addView(caseList);
        refresh();

        Button back=button("بازگشت به مرکز فرمان");
        back.setOnClickListener(v -> finish());
        root.addView(back,params(-1,dp(54),18,0));
        setContentView(scroll);
    }

    private void newCase() { input("نام پرونده جدید","مثلاً: پرونده قرارداد", value -> { addValue("cases",value); refresh(); }); }
    private void newNote() { input("یادداشت جدید","متن یادداشت یا اقدام بعدی", value -> { addValue("notes",value); refresh(); }); }
    private void search() { input("جست‌وجو","نام پرونده یا بخشی از یادداشت", this::showSearch); }

    private void input(String title,String hint,ValueHandler done) {
        EditText input=new EditText(this);
        input.setHint(hint);
        input.setTextDirection(View.TEXT_DIRECTION_RTL);
        input.setPadding(dp(18),dp(12),dp(18),dp(12));
        new AlertDialog.Builder(this).setTitle(title).setView(input)
                .setNegativeButton("انصراف",null)
                .setPositiveButton("ثبت",(d,w)->{
                    String value=input.getText().toString().trim();
                    if(!value.isEmpty()) done.accept(value);
                }).show();
    }

    private void showSearch(String q) {
        StringBuilder out=new StringBuilder();
        for(String x:getValues("cases")) if(x.contains(q)) out.append("پرونده: ").append(x).append("\n");
        for(String x:getValues("notes")) if(x.contains(q)) out.append("یادداشت: ").append(x).append("\n");
        showMessage(out.length()==0?"نتیجه‌ای پیدا نشد.":out.toString());
    }

    private void refresh() {
        if(caseList==null) return;
        caseList.removeAllViews();
        Set<String> cases=getValues("cases"), notes=getValues("notes"), docs=getValues("documents");
        if(cases.isEmpty()&&notes.isEmpty()&&docs.isEmpty()) {
            TextView empty=text("هنوز پرونده‌ای ثبت نشده است.",14,Color.rgb(155,178,190),Typeface.NORMAL);
            empty.setGravity(Gravity.RIGHT);
            caseList.addView(empty);
            return;
        }
        for(String x:cases) addRow("⚖ پرونده",x);
        for(String x:notes) addRow("✍ یادداشت",x);
        for(String x:docs) addRow("📎 سند","سند ذخیره‌شده در گوشی");
    }

    private void addRow(String kind,String value) {
        TextView row=text(kind+"\n"+value,15,Color.WHITE,Typeface.NORMAL);
        row.setGravity(Gravity.RIGHT);
        row.setPadding(dp(15),dp(12),dp(15),dp(12));
        row.setBackground(round(CARD,16));
        caseList.addView(row,params(-1,-2,0,8));
    }

    private void addValue(String key,String value) {
        Set<String> values=new LinkedHashSet<>(getValues(key));
        values.add(value);
        store.edit().putStringSet(key,values).apply();
    }
    private Set<String> getValues(String key) {
        return new LinkedHashSet<>(store.getStringSet(key,new LinkedHashSet<>()));
    }
    private void showMessage(String message) { new AlertDialog.Builder(this).setMessage(message).setPositiveButton("متوجه شدم",null).show(); }

    private View action(String title,String sub,View.OnClickListener click) {
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16),dp(14),dp(16),dp(14));
        box.setBackground(round(CARD,18));
        TextView t=text(title,17,Color.WHITE,Typeface.BOLD); t.setGravity(Gravity.RIGHT);
        TextView s=text(sub,12,Color.rgb(153,177,191),Typeface.NORMAL); s.setGravity(Gravity.RIGHT);
        box.addView(t); box.addView(s); box.setOnClickListener(click);
        box.setElevation(dp(3)); box.setLayoutParams(params(-1,-2,18,0));
        return box;
    }
    private Button button(String value) { Button b=new Button(this); b.setText(value); b.setTextSize(16); b.setTextColor(NAVY); b.setAllCaps(false); b.setBackground(round(CYAN,16)); return b; }
    private TextView text(String s,float size,int color,int style) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.create("sans",style)); return t; }
    private GradientDrawable round(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); d.setStroke(dp(1),Color.rgb(35,70,91)); return d; }
    private LinearLayout.LayoutParams params(int w,int h,int top,int bottom) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(0,dp(top),0,dp(bottom)); return p; }
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private interface ValueHandler { void accept(String value); }
    @Override protected void onDestroy(){ if(speaker!=null){speaker.stop();speaker.shutdown();} super.onDestroy(); }
}
