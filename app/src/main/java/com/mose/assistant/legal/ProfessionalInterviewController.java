package com.mose.assistant.legal;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.mose.assistant.data.CaseItemEntity;
import com.mose.assistant.data.LegalDao;
import com.mose.assistant.data.MoseLegalDatabase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProfessionalInterviewController {
    public interface Listener { void onStatus(String text); void onCompleted(String report); }

    private final AppCompatActivity activity;
    private final MoseLegalDatabase db;
    private final String caseId;
    private final String caseName;
    private final Listener listener;

    public ProfessionalInterviewController(AppCompatActivity activity,MoseLegalDatabase db,String caseId,String caseName,Listener listener){
        this.activity=activity;this.db=db;this.caseId=caseId;this.caseName=caseName;this.listener=listener;
    }

    public void start(){
        listener.onStatus("در حال آماده‌سازی مصاحبه هوشمند...");
        new Thread(()->{
            List<ProfessionalIntakeEngine.Question> questions=ProfessionalIntakeEngine.contractDispute();
            Map<String,String> answers=new LinkedHashMap<>();
            for(CaseItemEntity item:db.legalDao().itemsByKind(caseId,"INTAKE_ANSWER"))answers.put(item.title,item.content);
            activity.runOnUiThread(()->ask(questions,0,answers));
        }).start();
    }

    private void ask(List<ProfessionalIntakeEngine.Question> questions,int index,Map<String,String> answers){
        if(index>=questions.size()){finish(questions,answers);return;}
        ProfessionalIntakeEngine.Question q=questions.get(index);
        EditText input=new EditText(activity);input.setHint(q.prompt);input.setTextDirection(View.TEXT_DIRECTION_RTL);input.setGravity(Gravity.RIGHT|Gravity.TOP);input.setMinLines(4);input.setTextColor(Color.BLACK);input.setHintTextColor(Color.DKGRAY);
        String old=answers.get(q.id);if(old!=null)input.setText(old);
        LinearLayout wrap=new LinearLayout(activity);wrap.setPadding(dp(16),0,dp(16),0);wrap.addView(input,new LinearLayout.LayoutParams(-1,-2));
        AlertDialog dialog=new AlertDialog.Builder(activity)
                .setTitle("سؤال "+(index+1)+" از "+questions.size()+" — "+q.title+" ("+(q.required?"ضروری":"تکمیلی")+")")
                .setMessage(q.prompt).setView(wrap).setNegativeButton("ذخیره و توقف",null).setNeutralButton("قبلی",null).setPositiveButton("بعدی",null).create();
        dialog.setOnShowListener(x->{
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->{save(q,input.getText().toString().trim(),answers);dialog.dismiss();listener.onCompleted("پاسخ‌های ثبت‌شده محفوظ است؛ مصاحبه بعداً از همین مرحله قابل تکمیل است.");});
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{save(q,input.getText().toString().trim(),answers);dialog.dismiss();ask(questions,Math.max(0,index-1),answers);});
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String value=input.getText().toString().trim();if(q.required&&value.isEmpty()){input.setError("پاسخ ضروری است؛ اگر نمی‌دانید بنویسید «نمی‌دانم» تا نقص ثبت شود.");return;}save(q,value,answers);dialog.dismiss();ask(questions,index+1,answers);});
        });dialog.show();
    }

    private void save(ProfessionalIntakeEngine.Question q,String value,Map<String,String> answers){
        if(value.isEmpty())return;answers.put(q.id,value);
        new Thread(()->{
            long now=System.currentTimeMillis();
            db.legalDao().addItem(new CaseItemEntity(caseId,"INTAKE_ANSWER",q.id,value,ProfessionalIntakeEngine.isMissing(value)?"MISSING":"USER_REPORTED",now,now));
        }).start();
    }

    private void finish(List<ProfessionalIntakeEngine.Question> questions,Map<String,String> answers){
        StringBuilder report=new StringBuilder("گزارش مصاحبه حرفه‌ای پرونده «").append(caseName).append("»\n\n");
        int requiredMissing=0,answered=0;StringBuilder weaknesses=new StringBuilder();
        for(ProfessionalIntakeEngine.Question q:questions){
            String value=answers.get(q.id);boolean missing=ProfessionalIntakeEngine.isMissing(value);
            if(!missing)answered++;
            if(missing){if(q.required)requiredMissing++;weaknesses.append("• ").append(q.weaknessIfMissing).append(q.required?" [مانع اصلی]":" [تکمیلی]").append("\n");}
        }
        report.append("پاسخ داده‌شده: ").append(answered).append(" از ").append(questions.size()).append("\nنقص‌های ضروری: ").append(requiredMissing).append("\n\n");
        if(weaknesses.length()==0)report.append("همه اطلاعات پایه تکمیل شده است.\n");else report.append("نقاط ضعف و اطلاعات ناقص:\n").append(weaknesses);
        String arbitration=answers.get("arbitration"),goal=answers.get("goal"),possession=answers.get("possession"),evidence=answers.get("evidence");
        report.append("\nکنترل‌های هوشمند:\n");
        if(ProfessionalIntakeEngine.isMissing(arbitration))report.append("⚠ پیش از انتخاب مرجع، متن شرط داوری باید بررسی شود.\n");else report.append("✓ اطلاعات داوری ثبت شد؛ اعتبار و دامنه آن هنوز نیازمند تحلیل قرارداد است.\n");
        if(goal!=null&&goal.contains("فسخ")&&goal.contains("الزام"))report.append("⚠ فسخ و الزام هم‌زمان ذکر شده؛ خواسته اصلی و علی‌البدل باید تفکیک شود.\n");
        if(possession!=null&&(possession.contains("فروش")||possession.contains("انتقال")||possession.contains("از بین")))report.append("⚠ احتمال فوریت وجود دارد؛ دستور موقت، تأمین خواسته یا تأمین دلیل بررسی شود.\n");
        if(ProfessionalIntakeEngine.isMissing(evidence))report.append("⚠ تا اتصال هر ادعا به سند، متن نهایی آماده ثبت نیست.\n");
        report.append("\nوضعیت: ").append(requiredMissing==0?"آماده تحلیل حقوقی":"نیازمند تکمیل مصاحبه");
        String finalReport=report.toString();int missing=requiredMissing;
        new Thread(()->{
            LegalDao dao=db.legalDao();long now=System.currentTimeMillis();
            dao.addItem(new CaseItemEntity(caseId,"INTAKE_REPORT","گزارش مصاحبه",finalReport,missing==0?"READY_FOR_ANALYSIS":"INCOMPLETE",now,now));
            dao.updateWorkflow(caseId,missing==0?"ANALYSIS_READY":"INTAKE_INCOMPLETE",now);
        }).start();
        listener.onCompleted(finalReport);
    }

    private int dp(int value){return(int)(value*activity.getResources().getDisplayMetrics().density+.5f);}
}
