package com.mose.assistant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class LawyerActivity extends AppCompatActivity {
    private static final int NAVY=Color.rgb(37,20,15), CARD=Color.rgb(63,37,27);
    private static final int GOLD=Color.rgb(224,166,82), CYAN=Color.rgb(239,199,132);
    private SharedPreferences store;
    private LinearLayout caseList;
    private TextToSpeech speaker;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        store=getSharedPreferences("lawyer_private",MODE_PRIVATE);
        buildUi();
        speaker=new TextToSpeech(this,result->{
            if(result==TextToSpeech.SUCCESS){
                int fa=speaker.setLanguage(new Locale("fa","IR"));
                boolean ok=fa!=TextToSpeech.LANG_MISSING_DATA&&fa!=TextToSpeech.LANG_NOT_SUPPORTED;
                if(!ok)speaker.setLanguage(Locale.US);
                speaker.speak(ok?"سلطان، چه کنم؟":"Soltan, che konam?",TextToSpeech.QUEUE_FLUSH,null,"lawyer");
            }
        });
    }

    @Override protected void onResume(){super.onResume();refresh();}

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(NAVY);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(root);
        TextView title=text("⚖  وکیل MOSE",27,GOLD,Typeface.BOLD);title.setGravity(Gravity.RIGHT);root.addView(title);
        TextView greeting=text("سلطان، چه کنم؟",23,Color.WHITE,Typeface.BOLD);greeting.setGravity(Gravity.RIGHT);greeting.setPadding(0,dp(16),0,dp(6));root.addView(greeting);
        TextView privacy=text("🔒 تمام پرونده‌ها فقط در حافظه خصوصی همین برنامه نگهداری می‌شوند.",12,Color.rgb(148,172,187),Typeface.NORMAL);privacy.setGravity(Gravity.RIGHT);root.addView(privacy);

        Button create=button("＋ بازکردن پرونده جدید");create.setOnClickListener(v->newCase());
        root.addView(create,params(-1,dp(58),22,14));

        TextView heading=text("پرونده‌های من",19,Color.WHITE,Typeface.BOLD);heading.setGravity(Gravity.RIGHT);root.addView(heading);
        caseList=new LinearLayout(this);caseList.setOrientation(LinearLayout.VERTICAL);root.addView(caseList);

        Button back=button("بازگشت به مرکز فرمان");back.setOnClickListener(v->finish());root.addView(back,params(-1,dp(54),22,0));
        setContentView(scroll);
        refresh();
    }

    private void newCase(){
        EditText input=new EditText(this);input.setHint("مثلاً: پرونده باریت");input.setTextDirection(View.TEXT_DIRECTION_RTL);
        new AlertDialog.Builder(this).setTitle("نام پرونده جدید").setView(input)
                .setNegativeButton("انصراف",null).setPositiveButton("ایجاد",(d,w)->{
                    String name=input.getText().toString().trim();
                    if(name.isEmpty())return;
                    Set<String> cases=new LinkedHashSet<>(store.getStringSet("case_index",new LinkedHashSet<>()));
                    String id=UUID.randomUUID().toString();
                    cases.add(id+"|"+name);
                    store.edit().putStringSet("case_index",cases).apply();
                    openCase(id,name);
                }).show();
    }

    private void refresh(){
        if(caseList==null)return;caseList.removeAllViews();
        Set<String> cases=store.getStringSet("case_index",new LinkedHashSet<>());
        if(cases.isEmpty()){
            TextView empty=text("هنوز پرونده‌ای ایجاد نشده است. روی «بازکردن پرونده جدید» بزنید.",14,Color.rgb(156,180,193),Typeface.NORMAL);
            empty.setGravity(Gravity.RIGHT);empty.setPadding(0,dp(18),0,dp(18));caseList.addView(empty);return;
        }
        for(String raw:cases){
            int split=raw.indexOf('|');if(split<1)continue;
            String id=raw.substring(0,split),name=raw.substring(split+1);
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(16),dp(15),dp(16),dp(15));row.setBackground(round(CARD,18));
            TextView n=text("⚖  "+name,18,Color.WHITE,Typeface.BOLD);n.setGravity(Gravity.RIGHT);row.addView(n);
            TextView sub=text("ورود به پرونده، اسناد، یادداشت‌ها و ابزارهای حقوقی",12,Color.rgb(150,177,191),Typeface.NORMAL);sub.setGravity(Gravity.RIGHT);row.addView(sub);
            row.setOnClickListener(v->openCase(id,name));caseList.addView(row,params(-1,-2,12,0));
        }
    }

    private void openCase(String id,String name){
        Intent i=new Intent(this,LegalCaseActivity.class);i.putExtra("case_id",id);i.putExtra("case_name",name);startActivity(i);
    }
    private Button button(String value){Button b=new Button(this);b.setText(value);b.setTextSize(16);b.setTextColor(NAVY);b.setAllCaps(false);b.setBackground(round(CYAN,16));return b;}
    private TextView text(String s,float size,int color,int style){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans",style));return t;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),Color.rgb(35,70,91));return d;}
    private LinearLayout.LayoutParams params(int w,int h,int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(0,dp(top),0,dp(bottom));return p;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    @Override protected void onDestroy(){if(speaker!=null){speaker.stop();speaker.shutdown();}super.onDestroy();}
}
