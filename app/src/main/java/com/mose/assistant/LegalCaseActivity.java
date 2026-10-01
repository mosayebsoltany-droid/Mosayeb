package com.mose.assistant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.Set;

public class LegalCaseActivity extends AppCompatActivity {
    private static final int NAVY=Color.rgb(5,17,32),CARD=Color.rgb(16,35,55),GOLD=Color.rgb(232,190,92),CYAN=Color.rgb(42,222,193);
    private SharedPreferences store; private String caseId,caseName; private LinearLayout timeline;
    private final ActivityResultLauncher<String[]> picker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{
        if(uri==null)return;
        try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        add("docs",uri.toString());refresh();message("سند در پرونده «"+caseName+"» ذخیره شد.");
    });

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);
        caseId=getIntent().getStringExtra("case_id");caseName=getIntent().getStringExtra("case_name");
        if(caseId==null||caseName==null){finish();return;}
        store=getSharedPreferences("lawyer_case_"+caseId,MODE_PRIVATE);build();
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(NAVY);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(20),dp(20),dp(30));root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(root);
        TextView title=text("⚖ "+caseName,24,GOLD,Typeface.BOLD);title.setGravity(Gravity.RIGHT);root.addView(title);
        TextView sub=text("میز خصوصی پرونده — اسناد این پرونده از سایر پرونده‌ها جداست.",12,Color.rgb(148,173,188),Typeface.NORMAL);sub.setGravity(Gravity.RIGHT);sub.setPadding(0,dp(8),0,dp(14));root.addView(sub);

        root.addView(action("📎 افزودن PDF یا تصویر","سند را فقط در همین پرونده نگهداری کن",v->picker.launch(new String[]{"application/pdf","image/*","text/*"})));
        root.addView(action("✍ ثبت یادداشت و اقدام","جلسه، تماس، مهلت یا اقدام بعدی",v->input("یادداشت پرونده","متن یادداشت",x->{add("notes",x);refresh();})));
        root.addView(action("⚖ تنظیم متن حقوقی","لایحه، دادخواست، اظهارنامه یا شکواییه",v->chooseDraft()));
        root.addView(action("🔎 تحلیل خصوصی اسناد","مرحله بعد: استخراج متن و بررسی آفلاین PDF",v->message("اسناد این پرونده محفوظ‌اند. موتور تحلیل آفلاین PDF در مرحله بعد روی گوشی اضافه می‌شود.")));
        root.addView(action("⌕ جست‌وجوی داخل پرونده","جست‌وجو در یادداشت‌ها و پیش‌نویس‌ها",v->input("جست‌وجو","عبارت موردنظر",this::search)));

        TextView h=text("محتوای پرونده",18,Color.WHITE,Typeface.BOLD);h.setGravity(Gravity.RIGHT);h.setPadding(0,dp(20),0,dp(8));root.addView(h);
        timeline=new LinearLayout(this);timeline.setOrientation(LinearLayout.VERTICAL);root.addView(timeline);refresh();

        Button back=button("بازگشت به فهرست پرونده‌ها");back.setOnClickListener(v->finish());root.addView(back,params(-1,dp(54),20,0));setContentView(scroll);
    }

    private void chooseDraft(){
        String[] types={"لایحه","دادخواست","اظهارنامه","شکواییه","درخواست کارشناسی"};
        new AlertDialog.Builder(this).setTitle("نوع متن حقوقی").setItems(types,(d,which)->input(types[which],"موضوع و خواسته را بنویسید",facts->createDraft(types[which],facts))).show();
    }
    private void createDraft(String type,String facts){
        String draft="پیش‌نویس "+type+"\n\nموضوع: "+facts+"\n\nریاست محترم مرجع رسیدگی\nبا سلام و احترام،\nاینجانب/این شرکت در خصوص موضوع فوق، با استناد به اسناد موجود در پرونده، تقاضای رسیدگی و اتخاذ تصمیم قانونی را دارم.\n\nدلایل و مستندات:\n۱. اسناد بارگذاری‌شده در پرونده\n۲. مکاتبات و یادداشت‌های ثبت‌شده\n\nخواسته:\nرسیدگی، بررسی مستندات و صدور تصمیم شایسته قانونی.\n\nاین متن پیش‌نویس اولیه است و پیش از ثبت رسمی باید بازبینی حقوقی شود.";
        add("drafts",draft);refresh();message(draft);
    }
    private void search(String q){
        StringBuilder out=new StringBuilder();
        for(String x:get("notes"))if(x.contains(q))out.append("یادداشت: ").append(x).append("\n\n");
        for(String x:get("drafts"))if(x.contains(q))out.append("پیش‌نویس: ").append(x).append("\n\n");
        message(out.length()==0?"نتیجه‌ای پیدا نشد.":out.toString());
    }
    private void refresh(){
        if(timeline==null)return;timeline.removeAllViews();
        Set<String> docs=get("docs"),notes=get("notes"),drafts=get("drafts");
        if(docs.isEmpty()&&notes.isEmpty()&&drafts.isEmpty()){TextView e=text("هنوز سند یا یادداشتی در این پرونده نیست.",14,Color.rgb(155,178,191),Typeface.NORMAL);e.setGravity(Gravity.RIGHT);timeline.addView(e);return;}
        for(String x:docs)addRow("📎 سند","PDF یا تصویر ذخیره‌شده",v->openUri(x));
        for(String x:notes)addRow("✍ یادداشت",x,null);
        for(String x:drafts)addRow("⚖ پیش‌نویس حقوقی",x,v->message(x));
    }
    private void openUri(String raw){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(raw)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}catch(Exception e){message("برنامه‌ای برای بازکردن این سند پیدا نشد.");}}
    private void input(String title,String hint,Handler h){EditText i=new EditText(this);i.setHint(hint);i.setTextDirection(View.TEXT_DIRECTION_RTL);new AlertDialog.Builder(this).setTitle(title).setView(i).setNegativeButton("انصراف",null).setPositiveButton("ثبت",(d,w)->{String x=i.getText().toString().trim();if(!x.isEmpty())h.accept(x);}).show();}
    private void add(String key,String value){Set<String>s=new LinkedHashSet<>(get(key));s.add(value);store.edit().putStringSet(key,s).apply();}
    private Set<String> get(String key){return new LinkedHashSet<>(store.getStringSet(key,new LinkedHashSet<>()));}
    private void message(String x){new AlertDialog.Builder(this).setMessage(x).setPositiveButton("متوجه شدم",null).show();}
    private View action(String a,String b,View.OnClickListener c){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(15),dp(13),dp(15),dp(13));x.setBackground(round(CARD,17));TextView t=text(a,16,Color.WHITE,Typeface.BOLD);t.setGravity(Gravity.RIGHT);TextView s=text(b,12,Color.rgb(151,176,191),Typeface.NORMAL);s.setGravity(Gravity.RIGHT);x.addView(t);x.addView(s);x.setOnClickListener(c);x.setLayoutParams(params(-1,-2,9,0));return x;}
    private void addRow(String a,String b,View.OnClickListener c){View x=action(a,b,c==null?v->{}:c);timeline.addView(x);}
    private Button button(String x){Button b=new Button(this);b.setText(x);b.setTextSize(16);b.setTextColor(NAVY);b.setAllCaps(false);b.setBackground(round(CYAN,16));return b;}
    private TextView text(String s,float z,int c,int st){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setTypeface(Typeface.create("sans",st));return t;}
    private GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));d.setStroke(dp(1),Color.rgb(35,70,91));return d;}
    private LinearLayout.LayoutParams params(int w,int h,int t,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(0,dp(t),0,dp(b));return p;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private interface Handler{void accept(String value);}
}
