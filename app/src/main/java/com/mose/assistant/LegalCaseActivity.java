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
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

public class LegalCaseActivity extends AppCompatActivity {
    private static final int NAVY=Color.rgb(37,20,15),CARD=Color.rgb(63,37,27),GOLD=Color.rgb(224,166,82),CYAN=Color.rgb(239,199,132);
    private SharedPreferences store; private String caseId,caseName; private LinearLayout timeline;
    private EditText aiInput; private TextView aiLog;
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

        LinearLayout aiCard=new LinearLayout(this);aiCard.setOrientation(LinearLayout.VERTICAL);aiCard.setPadding(dp(16),dp(15),dp(16),dp(15));aiCard.setBackground(round(Color.rgb(78,45,31),20));
        TextView aiTitle=text("✦ وکیل هوشمند",19,GOLD,Typeface.BOLD);aiTitle.setGravity(Gravity.RIGHT);aiCard.addView(aiTitle);
        aiLog=text("سلطان، سؤال حقوقی یا دستور تنظیم متن را بفرمایید.",14,Color.rgb(247,231,207),Typeface.NORMAL);aiLog.setGravity(Gravity.RIGHT);aiLog.setPadding(0,dp(10),0,dp(10));aiCard.addView(aiLog);
        aiInput=new EditText(this);aiInput.setHint("سؤال یا دستور شما؛ فقط همین متن ارسال می‌شود");aiInput.setTextColor(Color.WHITE);aiInput.setHintTextColor(Color.rgb(177,150,130));aiInput.setTextDirection(View.TEXT_DIRECTION_RTL);aiInput.setMinLines(2);aiCard.addView(aiInput,params(-1,-2,4,8));
        LinearLayout aiButtons=new LinearLayout(this);aiButtons.setOrientation(LinearLayout.HORIZONTAL);
        Button settings=button("تنظیم کلید AI");settings.setOnClickListener(v->configureAi());aiButtons.addView(settings,new LinearLayout.LayoutParams(0,dp(50),1));
        Button send=button("ارسال سؤال");send.setOnClickListener(v->askAi());LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(50),1);sp.setMargins(dp(8),0,0,0);aiButtons.addView(send,sp);
        aiCard.addView(aiButtons);
        Button reviewers=button("تنظیم بازبین‌های DeepSeek و OpenAI");reviewers.setOnClickListener(v->configureReviewers());aiCard.addView(reviewers,params(-1,dp(48),9,0));
        root.addView(aiCard,params(-1,-2,4,14));

        root.addView(action("📎 افزودن PDF یا تصویر","سند را فقط در همین پرونده نگهداری کن",v->picker.launch(new String[]{"application/pdf","image/*","text/*"})));
        root.addView(action("✍ ثبت یادداشت و اقدام","جلسه، تماس، مهلت یا اقدام بعدی",v->input("یادداشت پرونده","متن یادداشت",x->{add("notes",x);refresh();})));
        root.addView(action("⚖ تنظیم متن حقوقی","لایحه، دادخواست، اظهارنامه یا شکواییه",v->chooseDraft()));
        root.addView(action("🔎 تحلیل خصوصی اسناد","مرحله بعد: استخراج متن و بررسی آفلاین PDF",v->message("اسناد این پرونده محفوظ‌اند. موتور تحلیل آفلاین PDF در مرحله بعد روی گوشی اضافه می‌شود.")));
        root.addView(action("⌕ جست‌وجوی داخل پرونده","جست‌وجو در یادداشت‌ها و پیش‌نویس‌ها",v->input("جست‌وجو","عبارت موردنظر",this::search)));

        TextView h=text("محتوای پرونده",18,Color.WHITE,Typeface.BOLD);h.setGravity(Gravity.RIGHT);h.setPadding(0,dp(20),0,dp(8));root.addView(h);
        timeline=new LinearLayout(this);timeline.setOrientation(LinearLayout.VERTICAL);root.addView(timeline);refresh();

        Button back=button("بازگشت به فهرست پرونده‌ها");back.setOnClickListener(v->finish());root.addView(back,params(-1,dp(54),20,0));setContentView(scroll);
    }

    private void configureAi(){
        EditText input=new EditText(this);input.setHint("کلید Gemini API");input.setSingleLine(true);
        new AlertDialog.Builder(this).setTitle("اتصال هوش مصنوعی").setMessage("کلید در فضای خصوصی برنامه ذخیره می‌شود. فقط متن‌هایی که خودتان ارسال می‌کنید به Gemini می‌روند.")
                .setView(input).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",(d,w)->{String key=input.getText().toString().trim();if(!key.isEmpty()){getSharedPreferences("mose_private_settings",MODE_PRIVATE).edit().putString("gemini_key",key).apply();message("کلید ذخیره شد.");}}).show();
    }
    private void configureReviewers(){
        SharedPreferences p=getSharedPreferences("mose_private_settings",MODE_PRIVATE);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),0,dp(14),0);
        EditText deep=new EditText(this);deep.setHint("کلید API دیپ‌سیک");deep.setSingleLine(true);box.addView(deep);
        EditText open=new EditText(this);open.setHint("کلید OpenAI API");open.setSingleLine(true);box.addView(open);
        new AlertDialog.Builder(this).setTitle("بازبینی چندمدلی")
                .setMessage("متن تولیدشده برای نقد به DeepSeek و سپس برای بازبینی نهایی به OpenAI ارسال می‌شود. هر دو کلید اختیاری‌اند.")
                .setView(box).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",(d,w)->{
                    SharedPreferences.Editor e=p.edit();
                    if(!deep.getText().toString().trim().isEmpty())e.putString("deepseek_key",deep.getText().toString().trim());
                    if(!open.getText().toString().trim().isEmpty())e.putString("openai_key",open.getText().toString().trim());
                    e.apply();message("تنظیمات بازبین‌ها ذخیره شد.");
                }).show();
    }

    private void askAi(){
        String question=aiInput.getText().toString().trim();if(question.isEmpty())return;
        String key=getSharedPreferences("mose_private_settings",MODE_PRIVATE).getString("gemini_key","");
        if(key.isEmpty()){configureAi();return;}
        new AlertDialog.Builder(this).setTitle("تأیید ارسال").setMessage("فقط متن همین سؤال برای تحلیل به Gemini ارسال شود؟")
                .setNegativeButton("خیر",null).setPositiveButton("بله، ارسال شود",(d,w)->sendQuestion(question,key)).show();
    }
    private void sendQuestion(String question,String key){
        aiInput.setText("");aiLog.setText("در حال بررسی حقوقی…");
        new Thread(()->{try{
            URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key="+key);
            HttpURLConnection con=(HttpURLConnection)url.openConnection();con.setRequestMethod("POST");con.setDoOutput(true);con.setConnectTimeout(20000);con.setReadTimeout(60000);con.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            String prompt="شما دستیار حقوقی فارسی‌زبان آشنا با حقوق ایران هستید. پاسخ رسمی، دقیق و ساختاریافته بدهید. مواد قانونی را فقط در صورت اطمینان ذکر و موارد نامطمئن را مشخص کنید. این پاسخ جایگزین بررسی وکیل دارای پروانه نیست. درخواست: "+question;
            JSONObject body=new JSONObject();JSONArray contents=new JSONArray();JSONObject one=new JSONObject();JSONArray parts=new JSONArray();parts.put(new JSONObject().put("text",prompt));one.put("parts",parts);contents.put(one);body.put("contents",contents);
            try(OutputStream os=con.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int code=con.getResponseCode();BufferedReader br=new BufferedReader(new InputStreamReader(code<400?con.getInputStream():con.getErrorStream(),StandardCharsets.UTF_8));StringBuilder raw=new StringBuilder();String line;while((line=br.readLine())!=null)raw.append(line);
            if(code>=400)throw new Exception();JSONObject response=new JSONObject(raw.toString());String answer=response.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
            String reviewed=reviewWithDeepSeek(question,answer);
            String finalText=reviewWithOpenAI(question,reviewed);
            String label=(reviewed.equals(answer)?"⚠ بدون بازبینی DeepSeek\n":"✓ بازبینی DeepSeek\n")+(finalText.equals(reviewed)?"⚠ بدون تأیید OpenAI\n\n":"✓ تأیید نهایی OpenAI\n\n");
            String result=label+finalText;
            runOnUiThread(()->{aiLog.setText(result);add("drafts",result);refresh();});
        }catch(Exception e){runOnUiThread(()->aiLog.setText("اتصال انجام نشد؛ کلید، اینترنت یا سهمیه رایگان را بررسی کنید."));}}).start();
    }

    private String reviewWithDeepSeek(String question,String draft){
        String key=getSharedPreferences("mose_private_settings",MODE_PRIVATE).getString("deepseek_key","");
        if(key.isEmpty())return draft;
        try{
            URL u=new URL("https://api.deepseek.com/chat/completions");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(20000);c.setReadTimeout(60000);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+key);
            String prompt="به عنوان بازبین حقوق ایران، متن زیر را از نظر تناقض، ادعای بی‌دلیل، مواد قانونی احتمالا نادرست، نقص خواسته و ساختار نقد و سپس نسخه اصلاح‌شده کامل را ارائه کن. درخواست اصلی: "+question+"\nمتن: "+draft;
            JSONObject body=new JSONObject().put("model","deepseek-chat").put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",prompt)));
            try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int code=c.getResponseCode();if(code>=400)return draft;BufferedReader b=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));StringBuilder raw=new StringBuilder();String l;while((l=b.readLine())!=null)raw.append(l);
            return new JSONObject(raw.toString()).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        }catch(Exception e){return draft;}
    }
    private String reviewWithOpenAI(String question,String draft){
        String key=getSharedPreferences("mose_private_settings",MODE_PRIVATE).getString("openai_key","");
        if(key.isEmpty())return draft;
        try{
            URL u=new URL("https://api.openai.com/v1/responses");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(20000);c.setReadTimeout(60000);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+key);
            String prompt="این متن قبلا تولید و نقد شده است. به عنوان بازبین نهایی حقوق ایران، فقط نسخه نهایی منسجم را ارائه کن؛ هیچ ماده قانونی مشکوک را قطعی ننویس و کاستی‌های اطلاعاتی را مشخص کن. درخواست: "+question+"\nمتن بازبینی‌شده: "+draft;
            JSONObject body=new JSONObject().put("model","gpt-5-mini").put("input",prompt);
            try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int code=c.getResponseCode();if(code>=400)return draft;BufferedReader b=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));StringBuilder raw=new StringBuilder();String l;while((l=b.readLine())!=null)raw.append(l);
            JSONObject response=new JSONObject(raw.toString());JSONArray output=response.getJSONArray("output");
            for(int i=0;i<output.length();i++){JSONObject item=output.getJSONObject(i);if(item.has("content")){JSONArray parts=item.getJSONArray("content");for(int j=0;j<parts.length();j++){JSONObject part=parts.getJSONObject(j);if(part.has("text"))return part.getString("text");}}}
            return draft;
        }catch(Exception e){return draft;}
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
