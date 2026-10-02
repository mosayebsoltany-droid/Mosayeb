package com.mose.assistant.legal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ContractDraftEngine {
    public static final class Result {
        public final String draft;
        public final String validation;
        public final boolean ready;
        public Result(String draft,String validation,boolean ready){this.draft=draft;this.validation=validation;this.ready=ready;}
    }

    private ContractDraftEngine(){}

    public static Result create(Map<String,String> a,String caseName){
        List<String> blockers=new ArrayList<>(),warnings=new ArrayList<>();
        required(a,"role","سمت و اختیار اقدام‌کننده مشخص نشده است.",blockers);
        required(a,"goal","خواسته دقیق مشخص نشده است.",blockers);
        required(a,"contract","مشخصات قرارداد اصلی ناقص است.",blockers);
        required(a,"obligation","تعهد طرف مقابل تعریف نشده است.",blockers);
        required(a,"deadline","موعد و محل اجرای تعهد مشخص نیست.",blockers);
        required(a,"your_performance","اجرای تعهدات متقابل خواهان اثبات نشده است.",blockers);
        required(a,"breach","نقض قرارداد و تاریخ آن مشخص نیست.",blockers);
        required(a,"timeline","خط زمانی وقایع ناقص است.",blockers);
        required(a,"arbitration","شرط داوری یا مرجع حل اختلاف بررسی نشده است.",blockers);
        required(a,"evidence","ادعاها به اسناد معین متصل نشده‌اند.",blockers);

        String goal=value(a,"goal");
        if(goal.contains("فسخ")&&goal.contains("الزام"))warnings.add("فسخ و الزام هم‌زمان آمده است؛ خواسته اصلی و علی‌البدل باید تفکیک شود.");
        if(!missing(value(a,"loss"))&&value(a,"loss").length()<25)warnings.add("شرح خسارت برای اثبات مبلغ و رابطه سببیت کافی به نظر نمی‌رسد.");
        if(missing(value(a,"demand")))warnings.add("مطالبه یا اخطار قبلی ثبت نشده؛ اثر آن بر سررسید و خسارت باید بررسی شود.");
        String possession=value(a,"possession");
        if(possession.contains("فروش")||possession.contains("انتقال")||possession.contains("تلف")||possession.contains("از بین"))warnings.add("خطر تغییر وضعیت مال وجود دارد؛ اقدام فوری باید جداگانه ارزیابی شود.");
        if(missing(value(a,"opponent_defense")))warnings.add("دفاع محتمل طرف مقابل هنوز تحلیل نشده است.");

        String arbitration=value(a,"arbitration");
        String authority=missing(arbitration)?"[مرجع صالح پس از بررسی شرط حل اختلاف]":
                (arbitration.contains("داور")||arbitration.contains("داوری")?"[مرجع داوری مندرج در قرارداد، پس از احراز اعتبار و دامنه شرط]":"[دادگاه عمومی حقوقی صالح پس از کنترل صلاحیت محلی]");
        String evidence=value(a,"evidence");
        String draft="دادخواست الزام به انجام تعهد و مطالبه خسارت قراردادی\nپرونده: «"+caseName+"»\nوضعیت: "+(blockers.isEmpty()?"پیش‌نویس قابل بازبینی":"پیش‌نویس ناقص — غیرقابل ثبت")+"\n\n"+
                "بسمه‌تعالی\n\nریاست محترم "+authority+"\n\n"+
                "خواهان و سمت: "+safe(value(a,"role"))+"\n"+
                "خوانده/خواندگان: [مشخصات کامل، شناسه و نشانی از شناسنامه پرونده درج شود]\n"+
                "خواسته:\n"+numberGoal(goal)+"\n"+
                "بهای خواسته: [بر اساس ارزش تعهد و خسارت مورد مطالبه تعیین شود]\n\n"+
                "دلایل و منضمات:\n"+numberEvidence(evidence)+"\n\n"+
                "شرح دادخواست\n"+
                "با سلام و احترام،\n"+
                "۱. مبنای رابطه قراردادی طرفین:\n"+safe(value(a,"contract"))+"\n"+
                (!missing(value(a,"chain"))?"\n۲. قراردادها و توافق‌های مرتبط:\n"+value(a,"chain")+"\n":"")+
                "\n۳. تعهد خوانده:\n"+safe(value(a,"obligation"))+"\n"+
                "\n۴. موعد، محل و شرایط اجرا:\n"+safe(value(a,"deadline"))+"\n"+
                "\n۵. اجرای تعهدات خواهان:\n"+safe(value(a,"your_performance"))+"\n"+
                "\n۶. نحوه نقض قرارداد:\n"+safe(value(a,"breach"))+"\n"+
                "\n۷. ترتیب زمانی وقایع:\n"+safe(value(a,"timeline"))+"\n"+
                "\n۸. مطالبه و اخطار قبلی:\n"+safe(value(a,"demand"))+"\n"+
                "\n۹. خسارات ادعایی و شیوه محاسبه:\n"+safe(value(a,"loss"))+"\n"+
                "\n۱۰. شرط داوری و مرجع حل اختلاف:\n"+safe(arbitration)+"\n"+
                "\nبا توجه به مفاد قرارداد، اجرای تعهدات اعلامی خواهان و نقض ادعایی تعهد از سوی خوانده، تقاضای رسیدگی و اتخاذ تصمیم نسبت به خواسته‌های مندرج در دادخواست، پس از احراز اصالت اسناد، تحقق شرایط مسئولیت و میزان خسارات، مورد درخواست است.\n\n"+
                "نام و سمت: "+safe(value(a,"role"))+"\nتاریخ و امضا: [تکمیل شود]\n";
        StringBuilder validation=new StringBuilder("گزارش کنترل پیش‌نویس\n\n");
        if(blockers.isEmpty())validation.append("مانع اطلاعاتی اصلی شناسایی نشد؛ متن هنوز نیازمند کنترل حقوقی و سندی است.\n");
        else{validation.append("موانع ثبت رسمی:\n");for(String x:blockers)validation.append("• ").append(x).append("\n");}
        if(!warnings.isEmpty()){validation.append("\nهشدارها:\n");for(String x:warnings)validation.append("• ").append(x).append("\n");}
        validation.append("\nکنترل‌های اجباری بعدی: اصالت قرارداد، اختیار امضاکنندگان، صلاحیت مرجع، بهای خواسته، پیوست‌های مصدق، مهلت‌ها، رابطه سببیت و محاسبه خسارت.");
        return new Result(draft,validation.toString(),blockers.isEmpty());
    }

    private static void required(Map<String,String>a,String key,String msg,List<String>b){if(missing(value(a,key)))b.add(msg);}
    private static String value(Map<String,String>a,String key){String x=a.get(key);return x==null?"":x.trim();}
    private static boolean missing(String x){return ProfessionalIntakeEngine.isMissing(x);}
    private static String safe(String x){return missing(x)?"[اطلاعات تکمیل شود]":x;}
    private static String numberGoal(String x){
        if(missing(x))return "۱. [خواسته دقیق تکمیل شود]";
        String[] p=x.split("[\\n،;]+");StringBuilder b=new StringBuilder();int n=1;for(String z:p){z=z.trim();if(!z.isEmpty())b.append(n++).append(". ").append(z).append("\n");}return b.toString();
    }
    private static String numberEvidence(String x){
        if(missing(x))return "۱. [فهرست اسناد و ارتباط آنها با ادعاها تکمیل شود]";
        String[] p=x.split("[\\n،;]+");StringBuilder b=new StringBuilder();int n=1;for(String z:p){z=z.trim();if(!z.isEmpty())b.append(n++).append(". ").append(z).append("\n");}return b.toString();
    }
}
