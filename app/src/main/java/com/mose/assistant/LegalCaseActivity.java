package com.mose.assistant;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.database.Cursor;
import android.provider.OpenableColumns;
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
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

public class LegalCaseActivity extends AppCompatActivity {
    private static final int NAVY=Color.rgb(2,23,39),CARD=Color.rgb(16,42,61),GOLD=Color.rgb(230,181,76),CYAN=Color.rgb(47,214,190);
    private SharedPreferences store; private String caseId,caseName; private LinearLayout timeline;
    private EditText aiInput; private TextView aiLog;
    private final ActivityResultLauncher<String[]> picker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{
        if(uri==null)return;
        try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        add("docs",uri.toString());refresh();
        new AlertDialog.Builder(this).setTitle("سند ذخیره شد").setMessage("سند در پرونده «"+caseName+"» ذخیره شد. اکنون برای تحلیل حقوقی ارسال شود؟")
                .setNegativeButton("فعلاً نه",null).setPositiveButton("تحلیل شود",(d,w)->confirmDocumentAnalysis(uri)).show();
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

        LinearLayout aiCard=new LinearLayout(this);aiCard.setOrientation(LinearLayout.VERTICAL);aiCard.setPadding(dp(16),dp(15),dp(16),dp(15));aiCard.setBackground(round(Color.rgb(16,42,61),20));
        TextView aiTitle=text("✦ وکیل هوشمند",19,GOLD,Typeface.BOLD);aiTitle.setGravity(Gravity.RIGHT);aiCard.addView(aiTitle);
        aiLog=text("سلطان، سؤال حقوقی یا دستور تنظیم متن را بفرمایید.",14,Color.rgb(247,231,207),Typeface.NORMAL);aiLog.setGravity(Gravity.RIGHT);aiLog.setPadding(0,dp(10),0,dp(10));aiCard.addView(aiLog);
        aiInput=new EditText(this);aiInput.setHint("سؤال یا دستور شما؛ فقط همین متن ارسال می‌شود");aiInput.setTextColor(Color.WHITE);aiInput.setHintTextColor(Color.rgb(177,150,130));aiInput.setTextDirection(View.TEXT_DIRECTION_RTL);aiInput.setMinLines(2);aiCard.addView(aiInput,params(-1,-2,4,8));
        LinearLayout aiButtons=new LinearLayout(this);aiButtons.setOrientation(LinearLayout.HORIZONTAL);
        Button settings=button("اتصال Gemini برای PDF");settings.setOnClickListener(v->configureAi());aiButtons.addView(settings,new LinearLayout.LayoutParams(0,dp(50),1));
        Button send=button("ارسال سؤال");send.setOnClickListener(v->askAi());LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(50),1);sp.setMargins(dp(8),0,0,0);aiButtons.addView(send,sp);
        aiCard.addView(aiButtons);
        Button reviewers=button("اتصال DeepSeek و OpenAI (الزامی)");reviewers.setOnClickListener(v->configureReviewers());aiCard.addView(reviewers,params(-1,dp(48),9,0));
        root.addView(aiCard,params(-1,-2,4,14));

        root.addView(action("📎 افزودن PDF یا تصویر","سند را فقط در همین پرونده نگهداری کن",v->picker.launch(new String[]{"application/pdf","image/*","text/*"})));
        root.addView(action("✍ ثبت یادداشت و اقدام","جلسه، تماس، مهلت یا اقدام بعدی",v->input("یادداشت پرونده","متن یادداشت",x->{add("notes",x);refresh();message("یادداشت با موفقیت در پرونده ذخیره شد.");})));
        root.addView(action("⚖ تنظیم متن حقوقی","لایحه، دادخواست، اظهارنامه یا شکواییه",v->chooseDraft()));
        root.addView(action("🔎 تحلیل هوشمند PDF","انتخاب سند، استخراج نکات و راستی‌آزمایی چندمدلی",v->chooseDocumentForAnalysis()));
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
        new AlertDialog.Builder(this).setTitle("اتصال الزامی نویسنده و بازبین")
                .setMessage("DeepSeek متن حقوقی را می‌نویسد و OpenAI نسخه نهایی را بازبینی می‌کند. برای تولید هوشمند، ثبت هر دو کلید الزامی است.")
                .setView(box).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",(d,w)->{
                    String dk=deep.getText().toString().trim(),ok=open.getText().toString().trim();
                    if(dk.isEmpty()||ok.isEmpty()){message("هر دو کلید DeepSeek و OpenAI الزامی‌اند و چیزی ذخیره نشد.");return;}
                    p.edit().putString("deepseek_key",dk).putString("openai_key",ok).apply();message("اتصال DeepSeek و OpenAI ذخیره شد.");
                }).show();
    }

    private void chooseDocumentForAnalysis(){
        Set<String> docs=get("docs");
        if(docs.isEmpty()){message("ابتدا یک فایل PDF یا تصویر به پرونده اضافه کنید.");return;}
        String[] uris=docs.toArray(new String[0]);String[] names=new String[uris.length];
        for(int i=0;i<uris.length;i++)names[i]=(i+1)+" — "+getDisplayName(Uri.parse(uris[i]));
        new AlertDialog.Builder(this).setTitle("انتخاب سند برای تحلیل").setItems(names,(d,which)->confirmDocumentAnalysis(Uri.parse(uris[which]))).show();
    }
    private void confirmDocumentAnalysis(Uri uri){
        String key=getSharedPreferences("mose_private_settings",MODE_PRIVATE).getString("gemini_key","");
        SharedPreferences settings=getSharedPreferences("mose_private_settings",MODE_PRIVATE);
        String deep=settings.getString("deepseek_key",""),open=settings.getString("openai_key","");
        if(key.isEmpty()){message("برای خواندن PDF ابتدا اتصال Gemini را ثبت کنید.");configureAi();return;}
        if(deep.isEmpty()||open.isEmpty()){message("برای تحلیل نهایی PDF، اتصال DeepSeek و OpenAI نیز الزامی است.");configureReviewers();return;}
        String name=getDisplayName(uri);long size=getDocumentSize(uri);
        if(size>10L*1024L*1024L){message("حجم این فایل بیشتر از ۱۰ مگابایت است. برای حفظ پایداری، فایل را کم‌حجم یا به چند بخش تقسیم کنید.");return;}
        String sizeText=size>0?String.format(java.util.Locale.US,"%.1f مگابایت",size/1048576.0):"نامشخص";
        new AlertDialog.Builder(this).setTitle("اجازه تحلیل سند")
                .setMessage("فایل: "+name+"\nحجم: "+sizeText+"\n\nاصل فایل برای استخراج و تحلیل به Gemini ارسال می‌شود. سپس متن تحلیل برای راستی‌آزمایی به DeepSeek و OpenAI فرستاده می‌شود. آیا تأیید می‌کنید؟")
                .setNegativeButton("خیر",null).setPositiveButton("تأیید و تحلیل",(d,w)->analyzeDocument(uri,key,name)).show();
    }
    private void analyzeDocument(Uri uri,String key,String name){
        aiLog.setText("در حال خواندن و تحلیل سند «"+name+"»…");
        new Thread(()->{try{
            byte[] bytes=readDocument(uri,10L*1024L*1024L);
            String mime=getContentResolver().getType(uri);if(mime==null)mime="application/pdf";
            URL url=new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key="+key);
            HttpURLConnection con=(HttpURLConnection)url.openConnection();con.setRequestMethod("POST");con.setDoOutput(true);con.setConnectTimeout(30000);con.setReadTimeout(120000);con.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            String prompt="این سند متعلق به پرونده «"+caseName+"» است. سند را به فارسی و با رویکرد حقوق ایران تحلیل کن. خروجی شامل: ۱) نوع و خلاصه سند، ۲) طرفین و سمت‌ها، ۳) تاریخ‌ها، شماره‌ها، مبالغ و تعهدات، ۴) ادعاها و ادله، ۵) تعارض‌ها و ابهام‌ها، ۶) نقاط قوت و ضعف اثباتی، ۷) مدارک مفقود، ۸) اقدامات و مهلت‌های پیشنهادی، ۹) هشدار درباره مواد قانونی نامطمئن باشد. هیچ متن ناخوانا یا ماده قانونی را حدس نزن و برای هر مورد نامطمئن صریحاً بنویس نیازمند بررسی است.";
            JSONArray parts=new JSONArray().put(new JSONObject().put("text",prompt))
                    .put(new JSONObject().put("inline_data",new JSONObject().put("mime_type",mime).put("data",Base64.encodeToString(bytes,Base64.NO_WRAP))));
            JSONObject body=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("parts",parts)));
            try(OutputStream os=con.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int code=con.getResponseCode();BufferedReader br=new BufferedReader(new InputStreamReader(code<400?con.getInputStream():con.getErrorStream(),StandardCharsets.UTF_8));StringBuilder raw=new StringBuilder();String line;while((line=br.readLine())!=null)raw.append(line);
            if(code>=400)throw new Exception("Gemini "+code);
            String answer=new JSONObject(raw.toString()).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
            String request="راستی‌آزمایی تحلیل حقوقی سند «"+name+"» در پرونده «"+caseName+"»";
            String reviewed=reviewWithDeepSeek(request,answer);if(reviewed.equals(answer))throw new Exception("بازبینی DeepSeek انجام نشد.");String finalText=reviewWithOpenAI(request,reviewed);if(finalText.equals(reviewed))throw new Exception("بازبینی OpenAI انجام نشد.");
            String label="تحلیل سند: "+name+"\n✓ استخراج Gemini\n✓ بازبینی DeepSeek\n✓ تأیید نهایی OpenAI\n\n";
            String result=label+finalText;
            runOnUiThread(()->{aiLog.setText(result);add("analyses",result);refresh();message("تحلیل سند ذخیره شد.");});
        }catch(Exception e){String err=e.getMessage()==null?"خطای نامشخص":e.getMessage();runOnUiThread(()->aiLog.setText("تحلیل سند انجام نشد: "+err+"\nنوع فایل، حجم، اینترنت، کلید و سهمیه API را بررسی کنید."));}}).start();
    }
    private byte[] readDocument(Uri uri,long max) throws Exception{
        try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new Exception("file");byte[] buf=new byte[8192];int n;long total=0;
            while((n=in.read(buf))!=-1){total+=n;if(total>max)throw new Exception("large");out.write(buf,0,n);}return out.toByteArray();
        }
    }
    private String getDisplayName(Uri uri){
        try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(c!=null&&c.moveToFirst())return c.getString(0);
        }catch(Exception ignored){}String x=uri.getLastPathSegment();return x==null?"سند":x;
    }
    private long getDocumentSize(Uri uri){
        try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){
            if(c!=null&&c.moveToFirst()&&!c.isNull(0))return c.getLong(0);
        }catch(Exception ignored){}return -1;
    }

    private void askAi(){
        String question=aiInput.getText().toString().trim();if(question.isEmpty()){message("ابتدا موضوع یا دستور متن را بنویسید.");return;}
        SharedPreferences p=getSharedPreferences("mose_private_settings",MODE_PRIVATE);
        String deep=p.getString("deepseek_key",""),open=p.getString("openai_key","");
        if(deep.isEmpty()||open.isEmpty()){message("برای نوشتن هوشمند، اتصال DeepSeek و OpenAI الزامی است. دکمه اتصال الزامی را بزنید.");configureReviewers();return;}
        new AlertDialog.Builder(this).setTitle("تأیید تولید متن").setMessage("DeepSeek متن را بنویسد و سپس OpenAI آن را بازبینی نهایی کند؟")
                .setNegativeButton("خیر",null).setPositiveButton("بله، شروع شود",(d,w)->sendQuestion(question)).show();
    }
    private void sendQuestion(String question){
        aiInput.setText("");aiLog.setText("DeepSeek در حال نوشتن متن حقوقی است…");
        new Thread(()->{try{
            String draft=writeWithDeepSeek(question);
            runOnUiThread(()->aiLog.setText("متن اولیه نوشته شد؛ OpenAI در حال بازبینی نهایی است…"));
            String finalText=reviewWithOpenAI(question,draft);
            if(finalText.equals(draft))throw new Exception("بازبینی OpenAI انجام نشد؛ کلید یا اعتبار API را بررسی کنید.");
            String result="✓ نگارش اولیه DeepSeek\n✓ بازبینی نهایی OpenAI\n\n"+finalText;
            runOnUiThread(()->{aiLog.setText(result);add("drafts",result);refresh();message("متن نهایی نوشته و در پرونده ذخیره شد.");});
        }catch(Exception e){String err=e.getMessage()==null?"خطای نامشخص":e.getMessage();runOnUiThread(()->aiLog.setText("تولید متن انجام نشد: "+err));}}).start();
    }
    private String writeWithDeepSeek(String question) throws Exception{
        String key=getSharedPreferences("mose_private_settings",MODE_PRIVATE).getString("deepseek_key","");
        URL u=new URL("https://api.deepseek.com/chat/completions");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(20000);c.setReadTimeout(90000);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+key);
        String prompt="به عنوان نویسنده حرفه‌ای متون حقوقی ایران، درخواست زیر را دقیق، رسمی، راست‌چین‌پذیر و قابل ویرایش تنظیم کن. هیچ واقعه، شماره، تاریخ، نام یا ماده قانونی را حدس نزن؛ جای اطلاعات ناقص را با [تکمیل شود] مشخص کن. ارکان دعوا، خواسته، ادله، دفاعیات و نتیجه‌گیری را متناسب با نوع متن کامل کن. درخواست: "+question;
        JSONObject body=new JSONObject().put("model","deepseek-chat").put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",prompt)));
        try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=c.getResponseCode();BufferedReader b=new BufferedReader(new InputStreamReader(code<400?c.getInputStream():c.getErrorStream(),StandardCharsets.UTF_8));StringBuilder raw=new StringBuilder();String l;while((l=b.readLine())!=null)raw.append(l);
        if(code>=400)throw new Exception("خطای DeepSeek "+code+": "+raw.substring(0,Math.min(250,raw.length())));
        return new JSONObject(raw.toString()).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
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
        String body;
        switch(type){
            case "لایحه": body="ریاست محترم مرجع رسیدگی\nبا سلام و احترام،\nدر خصوص پرونده و موضوع یادشده، مطالب و دفاعیات زیر به استحضار می‌رسد:\n\n۱. شرح ماوقع: "+facts+"\n\n۲. دلایل و مستندات:\n- قراردادها، مکاتبات و اسناد موجود در پرونده\n- رسیدها، نظریات کارشناسی و سایر ادله قابل ارائه\n\n۳. دفاعیات و استدلال حقوقی:\nبا توجه به اسناد موجود، تقاضا می‌شود رابطه حقوقی طرفین، تعهدات قراردادی، نحوه اجرا و آثار تخلف احتمالی بررسی شود. استناد قانونی قطعی پس از تطبیق اسناد با قوانین لازم‌الاجرا تکمیل خواهد شد.\n\n۴. خواسته:\nرسیدگی دقیق، بررسی دلایل و صدور تصمیم شایسته قانونی مورد تقاضاست.";break;
            case "دادخواست": body="ریاست محترم دادگاه صالح\nبا سلام،\nخواهان: [تکمیل شود]\nخوانده: [تکمیل شود]\nخواسته و بهای آن: "+facts+"\n\nدلایل و منضمات:\n۱. قرارداد و مکاتبات\n۲. رسیدها و اسناد پرداخت یا تحویل\n۳. نظریه کارشناسی و سایر ادله\n\nشرح دادخواست:\nبا توجه به رابطه حقوقی طرفین و اسناد پرونده، خوانده از اجرای تعهدات خودداری یا تخلف کرده است. تقاضای رسیدگی و صدور حکم متناسب با خواسته، به انضمام خسارات قانونی در صورت وجود شرایط، مورد استدعاست.";break;
            case "اظهارنامه": body="مخاطب محترم،\nموضوع اظهارنامه: "+facts+"\n\nبدین‌وسیله رسماً اعلام می‌شود ظرف مهلت قانونی/متعارف نسبت به انجام تعهد، ارائه پاسخ و جلوگیری از ورود خسارت اقدام کنید. این اظهارنامه به‌منظور مطالبه رسمی حق، حفظ ادله و جلوگیری از تضییع حقوق ارسال می‌شود. جزئیات تعهد، مهلت و مستندات باید پیش از ثبت تکمیل شود.";break;
            case "شکواییه": body="ریاست محترم دادسرای عمومی و انقلاب\nبا سلام،\nشاکی: [تکمیل شود]\nمشتکی‌عنه: [تکمیل شود]\nموضوع شکایت: "+facts+"\n\nشرح واقعه:\nواقعه مورد شکایت بر اساس اسناد و قرائن موجود رخ داده است. تقاضای انجام تحقیقات، استعلام‌های لازم، بررسی اصالت مدارک، تحقیق از مطلعان و اتخاذ تصمیم قانونی مورد استدعاست. عنوان کیفری و مواد قانونی باید پس از احراز دقیق ارکان قانونی، مادی و معنوی تعیین شود.";break;
            default: body="ریاست محترم مرجع صالح\nموضوع: درخواست کارشناسی و تأمین دلیل\n\nبا سلام،\nبا توجه به احتمال تغییر، زوال یا دشوار شدن دسترسی به دلایل مرتبط با موضوع «"+facts+"»، تقاضای حفظ وضعیت موجود، بازدید، صورت‌برداری، بررسی اسناد و ارجاع امر به کارشناس رسمی در رشته مرتبط را دارم. تعیین دقیق محل، موضوع کارشناسی و دلایل فوریت پیش از ثبت تکمیل شود.";
        }
        String draft="پیش‌نویس "+type+"\n\n"+body+"\n\n⚠ این نسخه محلی است و هنوز تأیید چندمدلی نشده است.";
        add("drafts",draft);refresh();
        SharedPreferences ai=getSharedPreferences("mose_private_settings",MODE_PRIVATE);String dk=ai.getString("deepseek_key",""),ok=ai.getString("openai_key","");
        if(dk.isEmpty()||ok.isEmpty()){message(draft+"\n\nنسخه اولیه ذخیره شد. برای تکمیل هوشمند، اتصال الزامی DeepSeek و OpenAI را ثبت کنید.");return;}
        new AlertDialog.Builder(this).setTitle("پیش‌نویس ذخیره شد").setMessage("نسخه اولیه نوشته و ذخیره شد. برای تکمیل حرفه‌ای و بازبینی چندمدلی ارسال شود؟")
                .setNegativeButton("فعلاً نه",(d,w)->message(draft)).setPositiveButton("تکمیل هوشمند",(d,w)->{
                    aiInput.setText("یک "+type+" حرفه‌ای و قابل ویرایش بر اساس حقوق ایران تنظیم کن. موضوع و اطلاعات: "+facts+"\nپیش‌نویس محلی: "+draft);askAi();
                }).show();
    }
    private void search(String q){
        StringBuilder out=new StringBuilder();
        for(String x:get("notes"))if(x.contains(q))out.append("یادداشت: ").append(x).append("\n\n");
        for(String x:get("drafts"))if(x.contains(q))out.append("پیش‌نویس: ").append(x).append("\n\n");
        for(String x:get("analyses"))if(x.contains(q))out.append("تحلیل سند: ").append(x).append("\n\n");
        message(out.length()==0?"نتیجه‌ای پیدا نشد.":out.toString());
    }
    private void refresh(){
        if(timeline==null)return;timeline.removeAllViews();
        Set<String> docs=get("docs"),notes=get("notes"),drafts=get("drafts"),analyses=get("analyses");
        if(docs.isEmpty()&&notes.isEmpty()&&drafts.isEmpty()&&analyses.isEmpty()){TextView e=text("هنوز سند یا یادداشتی در این پرونده نیست.",14,Color.rgb(155,178,191),Typeface.NORMAL);e.setGravity(Gravity.RIGHT);timeline.addView(e);return;}
        for(String x:docs)addRow("📎 سند","PDF یا تصویر ذخیره‌شده",v->openUri(x));
        for(String x:notes)addRow("✍ یادداشت",x,null);
        for(String x:drafts)addRow("⚖ پیش‌نویس حقوقی",x,v->message(x));
        for(String x:analyses)addRow("🔎 تحلیل سند",x,v->message(x));
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
