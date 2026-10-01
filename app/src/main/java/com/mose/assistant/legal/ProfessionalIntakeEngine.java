package com.mose.assistant.legal;

import java.util.ArrayList;
import java.util.List;

public final class ProfessionalIntakeEngine {
    public static final class Question {
        public final String id;
        public final String title;
        public final String prompt;
        public final boolean required;
        public final String weaknessIfMissing;
        public Question(String id,String title,String prompt,boolean required,String weaknessIfMissing){
            this.id=id;this.title=title;this.prompt=prompt;this.required=required;this.weaknessIfMissing=weaknessIfMissing;
        }
    }

    private ProfessionalIntakeEngine(){}

    public static List<Question> contractDispute(){
        List<Question> q=new ArrayList<>();
        q.add(new Question("role","سمت شما","در این اختلاف دقیقاً چه سمتی دارید؟ شخص حقیقی، مدیرعامل، نماینده شرکت، خواهان یا خوانده؟",true,"سمت و اختیار اقدام روشن نیست."));
        q.add(new Question("goal","نتیجه موردنظر","در پایان دقیقاً چه نتیجه‌ای می‌خواهید؟ تحویل مال، اجرای تعهد، دریافت وجه، فسخ، خسارت، توقف فروش یا ترکیبی از این موارد؟",true,"خواسته دقیق و قابل اجرا تعیین نشده است."));
        q.add(new Question("contract","قرارداد اصلی","شماره، تاریخ، موضوع، مبلغ/مقدار و نام طرفین قرارداد اصلی را بنویسید.",true,"هویت و موضوع قرارداد اصلی ناقص است."));
        q.add(new Question("chain","زنجیره قراردادها","آیا قرارداد، الحاقیه یا توافق دیگری در زنجیره معامله وجود دارد؟ رابطه آن با قرارداد شما چیست؟",false,"رابطه قراردادهای مرتبط بررسی نشده است."));
        q.add(new Question("obligation","تعهد طرف مقابل","طرف مقابل دقیقاً متعهد به انجام چه کاری، تحویل چه مقدار مال یا پرداخت چه مبلغی بوده است؟",true,"تعهد نقض‌شده به‌طور دقیق تعریف نشده است."));
        q.add(new Question("deadline","موعد و محل اجرا","موعد، محل و شرایط اجرای تعهد چه بوده است؟ آیا تحقق تعهد وابسته به شرطی بوده؟",true,"سررسید و امکان مطالبه تعهد روشن نیست."));
        q.add(new Question("your_performance","اجرای تعهدات شما","شما یا شرکتتان کدام تعهدات را انجام داده‌اید؟ برای هر مورد چه مدرکی دارید؟",true,"اجرای تعهدات متقابل شما اثبات نشده است."));
        q.add(new Question("breach","نقض قرارداد","طرف مقابل دقیقاً چه اقدام یا ترک اقدامی انجام داده و تاریخ هر مورد چیست؟",true,"رفتار ناقض قرارداد و تاریخ آن مبهم است."));
        q.add(new Question("timeline","ترتیب وقایع","وقایع مهم را از اولین مذاکره تا امروز، به ترتیب تاریخ بنویسید.",true,"خط زمانی قابل اتکا موجود نیست."));
        q.add(new Question("demand","مطالبه قبلی","آیا اظهارنامه، نامه، پیام یا اخطار رسمی ارسال کرده‌اید؟ تاریخ، مخاطب و نتیجه را بنویسید.",false,"زمان مطالبه و امتناع طرف مقابل اثبات نشده است."));
        q.add(new Question("loss","خسارت","چه خسارتی وارد شده؟ مبلغ، روش محاسبه و ارتباط آن با نقض قرارداد را توضیح دهید.",false,"وقوع، مبلغ یا رابطه سببیت خسارت اثبات نشده است."));
        q.add(new Question("arbitration","داوری و حل اختلاف","متن دقیق شرط داوری یا مرجع حل اختلاف قرارداد چیست؟ آیا قبلاً داور یا مرکز داوری اقدام کرده؟",true,"ممکن است مرجع انتخابی صلاحیت نداشته باشد."));
        q.add(new Question("evidence","ادله و اسناد","تمام مدارک را فهرست کنید: قرارداد، نامه، آزمایش، باسکول، رسید، بارنامه، پیام، صوت، عکس و شاهد.",true,"ادعاها هنوز به ادله مشخص متصل نشده‌اند."));
        q.add(new Question("possession","وضعیت مال","مال موضوع اختلاف اکنون کجاست، در اختیار چه کسی است و آیا احتمال انتقال، فروش یا از بین رفتن آن وجود دارد؟",false,"شرایط اقدام فوری و حفظ دلیل روشن نیست."));
        q.add(new Question("proceedings","سوابق رسیدگی","تمام پرونده‌ها، اظهارنامه‌ها، داوری‌ها، تصمیمات و شماره‌های قضایی مرتبط را بنویسید.",false,"خطر تعارض یا طرح مکرر دعوا بررسی نشده است."));
        q.add(new Question("opponent_defense","دفاع احتمالی طرف مقابل","اگر شما وکیل طرف مقابل بودید، مهم‌ترین دفاع یا ایراد را چه می‌دانستید؟",false,"دفاع محتمل طرف مقابل پیش‌بینی نشده است."));
        return q;
    }

    public static boolean isMissing(String value){
        if(value==null)return true;
        String x=value.trim();
        return x.isEmpty()||x.equals("نمی دانم")||x.equals("نمی‌دانم")||x.equals("نامشخص")||x.equals("-");
    }
}
