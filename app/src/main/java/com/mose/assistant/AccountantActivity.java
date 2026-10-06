package com.mose.assistant;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AccountantActivity extends AppCompatActivity {
    private static final int NAVY=Color.rgb(5,17,32), CARD=Color.rgb(16,35,55), GOLD=Color.rgb(232,190,92), CYAN=Color.rgb(42,222,193);
    private AccountingDb db;
    private TextView sales, cash, profit, inventory, recent;
    private final NumberFormat money = NumberFormat.getNumberInstance(new Locale("fa", "IR"));

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b); db = new AccountingDb(this); build(); refresh();
    }

    private void build() {
        ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(NAVY);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(22),dp(18),dp(30)); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); scroll.addView(root);
        TextView title=text("◈ حسابدار Mose",26,Color.WHITE,Typeface.BOLD); title.setGravity(Gravity.RIGHT); root.addView(title);
        TextView sub=text("حساب‌های شرکت، ساده و یکپارچه",14,Color.rgb(170,192,205),Typeface.NORMAL); sub.setGravity(Gravity.RIGHT); root.addView(sub,lp(-1,-2,0,16));

        LinearLayout dashboard=card(); dashboard.addView(text("خلاصه مالی امروز",18,GOLD,Typeface.BOLD));
        sales=text("",16,Color.WHITE,Typeface.BOLD); cash=text("",16,Color.WHITE,Typeface.BOLD); profit=text("",16,CYAN,Typeface.BOLD); inventory=text("",16,Color.WHITE,Typeface.BOLD);
        dashboard.addView(sales); dashboard.addView(cash); dashboard.addView(profit); dashboard.addView(inventory); root.addView(dashboard,lp(-1,-2,0,14));

        TextView h=text("ماژول‌ها",19,Color.WHITE,Typeface.BOLD); h.setGravity(Gravity.RIGHT); root.addView(h,lp(-1,-2,0,8));
        GridLayout grid=new GridLayout(this); grid.setColumnCount(2);
        module(grid,"🧾 فروش و فاکتور","فروش"); module(grid,"🛒 خرید","خرید"); module(grid,"💳 دریافت","دریافت"); module(grid,"💸 پرداخت","پرداخت");
        module(grid,"📦 ورود انبار","ورود انبار"); module(grid,"🚚 خروج انبار","خروج انبار"); module(grid,"👥 حقوق و دستمزد","حقوق"); module(grid,"🏢 دارایی ثابت","دارایی ثابت"); module(grid,"🏛 مالیات و مودیان","مالیات");
        root.addView(grid);

        LinearLayout log=card(); log.addView(text("آخرین عملیات",18,GOLD,Typeface.BOLD)); recent=text("هنوز عملیاتی ثبت نشده است.",14,Color.rgb(210,225,234),Typeface.NORMAL); recent.setGravity(Gravity.RIGHT); recent.setLineSpacing(dp(4),1); log.addView(recent); root.addView(log,lp(-1,-2,12,0));
        setContentView(scroll);
    }

    private void module(GridLayout grid,String label,String type) {
        Button b=new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(14); b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setBackground(round(CARD,18)); b.setOnClickListener(v->entry(type));
        GridLayout.LayoutParams p=new GridLayout.LayoutParams(); p.width=0; p.height=dp(76); p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); p.setMargins(dp(4),dp(4),dp(4),dp(4)); grid.addView(b,p);
    }

    private void entry(String type) {
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(18),0,dp(18),0); form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText title=input(type.equals("فروش") ? "شرح کالا یا خدمت" : "شرح عملیات",false); EditText amount=input("مبلغ به تومان",true); EditText quantity=input("تعداد/مقدار (اختیاری)",true); EditText party=input("طرف حساب (اختیاری)",false); EditText note=input("یادداشت (اختیاری)",false);
        form.addView(title); form.addView(amount); if(type.contains("انبار")||type.equals("فروش")||type.equals("خرید")) form.addView(quantity); form.addView(party); form.addView(note);
        new AlertDialog.Builder(this).setTitle("ثبت «"+type+"»").setView(form).setNegativeButton("انصراف",null).setPositiveButton("ثبت نهایی",(d,w)->{
            String t=title.getText().toString().trim(); if(t.isEmpty()){Toast.makeText(this,"شرح عملیات لازم است",Toast.LENGTH_LONG).show();return;}
            try { db.add(type,t,number(amount),number(quantity),party.getText().toString().trim(),note.getText().toString().trim()); refresh(); Toast.makeText(this,"ثبت شد",Toast.LENGTH_SHORT).show(); }
            catch(Exception e){Toast.makeText(this,"ثبت انجام نشد",Toast.LENGTH_LONG).show();}
        }).show();
    }

    private void refresh() {
        double sale=db.total("فروش"), purchase=db.total("خرید"), received=db.total("دریافت"), paid=db.total("پرداخت"), payroll=db.total("حقوق"), tax=db.total("مالیات");
        sales.setText("فروش: "+fmt(sale)); cash.setText("مانده خزانه: "+fmt(received-paid)); profit.setText("سود تقریبی: "+fmt(sale-purchase-payroll-tax)); inventory.setText("موجودی تعدادی: "+money.format(db.inventoryBalance()));
        List<String> rows=db.recent(12); recent.setText(rows.isEmpty()?"هنوز عملیاتی ثبت نشده است.":android.text.TextUtils.join("\n\n",rows));
    }

    private String fmt(double n){return money.format(Math.round(n))+" تومان";}
    private double number(EditText e){String s=e.getText().toString().replace(",","").replace("٬","").trim(); return s.isEmpty()?0:Double.parseDouble(s);}
    private EditText input(String hint,boolean numeric){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.BLACK);e.setHintTextColor(Color.DKGRAY);e.setSingleLine(true);e.setPadding(dp(12),dp(8),dp(12),dp(8));if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);return e;}
    private LinearLayout card(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(16),dp(16),dp(16),dp(16));x.setBackground(round(CARD,22));return x;}
    private TextView text(String s,float z,int c,int st){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setTypeface(Typeface.create("sans",st));t.setPadding(0,dp(5),0,dp(5));return t;}
    private GradientDrawable round(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));d.setStroke(dp(1),Color.rgb(35,69,88));return d;}
    private LinearLayout.LayoutParams lp(int w,int h,int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(0,dp(top),0,dp(bottom));return p;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
