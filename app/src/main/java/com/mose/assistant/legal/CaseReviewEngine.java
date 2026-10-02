package com.mose.assistant.legal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CaseReviewEngine {
    public static final class Review {
        public final String report;
        public final boolean approvable;
        public final List<String> blockers;
        Review(String report,boolean approvable,List<String> blockers){this.report=report;this.approvable=approvable;this.blockers=blockers;}
    }
    private CaseReviewEngine(){}

    public static Review reviewContractClaim(Map<String,String> a,int documentCount,String draftValidation){
        List<String> blockers=new ArrayList<>(),risks=new ArrayList<>(),strengths=new ArrayList<>();
        if(missing(a,"contract"))blockers.add("قرارداد اصلی و مشخصات آن کامل نشده است.");else strengths.add("مبنای قراردادی معرفی شده است.");
        if(missing(a,"role"))blockers.add("سمت و اختیار اقدام‌کننده احراز نشده است.");
        if(missing(a,"obligation"))blockers.add("تعهد دقیق طرف مقابل مشخص نشده است.");
        if(missing(a,"deadline"))blockers.add("سررسید و شرایط اجرای تعهد روشن نیست.");
        if(missing(a,"your_performance"))blockers.add("اجرای تعهدات متقابل خواهان اثبات نشده است.");
        if(missing(a,"breach"))blockers.add("فعل یا ترک فعل ناقض قرارداد و تاریخ آن روشن نیست.");
        if(missing(a,"arbitration"))blockers.add("شرط داوری و مرجع حل اختلاف بررسی نشده است.");
        if(missing(a,"evidence"))blockers.add("ارتباط ادعاها با ادله ثبت نشده است.");
        if(documentCount==0)blockers.add("هیچ فایل سندی به پرونده متصل نشده است.");
        else strengths.add(documentCount+" فایل سند در پرونده موجود است؛ اصالت و دلالت آنها باید کنترل شود.");

        String loss=get(a,"loss");
        if(!ProfessionalIntakeEngine.isMissing(loss))risks.add("خسارت ادعایی باید از حیث مبلغ، روش محاسبه، قابلیت پیش‌بینی و رابطه سببیت اثبات شود.");
        if(missing(a,"demand"))risks.add("مطالبه یا اخطار قبلی ثبت نشده است.");
        if(missing(a,"opponent_defense"))risks.add("دفاع محتمل طرف مقابل تحلیل نشده است.");
        String goal=get(a,"goal");
        if(goal.contains("فسخ")&&goal.contains("الزام"))risks.add("فسخ و الزام به اجرای همان تعهد نیازمند تفکیک خواسته اصلی و علی‌البدل است.");
        String possession=get(a,"possession");
        if(possession.contains("فروش")||possession.contains("انتقال")||possession.contains("تلف"))risks.add("خطر تغییر وضعیت مال مطرح شده و اقدام فوری باید مستقل بررسی شود.");
        if(draftValidation!=null&&draftValidation.contains("موانع ثبت رسمی"))risks.add("نسخه پیش‌نویس قبلی دارای گزارش مانع ثبت بوده است.");

        StringBuilder out=new StringBuilder("بازبینی سه‌جانبه پرونده قراردادی\n\n");
        out.append("۱) دیدگاه وکیل خواهان\n");
        if(strengths.isEmpty())out.append("• نقطه قوت مستند کافی هنوز ثبت نشده است.\n");else for(String x:strengths)out.append("• ").append(x).append("\n");
        out.append("• خواسته باید قطعی، قابل اجرا و دارای بهای مشخص باشد.\n");
        out.append("• هر واقعه باید به یک یا چند سند متصل شود.\n\n");

        out.append("۲) دیدگاه وکیل خوانده\n");
        out.append("• ایراد به صلاحیت به علت شرط داوری یا محل اجرای تعهد بررسی می‌شود.\n");
        out.append("• ممکن است اصل اختیار امضاکنندگان، انجام تعهدات خواهان، سررسید، مقدار مال یا محاسبه خسارت انکار شود.\n");
        out.append("• قراردادهای زنجیره‌ای ممکن است برای نفی رابطه مستقیم یا مسئولیت مورد استناد قرار گیرند.\n");
        for(String x:risks)out.append("• ").append(x).append("\n");

        out.append("\n۳) دیدگاه قاضی/داور\n");
        out.append("• ابتدا صلاحیت، سمت، وجود قرارداد معتبر و خواسته منجز بررسی می‌شود.\n");
        out.append("• سپس اجرای تعهدات متقابل، وقوع نقض، انتساب آن و رابطه سببیت ارزیابی می‌شود.\n");
        out.append("• میزان خسارت بدون دلیل و محاسبه قابل رسیدگی قطعی نیست.\n");
        out.append("• تصمیم نهایی باید فقط بر اسناد قابل ارائه و وقایع اثبات‌شده استوار باشد.\n\n");

        out.append("۴) موانع نهایی\n");
        if(blockers.isEmpty())out.append("• مانع ساختاری اصلی شناسایی نشد؛ بازبینی انسانی و تطبیق اصل اسناد همچنان لازم است.\n");
        else for(String x:blockers)out.append("• ").append(x).append("\n");

        boolean approvable=blockers.isEmpty();
        out.append("\nنتیجه سیستم: ").append(approvable?"قابل انتقال به مرحله تأیید انسانی":"غیرقابل تأیید؛ ابتدا موانع فوق رفع شود");
        return new Review(out.toString(),approvable,blockers);
    }

    private static boolean missing(Map<String,String>a,String k){return ProfessionalIntakeEngine.isMissing(get(a,k));}
    private static String get(Map<String,String>a,String k){String x=a.get(k);return x==null?"":x.trim();}
}
