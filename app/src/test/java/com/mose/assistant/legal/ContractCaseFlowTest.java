package com.mose.assistant.legal;

import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class ContractCaseFlowTest {
    private Map<String,String> completeCase(){
        Map<String,String> a=new LinkedHashMap<>();
        a.put("role","مدیرعامل و نماینده قانونی شرکت خواهان");
        a.put("goal","الزام به تحویل کالای موضوع قرارداد، مطالبه خسارت اثبات‌شده و هزینه‌های قانونی");
        a.put("contract","قرارداد نمونه شماره ۱۰۰ مورخ ۱۴۰۵/۰۱/۱۰ بین شرکت الف و شرکت ب درباره تحویل کالای معدنی");
        a.put("chain","یک قرارداد تأمین مرتبط وجود دارد که تصویر آن پیوست پرونده است.");
        a.put("obligation","تحویل ۱۰۰۰ تن کالای معدنی با مشخصات فنی توافق‌شده");
        a.put("deadline","حداکثر تا ۱۴۰۵/۰۲/۳۰ در انبار مورد توافق");
        a.put("your_performance","خواهان پیش‌پرداخت را انجام داده و رسید بانکی و مکاتبات آمادگی تحویل موجود است.");
        a.put("breach","خوانده پس از پایان موعد فقط بخشی از کالا را تحویل داده و از تحویل باقیمانده امتناع کرده است.");
        a.put("timeline","۱۴۰۵/۰۱/۱۰ انعقاد قرارداد؛ ۱۴۰۵/۰۱/۱۵ پرداخت؛ ۱۴۰۵/۰۲/۳۰ سررسید؛ ۱۴۰۵/۰۳/۰۵ اخطار");
        a.put("demand","اظهارنامه نمونه در ۱۴۰۵/۰۳/۰۵ ابلاغ و بدون پاسخ مؤثر مانده است.");
        a.put("loss","مابه‌التفاوت خرید جایگزین طبق سه فاکتور رسمی و هزینه حمل اضافی؛ مبلغ نهایی نیازمند کارشناسی است.");
        a.put("arbitration","قرارداد فاقد شرط داوری است و حل اختلاف را در دادگاه صالح مقرر کرده است.");
        a.put("evidence","اصل قرارداد؛ رسید بانکی؛ اظهارنامه و گواهی ابلاغ؛ فاکتورهای خرید جایگزین؛ مکاتبات");
        a.put("possession","باقیمانده کالا نزد خوانده است و خطر انتقال آن اعلام نشده است.");
        a.put("proceedings","پرونده قضایی دیگری درباره همین موضوع ثبت نشده است.");
        a.put("opponent_defense","طرف مقابل ممکن است ادعا کند مشخصات فنی کالا یا موعد تحویل تغییر کرده است.");
        return a;
    }

    @Test public void completeCaseReachesHumanReview(){
        Map<String,String> answers=completeCase();
        ContractDraftEngine.Result draft=ContractDraftEngine.create(answers,"پرونده آزمایشی");
        assertTrue(draft.ready);
        assertTrue(draft.draft.contains("الزام به انجام تعهد"));
        CaseReviewEngine.Review review=CaseReviewEngine.reviewContractClaim(answers,5,draft.validation);
        assertTrue(review.approvable);
        assertTrue(review.report.contains("دیدگاه وکیل خوانده"));
        assertTrue(review.report.contains("دیدگاه قاضی/داور"));
    }

    @Test public void missingArbitrationAndDocumentsBlocksApproval(){
        Map<String,String> answers=completeCase();
        answers.put("arbitration","نمی‌دانم");
        ContractDraftEngine.Result draft=ContractDraftEngine.create(answers,"پرونده ناقص");
        assertFalse(draft.ready);
        CaseReviewEngine.Review review=CaseReviewEngine.reviewContractClaim(answers,0,draft.validation);
        assertFalse(review.approvable);
        assertTrue(review.report.contains("هیچ فایل سندی"));
        assertTrue(review.report.contains("شرط داوری"));
    }
}
