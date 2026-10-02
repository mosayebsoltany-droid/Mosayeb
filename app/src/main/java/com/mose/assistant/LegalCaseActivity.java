package com.mose.assistant;

import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.mose.assistant.data.MoseLegalDatabase;
import com.mose.assistant.data.LegalDao;
import com.mose.assistant.data.LegalCaseEntity;
import com.mose.assistant.data.CaseItemEntity;
import com.mose.assistant.data.LegalDraftEntity;
import java.util.List;
import com.mose.assistant.legal.ProfessionalInterviewController;

public class LegalCaseActivity extends AppCompatActivity {
    private static final int NAVY=Color.rgb(2,23,39),CARD=Color.rgb(16,42,61),GOLD=Color.rgb(230,181,76),CYAN=Color.rgb(47,214,190);
    private SharedPreferences store; private String caseId,caseName; private LinearLayout timeline;
    private EditText aiInput; private TextView aiLog,profileSummary; private MoseLegalDatabase legalDb;
    private final ActivityResultLauncher<String[]> picker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{
        if(uri==null)return;
        try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        add("docs",uri.toString());refresh();
        new AlertDialog.Builder(this).setTitle("سند ذخیره شد").setMessage("سند در پرونده «"+caseName+"» ذخیره شد. اگر PDF متنی است، اکنون روی همین گوشی استخراج شود؟")
                .setNegativeButton("فعلاً نه",null).setPositiveButton("استخراج محلی",(d,w)->analyzeLocalPdf(uri,getDisplayName(uri))).show();
    });

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);PDFBoxResourceLoader.init(getApplicationContext());getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);
        caseId=getIntent().getStringExtra("case_id");caseName=getIntent().getStringExtra("case_name");
        if(caseId==null||caseName==null){finish();return;}
        store=getSharedPreferences("lawyer_case_"+caseId,MODE_PRIVATE);legalDb=MoseLegalDatabase.get(this);migrateCaseToProfessionalDatabase();build();
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(NAVY);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(20),dp(20),dp(30));root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(root);
        TextView title=text("⚖ "+caseName,24,GOLD,Typeface.BOLD);title.setGravity(Gravity.RIGHT);root.addView(title);
        TextView sub=text("میز خصوصی پرونده — اسناد این پرونده از سایر پرونده‌ها جداست.",12,Color.rgb(148,173,188),Typeface.NORMAL);sub.setGravity(Gravity.RIGHT);sub.setPadding(0,dp(8),0,dp(10));root.addView(sub);
        profileSummary=text("",13,Color.rgb(205,218,226),Typeface.NORMAL);profileSummary.setGravity(Gravity.RIGHT);profileSummary.setPadding(dp(12),dp(10),dp(12),dp(10));profileSummary.setBackground(round(Color.rgb(10,34,50),14));root.addView(profileSummary,params(-1,-2,2,12));

        LinearLayout aiCard=new LinearLayout(this);aiCard.setOrientation(LinearLayout.VERTICAL);aiCard.setPadding(dp(16),dp(15),dp(16),dp(15));aiCard.setBackground(round(Color.rgb(16,42,61),20));
        TextView aiTitle=text("✦ وکیل هوشمند",19,GOLD,Typeface.BOLD);aiTitle.setGravity(Gravity.RIGHT);aiCard.addView(aiTitle);
        aiLog=text("سلطان، سؤال حقوقی یا دستور تنظیم متن را بفرمایید.",14,Color.rgb(247,231,207),Typeface.NORMAL);aiLog.setGravity(Gravity.RIGHT);aiLog.setPadding(0,dp(10),0,dp(10));aiCard.addView(aiLog);
        aiInput=new EditText(this);aiInput.setHint("سؤال یا دستور شما؛ فقط همین متن ارسال می‌شود");aiInput.setTextColor(Color.WHITE);aiInput.setHintTextColor(Color.rgb(177,150,130));aiInput.setTextDirection(View.TEXT_DIRECTION_RTL);aiInput.setMinLines(2);aiCard.addView(aiInput,params(-1,-2,4,8));
        Button freeWrite=button("ساخت و ذخیره متن رایگان");
        freeWrite.setOnClickListener(v->freeQuickDraft());aiCard.addView(freeWrite,params(-1,dp(52),8,0));
        TextView offline=text("نسخه رایگان و آفلاین — بدون کلید API و بدون ارسال اطلاعات",12,Color.rgb(148,173,188),Typeface.NORMAL);
        offline.setGravity(Gravity.RIGHT);offline.setPadding(0,dp(8),0,0);aiCard.addView(offline);
        root.addView(aiCard,params(-1,-2,4,14));

        root.addView(action("🗂 شناسنامه حرفه‌ای پرونده","مرجع، طرفین، شماره، موضوع، خواسته و وضعیت",v->editCaseProfile()));
        root.addView(action("📋 گزارش ساختاری پرونده","طرفین، وقایع، ادله و وضعیت اعتبار اطلاعات",v->showStructuredCaseReport()));
        root.addView(action("🧭 مصاحبه هوشمند پرونده","سؤال‌به‌سؤال، ذخیره پاسخ و گزارش نقاط ضعف",v->startProfessionalInterview()));
        root.addView(action("🕒 خط زمانی وقایع","ثبت تاریخ، رویداد و شرح هر اتفاق",v->addTimelineEvent()));
        root.addView(action("⚖ ماتریس ادعا و ادله","ارتباط هر ادعا با دلیل، ایراد و پاسخ",v->addEvidenceMatrix()));
        root.addView(action("📎 افزودن PDF یا تصویر","سند را فقط در همین پرونده نگهداری کن",v->picker.launch(new String[]{"application/pdf","image/*","text/*"})));
        root.addView(action("✍ ثبت یادداشت و اقدام","جلسه، تماس، مهلت یا اقدام بعدی",v->input("یادداشت پرونده","متن یادداشت",x->{add("notes",x);refresh();message("یادداشت با موفقیت در پرونده ذخیره شد.");})));
        root.addView(action("⚖ تنظیم متن حقوقی","لایحه، دادخواست، اظهارنامه یا شکواییه",v->chooseDraft()));
        root.addView(action("🔎 تحلیل رایگان PDF","استخراج متن و شناسایی اطلاعات روی همین گوشی",v->chooseDocumentForLocalAnalysis()));
        root.addView(action("⌕ جست‌وجوی داخل پرونده","جست‌وجو در یادداشت‌ها و پیش‌نویس‌ها",v->input("جست‌وجو","عبارت موردنظر",this::search)));

        TextView h=text("محتوای پرونده",18,Color.WHITE,Typeface.BOLD);h.setGravity(Gravity.RIGHT);h.setPadding(0,dp(20),0,dp(8));root.addView(h);
        timeline=new LinearLayout(this);timeline.setOrientation(LinearLayout.VERTICAL);root.addView(timeline);refresh();

        Button back=button("بازگشت به فهرست پرونده‌ها");back.setOnClickListener(v->finish());root.addView(back,params(-1,dp(54),20,0));setContentView(scroll);
    }

    private void startProfessionalInterview(){
        new ProfessionalInterviewController(this,legalDb,caseId,caseName,new ProfessionalInterviewController.Listener(){
            @Override public void onStatus(String text){aiLog.setText(text);}
            @Override public void onCompleted(String report){aiLog.setText(report);message(report);}
        }).start();
    }

    private void migrateCaseToProfessionalDatabase(){
        new Thread(()->{
            LegalDao dao=legalDb.legalDao();long now=System.currentTimeMillis();
            LegalCaseEntity existing=dao.findCase(caseId);
            if(existing==null){
                String subject=store.getString("profile_subject","");
                LegalCaseEntity c=new LegalCaseEntity(caseId,caseName,"UNCLASSIFIED",subject.isEmpty()?"UNCLASSIFIED":subject,store.getString("profile_role","CLAIMANT"),"INTAKE",now,now);
                c.authority=store.getString("profile_authority","");
                c.opponent=store.getString("profile_parties","");
                c.claim=store.getString("profile_claim","");
                dao.saveCase(c);
            }
            if(!store.getBoolean("room_migrated_v1",false)){
                migrateSet(dao,"EVENT",get("events"),"رویداد منتقل‌شده");
                migrateSet(dao,"NOTE",get("notes"),"یادداشت منتقل‌شده");
                migrateSet(dao,"EVIDENCE_LINK",get("evidence_matrix"),"ماتریس ادعا و دلیل");
                migrateSet(dao,"DOCUMENT_URI",get("docs"),"سند محلی پرونده");
                for(String d:get("drafts"))dao.saveNextDraft(caseId,"LEGACY",d,"منتقل‌شده از نسخه قبلی؛ نیازمند کنترل",now);
                for(String a:get("analyses"))addDbItem(dao,"ANALYSIS","تحلیل منتقل‌شده",a,"UNVERIFIED");
                store.edit().putBoolean("room_migrated_v1",true).apply();
            }
        }).start();
    }
    private void migrateSet(LegalDao dao,String kind,Set<String> values,String title){for(String v:values)addDbItem(dao,kind,title,v,"UNVERIFIED");}
    private void addDbItem(LegalDao dao,String kind,String title,String content,String status){
        long now=System.currentTimeMillis();dao.addItem(new CaseItemEntity(caseId,kind,title,content,status,now,now));
    }
    private void showStructuredCaseReport(){
        aiLog.setText("در حال خواندن پرونده ساختاری...");
        new Thread(()->{
            LegalDao dao=legalDb.legalDao();LegalCaseEntity c=dao.findCase(caseId);List<CaseItemEntity> items=dao.items(caseId);List<LegalDraftEntity> drafts=dao.drafts(caseId);
            StringBuilder out=new StringBuilder("گزارش ساختاری پرونده «").append(caseName).append("»\n\n");
            if(c!=null)out.append("حوزه: ").append(c.domain).append("\nنوع موضوع: ").append(c.matterType).append("\nسمت: ").append(c.userRole).append("\nمرحله: ").append(c.workflowState).append("\nطرف مقابل: ").append(blank(c.opponent)).append("\nخواسته: ").append(blank(c.claim)).append("\n\n");
            String last="";int n=1;for(CaseItemEntity i:items){if(!last.equals(i.kind)){last=i.kind;out.append("\n[").append(last).append("]\n");}out.append(n++).append(". ").append(i.title).append("\n").append(i.content).append("\nوضعیت اعتبار: ").append(i.verificationStatus).append("\n\n");}
            out.append("تعداد نسخه‌های پیش‌نویس: ").append(drafts.size()).append("\n\nراهنما: اطلاعات منتقل‌شده تا زمان تطبیق با اصل سند، بررسی‌نشده محسوب می‌شود.");
            runOnUiThread(()->{aiLog.setText(out.toString());message(out.toString());});
        }).start();
    }

    private void editCaseProfile(){
        ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(4),dp(14),dp(8));box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(box);
        EditText role=profileField("سمت شما در پرونده",store.getString("profile_role","")),parties=profileField("طرف مقابل و سمت او",store.getString("profile_parties","")),authority=profileField("مرجع رسیدگی و شعبه",store.getString("profile_authority","")),number=profileField("شماره پرونده / بایگانی",store.getString("profile_number","")),subject=profileField("موضوع اختلاف",store.getString("profile_subject","")),claim=profileField("خواسته یا نتیجه موردنظر",store.getString("profile_claim","")),status=profileField("وضعیت فعلی و اقدام بعدی",store.getString("profile_status",""));
        box.addView(role);box.addView(parties);box.addView(authority);box.addView(number);box.addView(subject);box.addView(claim);box.addView(status);
        new AlertDialog.Builder(this).setTitle("شناسنامه پرونده «"+caseName+"»").setView(scroll).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",(d,w)->{store.edit().putString("profile_role",role.getText().toString().trim()).putString("profile_parties",parties.getText().toString().trim()).putString("profile_authority",authority.getText().toString().trim()).putString("profile_number",number.getText().toString().trim()).putString("profile_subject",subject.getText().toString().trim()).putString("profile_claim",claim.getText().toString().trim()).putString("profile_status",status.getText().toString().trim()).apply();refresh();message("شناسنامه پرونده ذخیره شد.");}).show();
    }
    private EditText profileField(String hint,String value){EditText e=field(hint,1);e.setText(value);return e;}
    private void addTimelineEvent(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),0,dp(14),0);box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText date=field("تاریخ رویداد؛ مثال ۱۴۰۵/۰۶/۲۶",1),title=field("عنوان کوتاه رویداد",1),detail=field("شرح دقیق اتفاق، اشخاص و نتیجه",3);box.addView(date);box.addView(title);box.addView(detail);
        new AlertDialog.Builder(this).setTitle("ثبت رویداد پرونده").setView(box).setNegativeButton("انصراف",null).setPositiveButton("ثبت",(d,w)->{String da=date.getText().toString().trim(),ti=title.getText().toString().trim(),de=detail.getText().toString().trim();if(ti.isEmpty()||de.isEmpty()){message("عنوان و شرح رویداد الزامی است.");return;}add("events",(da.isEmpty()?"بدون تاریخ":da)+" | "+ti+" | "+de);refresh();message("رویداد در خط زمانی ثبت شد.");}).show();
    }
    private void addEvidenceMatrix(){
        ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),0,dp(14),0);box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(box);
        EditText claim=field("ادعا یا نکته‌ای که باید اثبات شود",2),evidence=field("دلیل و سند پشتیبان",2),objection=field("ایراد یا دفاع احتمالی طرف مقابل",2),answer=field("پاسخ شما و مدرک تکمیلی",2);box.addView(claim);box.addView(evidence);box.addView(objection);box.addView(answer);
        new AlertDialog.Builder(this).setTitle("افزودن ردیف ماتریس ادله").setView(scroll).setNegativeButton("انصراف",null).setPositiveButton("ثبت",(d,w)->{String c=claim.getText().toString().trim(),e=evidence.getText().toString().trim(),o=objection.getText().toString().trim(),an=answer.getText().toString().trim();if(c.isEmpty()||e.isEmpty()){message("ادعا و دلیل پشتیبان الزامی است.");return;}add("evidence_matrix","ادعا: "+c+"\nدلیل: "+e+"\nایراد احتمالی: "+(o.isEmpty()?"ثبت نشده":o)+"\nپاسخ: "+(an.isEmpty()?"نیازمند تکمیل":an));refresh();message("ردیف ادله ذخیره شد.");}).show();
    }
    private void updateProfileSummary(){
        if(profileSummary==null)return;String subject=store.getString("profile_subject",""),authority=store.getString("profile_authority",""),number=store.getString("profile_number",""),status=store.getString("profile_status","");
        if(subject.isEmpty()&&authority.isEmpty()&&number.isEmpty()&&status.isEmpty()){profileSummary.setText("شناسنامه پرونده هنوز تکمیل نشده است.");return;}
        profileSummary.setText("موضوع: "+blank(subject)+"\nمرجع: "+blank(authority)+"\nشماره: "+blank(number)+"\nوضعیت: "+blank(status));
    }
    private String blank(String x){return x==null||x.trim().isEmpty()?"تکمیل نشده":x;}

    private void freeQuickDraft(){
        String q=aiInput.getText().toString().trim();
        if(q.isEmpty()){message("فقط دستور خود را بنویسید؛ مثال: یک دادخواست مطالبه خسارت برایم بنویس.");return;}
        if(q.contains("مهریه")){showMahriyehInterview(q);return;}
        if(q.contains("انجام تعهد")||q.contains("نقض قرارداد")||q.contains("خسارت قراردادی")||q.contains("مطالبه خسارت")||q.contains("قرارداد باریت")){showContractClaimInterview(q);return;}
        String type=detectDocumentType(q);
        String authority=blankForDraft(store.getString("profile_authority",""),"[مرجع رسیدگی تکمیل شود]");
        String parties=blankForDraft(store.getString("profile_parties",""),"[مشخصات و سمت طرفین تکمیل شود]");
        String subject=store.getString("profile_subject","").trim();
        if(subject.isEmpty())subject=cleanInstruction(q,type);
        String facts=buildCaseFacts(q);
        String evidence=buildCaseEvidence();
        String request=store.getString("profile_claim","").trim();
        if(request.isEmpty())request=inferRequest(type,subject);
        aiInput.setText("");
        createProfessionalDraft(type,authority,parties,subject,facts,evidence,request);
    }
    private void showContractClaimInterview(String instruction){
        ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(4),dp(14),dp(12));box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(box);
        EditText claimant=field("خواهان/متعهدله: نام، شناسه ملی یا کد ملی و سمت",2);
        EditText defendant=field("خوانده/متعهد: نام، شناسه ملی یا کد ملی و نشانی",2);
        EditText contract=field("شماره، تاریخ، موضوع قرارداد و طرفین",3);
        EditText obligation=field("تعهد دقیق خوانده: تحویل چه مال، پرداخت چه مبلغ یا انجام چه کار؟",3);
        EditText deadline=field("موعد اجرای تعهد و محل اجرای آن",2);
        EditText breach=field("نقض قرارداد دقیقاً چگونه و در چه تاریخی رخ داده است؟",4);
        EditText performance=field("تعهدات انجام‌شده از طرف شما و اسناد اثبات آن",3);
        EditText demand=field("اظهارنامه، مطالبه یا اخطار قبلی و تاریخ ابلاغ",2);
        EditText loss=field("نوع و مبلغ خسارت؛ وجه التزام قراردادی یا خسارت واقعی",3);
        EditText arbitration=field("آیا قرارداد شرط داوری یا مرجع اختصاصی دارد؟ متن آن",2);
        EditText evidence=field("دلایل: قرارداد، الحاقیه، فاکتور، نامه، پیام، رسید، باسکول، آزمایش، شاهد و...",4);
        EditText provisional=field("تأمین خواسته یا دستور موقت لازم است؟ موضوع و فوریت",3);
        box.addView(claimant);box.addView(defendant);box.addView(contract);box.addView(obligation);box.addView(deadline);box.addView(breach);box.addView(performance);box.addView(demand);box.addView(loss);box.addView(arbitration);box.addView(evidence);box.addView(provisional);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("مصاحبه دعوای قراردادی و خسارت").setView(scroll).setNegativeButton("انصراف",null).setPositiveButton("ساخت دادخواست",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String cl=claimant.getText().toString().trim(),de=defendant.getText().toString().trim(),co=contract.getText().toString().trim(),ob=obligation.getText().toString().trim(),br=breach.getText().toString().trim();
            if(cl.isEmpty()||de.isEmpty()||co.isEmpty()||ob.isEmpty()||br.isEmpty()){message("مشخصات طرفین، قرارداد، تعهد و نحوه نقض آن الزامی است.");return;}
            createContractClaimDraft(cl,de,co,ob,deadline.getText().toString().trim(),br,performance.getText().toString().trim(),demand.getText().toString().trim(),loss.getText().toString().trim(),arbitration.getText().toString().trim(),evidence.getText().toString().trim(),provisional.getText().toString().trim());
            dialog.dismiss();
        }));dialog.show();
    }
    private void createContractClaimDraft(String claimant,String defendant,String contract,String obligation,String deadline,String breach,String performance,String priorDemand,String loss,String arbitration,String evidence,String provisional){
        boolean hasArbitration=!arbitration.isEmpty()&&!arbitration.contains("ندارد");
        String authority=store.getString("profile_authority","").trim();
        if(authority.isEmpty())authority=hasArbitration?"[مرجع داوری مقرر در قرارداد پس از بررسی اعتبار و دامنه شرط]":"[دادگاه عمومی حقوقی صالح پس از بررسی صلاحیت محلی]";
        String claim="۱. الزام خوانده به اجرای کامل تعهد قراردادی: "+obligation+"\n"+
                "۲. محکومیت خوانده به جبران خسارات اثبات‌شده ناشی از عدم اجرا یا تأخیر در اجرا: "+blankForDraft(loss,"[نوع، مبنا و مبلغ خسارت تکمیل و اثبات شود]")+"\n"+
                "۳. پرداخت هزینه دادرسی، کارشناسی و سایر هزینه‌های قانونی قابل مطالبه پس از احراز";
        if(!provisional.isEmpty())claim+="\n۴. رسیدگی جداگانه به درخواست اقدام فوری: "+provisional;
        String draft="دادخواست الزام به انجام تعهد و مطالبه خسارت قراردادی — نسخه قابل ویرایش\n\nبسمه‌تعالی\n\nریاست محترم "+authority+"\n\nخواهان: "+claimant+"\nخوانده: "+defendant+"\n\nخواسته و بهای خواسته:\n"+claim+"\nبهای خواسته: [بر اساس ارزش تعهد و مبلغ خسارت تکمیل شود]\n\nدلایل و منضمات:\n۱. قرارداد و پیوست‌های آن: "+contract+"\n۲. اسناد اجرای تعهدات خواهان: "+blankForDraft(performance,"[تکمیل شود]")+"\n۳. اخطار یا مطالبه قبلی: "+blankForDraft(priorDemand,"[در صورت وجود تکمیل شود]")+"\n۴. سایر ادله: "+blankForDraft(evidence,"[فهرست و شماره‌گذاری شود]")+"\n۵. عنداللزوم ارجاع امر به کارشناسی و استعلام از مراجع مرتبط\n\nشرح دادخواست\nبا سلام و احترام،\nبه موجب "+contract+"، رابطه قراردادی میان طرفین ایجاد شده و خوانده متعهد گردیده است: "+obligation+"\n\nموعد و محل اجرای تعهد: "+blankForDraft(deadline,"[تکمیل شود]")+"\n\nخواهان تعهدات خود را حسب اسناد زیر انجام داده یا آمادگی اجرای آن را داشته است:\n"+blankForDraft(performance,"[اقدامات و اسناد انجام تعهد خواهان تکمیل شود]")+"\n\nبا این حال، خوانده تعهد قراردادی خود را به شرح زیر نقض کرده است:\n"+breach+"\n\nمطالبه و اخطار قبلی:\n"+blankForDraft(priorDemand,"[تاریخ و نحوه مطالبه تکمیل شود]")+"\n\nخسارات ادعایی و مبنای محاسبه:\n"+blankForDraft(loss,"[هر خسارت باید از حیث وقوع، مبلغ، رابطه سببیت و مبنای قراردادی یا قانونی اثبات شود]")+"\n\nوضعیت شرط داوری یا مرجع حل اختلاف:\n"+blankForDraft(arbitration,"شرطی اعلام نشده است؛ متن کامل قرارداد باید مجدداً کنترل شود.")+"\n\nبا توجه به اعتبار تعهدات قراردادی، لزوم اجرای مفاد توافق و مسئولیت ناشی از نقض اثبات‌شده تعهد، رسیدگی و صدور تصمیم نسبت به خواسته‌های فوق تقاضا می‌شود.\n\nنام و سمت نماینده خواهان: "+claimant+"\nتاریخ و امضا: [تکمیل شود]\n\nکنترل نهایی پیش از ثبت:\n• اصالت و امضای قرارداد و اختیار امضاکنندگان\n• شرط داوری و مرجع صالح\n• سررسید تعهد و اثبات مطالبه\n• انجام تعهدات متقابل خواهان\n• مبلغ و شیوه محاسبه خسارت و رابطه سببیت\n• نشانی دقیق طرفین، بهای خواسته و پیوست‌های مصدق\n\nهشدار: شماره ماده یا رأی قضایی بدون اتصال به منبع رسمی به این متن افزوده نشده است.";
        add("drafts",draft);aiLog.setText(draft);refresh();showOutputActions(draft);
    }

    private void showMahriyehInterview(String instruction){
        ScrollView scroll=new ScrollView(this);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(4),dp(14),dp(12));box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(box);
        EditText role=field("سمت شما: زوجه یا زوج؟",1);
        EditText wife=field("نام، نام خانوادگی و کد ملی زوجه",1);
        EditText husband=field("نام، نام خانوادگی و کد ملی زوج",1);
        EditText marriage=field("تاریخ عقد و شماره سند ازدواج/دفترخانه",2);
        EditText amount=field("مهریه دقیق؛ مثال: ۳۱۴ سکه تمام بهار آزادی",1);
        EditText condition=field("عندالمطالبه یا عندالاستطاعه",1);
        EditText demand=field("تمام مهریه یا چه مقدار مطالبه می‌شود؟",1);
        EditText paid=field("پرداخت، بذل یا وصول قبلی؛ اگر ندارد بنویسید ندارد",2);
        EditText assets=field("آیا تأمین خواسته یا توقیف اموال می‌خواهید؟ اموال شناخته‌شده",2);
        EditText evidence=field("مدارک موجود: سند ازدواج، شناسنامه، اجرائیه، رسید و...",3);
        box.addView(role);box.addView(wife);box.addView(husband);box.addView(marriage);box.addView(amount);box.addView(condition);box.addView(demand);box.addView(paid);box.addView(assets);box.addView(evidence);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("مصاحبه تخصصی مهریه").setView(scroll)
                .setNegativeButton("انصراف",null).setPositiveButton("ساخت متن کامل",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String ro=role.getText().toString().trim(),wi=wife.getText().toString().trim(),hu=husband.getText().toString().trim(),ma=marriage.getText().toString().trim(),am=amount.getText().toString().trim();
            if(ro.isEmpty()||wi.isEmpty()||hu.isEmpty()||ma.isEmpty()||am.isEmpty()){message("سمت شما، مشخصات زوجین، اطلاعات عقد و میزان دقیق مهریه الزامی است.");return;}
            createMahriyehDraft(ro,wi,hu,ma,am,condition.getText().toString().trim(),demand.getText().toString().trim(),paid.getText().toString().trim(),assets.getText().toString().trim(),evidence.getText().toString().trim());
            dialog.dismiss();
        }));dialog.show();
    }
    private void createMahriyehDraft(String role,String wife,String husband,String marriage,String amount,String condition,String demand,String paid,String assets,String evidence){
        boolean isHusband=role.contains("زوج")&&!role.contains("زوجه");
        String draft;
        if(isHusband){
            draft="لایحه دفاعیه در پرونده مطالبه مهریه — نسخه قابل ویرایش\n\nبسمه‌تعالی\n\nریاست محترم شعبه رسیدگی‌کننده دادگاه خانواده\n\nموضوع: لایحه دفاعیه در پاسخ به مطالبه مهریه\n\nزوجه/خواهان: "+wife+"\nزوج/خوانده: "+husband+"\nمشخصات نکاح: "+marriage+"\nمیزان مهریه مندرج در سند نکاحیه: "+amount+"\nنوع تعهد اعلامی: "+blankForDraft(condition,"[تکمیل شود]")+"\n\nبا سلام و احترام،\nاینجانب به‌عنوان زوج، ضمن پذیرش اصل مفاد سند رسمی نکاحیه در حدود اصالت و اعتبار آن، دفاعیات و توضیحات خود را بدون اسقاط هیچ‌یک از حقوق قانونی به شرح زیر تقدیم می‌کنم:\n\n۱. میزان مورد مطالبه: "+blankForDraft(demand,"[تکمیل شود]")+"\n۲. سوابق پرداخت، وصول یا بذل: "+blankForDraft(paid,"[تکمیل و مستند شود]")+"\n۳. توضیحات درباره اموال و تأمین خواسته: "+blankForDraft(assets,"[تکمیل شود]")+"\n۴. مدارک قابل ارائه: "+blankForDraft(evidence,"[پیوست‌ها تکمیل شود]")+"\n\nخواسته:\n۱. محاسبه دقیق میزان باقی‌مانده تعهد پس از کسر پرداخت‌ها یا وصول‌های اثبات‌شده؛\n۲. بررسی اصالت و دلالت تمام رسیدها و مستندات طرفین؛\n۳. اتخاذ تصمیم متناسب با وضعیت اثبات‌شده پرونده و مقررات لازم‌الاجرا؛\n۴. جلوگیری از محاسبه یا وصول مضاعف هر میزان که قبلاً پرداخت، وصول یا به‌طور معتبر بذل شده است.\n\nنام و امضا: "+husband+"\nتاریخ: [تکمیل شود]";
        }else{
            draft="دادخواست مطالبه مهریه — نسخه قابل ویرایش\n\nبسمه‌تعالی\n\nریاست محترم دادگاه خانواده صالح\n\nخواهان: "+wife+"\nخوانده: "+husband+"\nخواسته: مطالبه "+blankForDraft(demand,amount)+" از مهریه مندرج در سند رسمی ازدواج، به‌همراه هزینه‌های قانونی قابل مطالبه پس از احراز\nبهای خواسته: [بر اساس میزان مورد مطالبه و ضوابط روز تکمیل شود]\n\nدلایل و منضمات:\n۱. تصویر مصدق سند رسمی ازدواج: "+marriage+"\n۲. مدارک هویتی خواهان\n۳. "+blankForDraft(evidence,"سایر مدارک و استعلام‌های لازم") +"\n۴. عندالاقتضاء استعلام اموال و سوابق اجرایی مرتبط\n\nشرح دادخواست\nبا سلام و احترام،\nبر اساس سند رسمی ازدواج با مشخصات فوق، خوانده متعهد به تأدیه مهریه به میزان "+amount+" شده است. وضعیت تعهد در سند به صورت "+blankForDraft(condition,"[عندالمطالبه/عندالاستطاعه تکمیل شود]")+" اعلام شده است. میزان مورد مطالبه در این دادخواست "+blankForDraft(demand,amount)+" است.\n\nسوابق پرداخت، وصول یا بذل احتمالی: "+blankForDraft(paid,"موردی اعلام نشده است")+"\n\nبا وجود استحقاق خواهان بر اساس مفاد سند نکاحیه، میزان مورد مطالبه تاکنون حسب اظهارات ارائه‌شده وصول نشده است. لذا تقاضای رسیدگی، بررسی سند و سوابق پرداخت و صدور حکم نسبت به میزان اثبات‌شده و باقی‌مانده مهریه را دارم.\n\nدرخواست‌های تکمیلی:\n۱. صدور حکم بر محکومیت خوانده به پرداخت میزان باقی‌مانده و اثبات‌شده مهریه؛\n۲. احتساب و کسر هر مبلغ یا مالی که پرداخت یا وصول آن با دلیل معتبر ثابت شود؛\n۳. تصمیم نسبت به هزینه‌های قانونی قابل مطالبه پس از احراز؛\n۴. "+blankForDraft(assets,"در صورت وجود شرایط قانونی، بررسی درخواست تأمین خواسته و شناسایی اموال")+"\n\nنام و امضای خواهان: "+wife+"\nتاریخ: [تکمیل شود]\n\nکنترل نهایی پیش از ثبت: مرجع صالح، نشانی طرفین، بهای خواسته، وضعیت اجرای ثبتی قبلی، میزان دقیق مورد مطالبه و پیوست‌های مصدق باید بررسی شود.";
        }
        add("drafts",draft);aiLog.setText(draft);refresh();showOutputActions(draft);
    }

    private String detectDocumentType(String q){
        if(q.contains("شکواییه")||q.contains("شکایت کیفری")||q.contains("جرم"))return "شکواییه کیفری";
        if(q.contains("اظهارنامه"))return "اظهارنامه رسمی";
        if(q.contains("تامین دلیل")||q.contains("تأمین دلیل")||q.contains("کارشناسی"))return "تأمین دلیل و کارشناسی";
        if(q.contains("لایحه")||q.contains("دفاع"))return "لایحه دفاعیه";
        return "دادخواست حقوقی";
    }
    private String cleanInstruction(String q,String type){
        String x=q.replace("برایم","").replace("برام","").replace("بنویس","").replace("تنظیم کن","")
                .replace("یک","").replace(type,"").replace("دادخواست","").replace("لایحه","")
                .replace("شکواییه","").replace("اظهارنامه","").trim();
        return x.isEmpty()?"موضوع مندرج در دستور کاربر":x;
    }
    private String blankForDraft(String x,String fallback){return x==null||x.trim().isEmpty()?fallback:x.trim();}
    private String buildCaseFacts(String instruction){
        StringBuilder b=new StringBuilder();
        b.append("دستور کاربر: ").append(instruction).append("\n");
        Set<String> events=get("events");
        if(!events.isEmpty()){b.append("\nترتیب وقایع ثبت‌شده در پرونده:\n");for(String e:events)b.append("• ").append(e).append("\n");}
        Set<String> notes=get("notes");
        if(!notes.isEmpty()){b.append("\nیادداشت‌های مرتبط:\n");for(String n:notes)b.append("• ").append(n).append("\n");}
        String status=store.getString("profile_status","").trim();
        if(!status.isEmpty())b.append("\nوضعیت فعلی: ").append(status);
        return b.toString();
    }
    private String buildCaseEvidence(){
        StringBuilder b=new StringBuilder();
        for(String e:get("evidence_matrix"))b.append(e).append("\n");
        int i=1;for(String ignored:get("docs"))b.append("سند پیوست شماره ").append(i++).append("\n");
        if(b.length()==0)b.append("[دلایل و پیوست‌ها هنوز ثبت نشده است]");
        return b.toString();
    }
    private String inferRequest(String type,String subject){
        if(type.equals("شکواییه کیفری"))return "انجام تحقیقات، جمع‌آوری ادله و تعقیب قانونی مرتکب یا مرتکبان پس از احراز ارکان قانونی";
        if(type.equals("اظهارنامه رسمی"))return "انجام تعهد و ارائه پاسخ رسمی در مهلت قانونی، با حفظ کلیه حقوق اظهارکننده";
        if(type.equals("تأمین دلیل و کارشناسی"))return "ثبت فوری وضعیت موجود، صورت‌برداری از ادله و ارجاع امر به کارشناس رسمی رشته مرتبط";
        if(type.equals("لایحه دفاعیه"))return "رد ادعاهای فاقد دلیل طرف مقابل و اتخاذ تصمیم شایسته بر پایه اسناد پرونده";
        return "صدور حکم شایسته نسبت به "+subject+" به انضمام خسارات و هزینه‌های قانونی پس از احراز";
    }
    private void showOutputActions(String draft){
        new AlertDialog.Builder(this).setTitle("خروجی آماده است").setMessage(draft)
                .setNeutralButton("کپی",(d,w)->{ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("متن حقوقی",draft));message("متن کپی شد.");})
                .setNegativeButton("اشتراک‌گذاری",(d,w)->{Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_SUBJECT,"متن حقوقی پرونده "+caseName);send.putExtra(Intent.EXTRA_TEXT,draft);startActivity(Intent.createChooser(send,"ارسال خروجی"));})
                .setPositiveButton("ذخیره شد",null).show();
    }
    private void chooseDocumentForLocalAnalysis(){
        Set<String> docs=get("docs");
        if(docs.isEmpty()){message("ابتدا یک فایل PDF به پرونده اضافه کنید.");return;}
        String[] uris=docs.toArray(new String[0]);String[] names=new String[uris.length];
        for(int i=0;i<uris.length;i++)names[i]=(i+1)+" — "+getDisplayName(Uri.parse(uris[i]));
        new AlertDialog.Builder(this).setTitle("انتخاب PDF").setItems(names,(d,which)->analyzeLocalPdf(Uri.parse(uris[which]),names[which])).show();
    }
    private void analyzeLocalPdf(Uri uri,String name){
        String mime=getContentResolver().getType(uri);
        if(mime!=null&&!mime.equals("application/pdf")){message("در نسخه رایگان، استخراج متن فقط برای فایل PDF متنی فعال است.");return;}
        aiLog.setText("در حال استخراج متن PDF روی گوشی…");
        new Thread(()->{try(InputStream in=getContentResolver().openInputStream(uri);PDDocument doc=PDDocument.load(in)){
            String extracted=new PDFTextStripper().getText(doc).trim();
            if(extracted.isEmpty())throw new Exception("این PDF اسکن تصویری است و متن قابل استخراج ندارد.");
            String[] keys={"قرارداد","خواهان","خوانده","شاکی","مشتکی","تعهد","مبلغ","تاریخ","مهلت","داوری","کارشناس","امضا","فسخ","خسارت"};
            StringBuilder found=new StringBuilder();for(String k:keys)if(extracted.contains(k))found.append("• ").append(k).append("\n");
            String preview=extracted.length()>12000?extracted.substring(0,12000)+"\n… [ادامه متن به علت طول زیاد نمایش داده نشد]":extracted;
            String result="تحلیل رایگان و محلی PDF\nنام: "+getDisplayName(uri)+"\nتعداد صفحات: "+doc.getNumberOfPages()+"\nتعداد نویسه‌های استخراج‌شده: "+extracted.length()+"\n\nکلیدواژه‌های حقوقی پیدا‌شده:\n"+(found.length()==0?"مورد مشخصی پیدا نشد.\n":found.toString())+"\nمتن استخراج‌شده:\n"+preview+"\n\n⚠ این گزارش استخراج ماشینی است و تأیید حقوقی هوش مصنوعی محسوب نمی‌شود.";
            runOnUiThread(()->{aiLog.setText(result);add("analyses",result);refresh();message("متن PDF استخراج و در پرونده ذخیره شد.");});
        }catch(Exception e){String err=e.getMessage()==null?"فایل قابل خواندن نیست.":e.getMessage();runOnUiThread(()->aiLog.setText("تحلیل محلی انجام نشد: "+err+"\nاگر PDF اسکن‌شده است، نسخه متنی یا تصاویر صفحات را استفاده کنید."));}}).start();
    }

    private String getDisplayName(Uri uri){
        try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(c!=null&&c.moveToFirst())return c.getString(0);
        }catch(Exception ignored){}String x=uri.getLastPathSegment();return x==null?"سند":x;
    }
    private void chooseDraft(){
        String[] types={"لایحه دفاعیه","دادخواست حقوقی","اظهارنامه رسمی","شکواییه کیفری","تأمین دلیل و کارشناسی"};
        new AlertDialog.Builder(this).setTitle("تنظیم حرفه‌ای متن حقوقی").setItems(types,(d,which)->showProfessionalForm(types[which])).show();
    }
    private EditText field(String hint,int lines){
        EditText e=new EditText(this);e.setHint(hint);e.setTextDirection(View.TEXT_DIRECTION_RTL);e.setGravity(Gravity.RIGHT|Gravity.TOP);e.setMinLines(lines);e.setTextColor(Color.BLACK);e.setHintTextColor(Color.DKGRAY);return e;
    }
    private void showProfessionalForm(String type){
        ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(4),dp(14),dp(8));box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(box);
        EditText authority=field("مرجع مخاطب؛ مثال: ریاست شعبه ... دادگاه/دادسرا",1);
        EditText parties=field("مشخصات و سمت طرفین؛ مثال: شرکت ... به طرفیت ...",2);
        EditText subject=field("موضوع و خواسته دقیق",2);
        EditText facts=field("شرح کامل و زمان‌بندی‌شده ماجرا؛ تاریخ‌ها، مبالغ، اقدامات و تخلف",5);
        EditText evidence=field("دلایل و پیوست‌ها؛ قرارداد، نامه، رسید، شاهد، کارشناسی و ...",3);
        EditText request=field("درخواست نهایی از مرجع؛ دقیق و شماره‌گذاری‌شده",3);
        box.addView(authority);box.addView(parties);box.addView(subject);box.addView(facts);box.addView(evidence);box.addView(request);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(type+" — پرونده "+caseName).setView(scroll)
                .setNegativeButton("انصراف",null).setPositiveButton("ساخت متن",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String su=subject.getText().toString().trim(),fa=facts.getText().toString().trim();
            if(su.isEmpty()||fa.isEmpty()){message("موضوع و شرح ماوقع الزامی است.");return;}
            String au=authority.getText().toString().trim(),pa=parties.getText().toString().trim(),ev=evidence.getText().toString().trim(),re=request.getText().toString().trim();
            if(au.isEmpty())au="[مرجع رسیدگی تکمیل شود]";if(pa.isEmpty())pa="[مشخصات طرفین تکمیل شود]";if(ev.isEmpty())ev="[دلایل و پیوست‌ها تکمیل شود]";if(re.isEmpty())re="[خواسته نهایی تکمیل شود]";
            createProfessionalDraft(type,au,pa,su,fa,ev,re);dialog.dismiss();
        }));dialog.show();
    }
    private void createProfessionalDraft(String type,String authority,String parties,String subject,String facts,String evidence,String request){
        String body;
        if(type.equals("لایحه دفاعیه")){
            body="بسمه‌تعالی\n\n"+authority+"\n\nموضوع: تقدیم لایحه در پرونده «"+caseName+"» — "+subject+"\n\nبا سلام و احترام،\nاینجانب/این شرکت با مشخصات و سمت زیر:\n"+parties+"\nدر مقام تبیین موضوع و دفاع از حقوق قانونی خود، مطالب زیر را به استحضار می‌رسانم:\n\nالف) شرح منظم ماوقع\n"+facts+"\n\nب) دلایل و مستندات\n"+numberLines(evidence)+"\n\nپ) تحلیل و دفاع\n۱. رسیدگی به اختلاف باید بر پایه مفاد واقعی اسناد، ترتیب زمانی وقایع و تعهدات هر یک از طرفین انجام شود.\n۲. هر ادعا باید با سند، اقرار، مکاتبه، رسید، گواهی، نظریه کارشناسی یا سایر ادله قابل استناد تطبیق داده شود.\n۳. چنانچه طرف مقابل برخلاف قرارداد یا تعهدات خود اقدام کرده باشد، آثار آن باید با توجه به نوع تعهد، میزان خسارت و رابطه سببیت بررسی شود.\n۴. هرگونه ادعای فاقد دلیل یا مغایر با اسناد پیوست، مورد ایراد و انکار است و تقاضای بررسی اصالت و اعتبار آن می‌شود.\n\nت) خواسته و نتیجه‌گیری\n"+numberLines(request)+"\n\nبا عنایت به مراتب و مستندات پیوست، تقاضای رسیدگی دقیق، توجه به مجموع ادله و اتخاذ تصمیم شایسته و منطبق با قانون را دارم.\n\nنام و سمت: [تکمیل شود]\nتاریخ و امضا: [تکمیل شود]";
        }else if(type.equals("دادخواست حقوقی")){
            body="بسمه‌تعالی\n\nریاست محترم "+authority+"\n\nخواهان: "+parties+"\nخوانده/خواندگان: [مشخصات دقیق و نشانی تکمیل شود]\nخواسته و بهای خواسته: "+subject+"\n\nدلایل و منضمات:\n"+numberLines(evidence)+"\n\nشرح دادخواست\nبا سلام و احترام،\nشرح رابطه حقوقی طرفین و وقایع مؤثر به ترتیب زمانی چنین است:\n"+facts+"\n\nبا توجه به اسناد و تعهدات موجود، رفتار یا خودداری خوانده موجب تضییع حق و ورود خسارت شده است. تعیین مسئولیت نهایی مستلزم بررسی مفاد قرارداد، نحوه اجرای تعهدات، مکاتبات، پرداخت‌ها یا تحویل‌ها و عنداللزوم ارجاع امر به کارشناسی است.\n\nبنا بر مراتب فوق، صدور حکم نسبت به موارد زیر تقاضا می‌شود:\n"+numberLines(request)+"\nهمچنین در صورت احراز شرایط قانونی، هزینه دادرسی، حق‌الزحمه کارشناسی و سایر خسارات قابل مطالبه نیز مورد درخواست است.\n\nنام و امضا: [تکمیل شود]\nتاریخ: [تکمیل شود]";
        }else if(type.equals("اظهارنامه رسمی")){
            body="بسمه‌تعالی\n\nاظهارکننده: "+parties+"\nمخاطب: [نام، شناسه/کد ملی و نشانی تکمیل شود]\nموضوع: "+subject+"\n\nمخاطب محترم،\nبا سلام؛\nبه موجب این اظهارنامه و با حفظ کلیه حقوق قانونی، مراتب زیر رسماً به شما اعلام می‌شود:\n\n"+facts+"\n\nمستندات این اعلام و مطالبه عبارت‌اند از:\n"+numberLines(evidence)+"\n\nلذا از شما درخواست می‌شود:\n"+numberLines(request)+"\nاز تاریخ ابلاغ، اقدامات و پاسخ شما به‌عنوان بخشی از سوابق و ادله موضوع نگهداری خواهد شد. ارسال این اظهارنامه به معنای اسقاط هیچ‌یک از حقوق، مطالبات یا طرق قانونی اظهارکننده نیست.\n\nنام و سمت اظهارکننده: [تکمیل شود]\nتاریخ و امضا: [تکمیل شود]";
        }else if(type.equals("شکواییه کیفری")){
            body="بسمه‌تعالی\n\nریاست محترم "+authority+"\n\nشاکی: "+parties+"\nمشتکی‌عنه/مشتکی‌عنهم: [مشخصات و نشانی تکمیل شود]\nموضوع شکایت: "+subject+"\nزمان و محل وقوع: [تکمیل شود]\n\nدلایل و مستندات:\n"+numberLines(evidence)+"\n\nشرح شکایت\nبا سلام و احترام،\nوقایع موضوع شکایت به ترتیب زمانی و بدون افزودن مطالب اثبات‌نشده به شرح زیر است:\n"+facts+"\n\nبا توجه به اینکه تشخیص عنوان کیفری نهایی و احراز ارکان قانونی، مادی و معنوی رفتار در صلاحیت مرجع قضایی است، تقاضا دارم ضمن حفظ و جمع‌آوری ادله، اقدامات زیر انجام شود:\n"+numberLines(request)+"\n- تحقیق از مطلعان و اشخاص مرتبط؛\n- استعلام و بررسی اسناد، مکاتبات، تراکنش‌ها و سوابق مرتبط؛\n- بررسی اصالت مدارک و عنداللزوم ارجاع امر به کارشناسی؛\n- جلوگیری از امحا، انتقال یا دستکاری ادله در صورت وجود شرایط قانونی؛\n- تعقیب مرتکب یا مرتکبان در صورت احراز وقوع جرم.\n\nاین شکایت صرفاً بر وقایع و مدارک قابل ارائه استوار است و از انتساب قطعی عناوین اثبات‌نشده خودداری شده است.\n\nنام و امضای شاکی: [تکمیل شود]\nتاریخ: [تکمیل شود]";
        }else{
            body="بسمه‌تعالی\n\nریاست محترم "+authority+"\n\nمتقاضی: "+parties+"\nموضوع: درخواست تأمین دلیل و ارجاع امر به کارشناسی درباره «"+subject+"»\n\nبا سلام و احترام،\nبه استحضار می‌رساند اوضاع و وقایع مرتبط با موضوع به شرح زیر است:\n"+facts+"\n\nدلایل و مدارک فعلی:\n"+numberLines(evidence)+"\n\nبا توجه به احتمال تغییر وضعیت موجود، زوال آثار، دشوار شدن دسترسی به مدارک یا ضرورت ثبت فوری کیفیت و کمیت موضوع، تقاضا می‌شود بدون ورود ماهوی به اصل اختلاف، وضعیت فعلی مشاهده، صورت‌برداری و حفظ شود و در صورت لزوم کارشناس رسمی رشته مرتبط تعیین گردد.\n\nمحورهای پیشنهادی کارشناسی:\n۱. بررسی و توصیف دقیق وضعیت موجود؛\n۲. تطبیق اسناد، مقادیر، کیفیت، تاریخ‌ها و اقدامات انجام‌شده؛\n۳. تعیین میزان اختلاف، کسری، خسارت یا هزینه حسب موضوع؛\n۴. ثبت تصاویر، نمونه‌ها، مشخصات فنی و اظهارات اشخاص حاضر؛\n۵. پاسخ به پرسش‌های تخصصی مندرج در درخواست.\n\nخواسته نهایی:\n"+numberLines(request)+"\n\nنام و امضا: [تکمیل شود]\nتاریخ: [تکمیل شود]";
        }
        String draft=type+" — نسخه حرفه‌ای قابل ویرایش\n\n"+body+"\n\nیادآوری: پیش از ثبت رسمی، مشخصات هویتی، مرجع صالح، خواسته، بهای خواسته، مهلت‌ها و تطبیق نهایی پیوست‌ها کنترل شود.";
        add("drafts",draft);refresh();aiLog.setText(draft);showOutputActions(draft);
    }
    private String numberLines(String raw){
        String[] lines=raw.split("\\n");StringBuilder out=new StringBuilder();int n=1;
        for(String line:lines){String z=line.trim();if(!z.isEmpty())out.append(n++).append(". ").append(z).append("\n");}
        return out.length()==0?"[تکمیل شود]\n":out.toString();
    }
    private void search(String q){
        StringBuilder out=new StringBuilder();
        for(String x:get("notes"))if(x.contains(q))out.append("یادداشت: ").append(x).append("\n\n");
        for(String x:get("drafts"))if(x.contains(q))out.append("پیش‌نویس: ").append(x).append("\n\n");
        for(String x:get("events"))if(x.contains(q))out.append("رویداد: ").append(x).append("\n\n");
        for(String x:get("evidence_matrix"))if(x.contains(q))out.append("ادعا و دلیل: ").append(x).append("\n\n");
        for(String x:get("analyses"))if(x.contains(q))out.append("تحلیل سند: ").append(x).append("\n\n");
        message(out.length()==0?"نتیجه‌ای پیدا نشد.":out.toString());
    }
    private void refresh(){
        if(timeline==null)return;updateProfileSummary();timeline.removeAllViews();
        Set<String> docs=get("docs"),notes=get("notes"),drafts=get("drafts"),analyses=get("analyses"),events=get("events"),matrix=get("evidence_matrix");
        if(docs.isEmpty()&&notes.isEmpty()&&drafts.isEmpty()&&analyses.isEmpty()&&events.isEmpty()&&matrix.isEmpty()){TextView e=text("هنوز سند یا یادداشتی در این پرونده نیست.",14,Color.rgb(155,178,191),Typeface.NORMAL);e.setGravity(Gravity.RIGHT);timeline.addView(e);return;}
        for(String x:events)addRow("🕒 رویداد خط زمانی",x,v->message(x));
        for(String x:matrix)addRow("⚖ ادعا و دلیل",x,v->message(x));
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
