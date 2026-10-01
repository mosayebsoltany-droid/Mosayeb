package com.mose.assistant;

import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements RecognitionListener {
    private static final int NAVY = Color.rgb(5, 17, 32);
    private static final int CARD = Color.rgb(16, 35, 55);
    private static final int GOLD = Color.rgb(232, 190, 92);
    private static final int CYAN = Color.rgb(42, 222, 193);

    private SpeechRecognizer recognizer;
    private Intent speechIntent;
    private TextToSpeech speaker;
    private TextView status, liveText, permissionCount;
    private Button mic;
    private LinearLayout conversation;
    private boolean listening;
    private boolean persianVoiceAvailable;

    private final ActivityResultLauncher<String[]> permissions =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> updatePermissionCount());

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        buildUi();
        initVoice();
        updatePermissionCount();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(4,14,28), Color.rgb(8,31,49), Color.rgb(4,14,28)});
        scroll.setBackground(background);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(22), dp(22), dp(36));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root);

        LinearLayout top = row();
        TextView crown = text("♛", 30, GOLD, Typeface.BOLD);
        TextView brand = text("MOSE  SULTAN", 24, Color.WHITE, Typeface.BOLD);
        top.addView(crown, new LinearLayout.LayoutParams(0, dp(54), 1));
        top.addView(brand, new LinearLayout.LayoutParams(0, dp(54), 4));
        root.addView(top);

        TextView online = pill("●  آنلاین و آماده فرمان", CYAN, Color.rgb(7,48,48));
        root.addView(online, params(-1, dp(44), 0, 8));

        LinearLayout hero = card();
        TextView greeting = text("در خدمت شما هستم، سلطان", 25, Color.WHITE, Typeface.BOLD);
        greeting.setGravity(Gravity.RIGHT);
        hero.addView(greeting);
        TextView sub = text("فرمان بدهید؛ من اجرا می‌کنم و نتیجه را گزارش می‌دهم.", 15, Color.rgb(180,198,211), Typeface.NORMAL);
        sub.setGravity(Gravity.RIGHT);
        sub.setPadding(0, dp(8), 0, dp(18));
        hero.addView(sub);

        status = text("آماده دریافت دستور", 14, GOLD, Typeface.BOLD);
        status.setGravity(Gravity.CENTER);
        hero.addView(status);

        liveText = text("«Hi Mose»", 18, Color.WHITE, Typeface.NORMAL);
        liveText.setGravity(Gravity.CENTER);
        liveText.setPadding(0, dp(10), 0, dp(14));
        hero.addView(liveText);

        mic = new Button(this);
        mic.setText("🎙");
        mic.setTextSize(38);
        mic.setTextColor(NAVY);
        mic.setAllCaps(false);
        mic.setBackground(oval(CYAN));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(108), dp(108));
        mp.gravity = Gravity.CENTER;
        hero.addView(mic, mp);
        mic.setOnClickListener(v -> { if (listening) stopListening(); else ensureMic(); });

        TextView wave = text("▂ ▅ ▇ ▃ ▆ ▂ ▇ ▅ ▂", 22, CYAN, Typeface.BOLD);
        wave.setGravity(Gravity.CENTER);
        wave.setPadding(0, dp(12), 0, 0);
        hero.addView(wave);
        root.addView(hero, params(-1, -2, 0, 14));

        TextView roleTitle = text("نیروی اجرایی شما", 19, Color.WHITE, Typeface.BOLD);
        roleTitle.setGravity(Gravity.RIGHT);
        root.addView(roleTitle, params(-1, -2, 0, 8));

        HorizontalScrollView rolesScroll = new HorizontalScrollView(this);
        rolesScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout roles = row();
        roles.addView(role("⚖", "وکیل Mose", "پرونده‌ها و لوایح", v -> command("وکیل mose پرونده باریت را باز کن")));
        roles.addView(role("◈", "حسابدار Mose", "هزینه و گزارش مالی", v -> command("حسابدار mose")));
        roles.addView(role("✦", "منشی Mose", "تماس و برنامه‌ها", v -> command("منشی mose")));
        roles.addView(role("⌁", "کارمند Mose", "پیگیری و اجرا", v -> command("کارمند mose")));
        rolesScroll.addView(roles);
        root.addView(rolesScroll, params(-1, dp(154), 0, 16));

        LinearLayout access = card();
        LinearLayout accessTop = row();
        TextView accessTitle = text("مرکز فرمان و دسترسی", 18, Color.WHITE, Typeface.BOLD);
        permissionCount = pill("در حال بررسی", GOLD, Color.rgb(65,48,20));
        accessTop.addView(accessTitle, new LinearLayout.LayoutParams(0, dp(50), 3));
        accessTop.addView(permissionCount, new LinearLayout.LayoutParams(0, dp(42), 2));
        access.addView(accessTop);

        access.addView(actionButton("مجوزهای پایه گوشی", "میکروفن، مخاطبین، تماس، پیام و اعلان", v -> requestBasePermissions()));
        access.addView(actionButton("کنترل صفحه و برنامه‌ها", "فعال‌سازی Accessibility برای اجرای فرمان", v -> openSetting(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        access.addView(actionButton("دسترسی به اعلان‌ها", "خواندن و مدیریت اعلان‌ها با اجازه شما", v -> openSetting("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        root.addView(access, params(-1, -2, 0, 16));

        LinearLayout chatCard = card();
        TextView chatTitle = text("گزارش عملیات", 18, Color.WHITE, Typeface.BOLD);
        chatTitle.setGravity(Gravity.RIGHT);
        chatCard.addView(chatTitle);
        conversation = new LinearLayout(this);
        conversation.setOrientation(LinearLayout.VERTICAL);
        conversation.setPadding(0, dp(8), 0, 0);
        chatCard.addView(conversation);
        addBubble("Mose", "چشم سلطان. آماده‌ام؛ اولین فرمان را بفرمایید.", false);
        root.addView(chatCard, params(-1, -2, 0, 12));

        TextView safety = text("🔒 اطلاعات شما روی گوشی می‌ماند؛ اقدامات حساس فقط پس از تأیید اجرا می‌شوند.", 12, Color.rgb(125,151,168), Typeface.NORMAL);
        safety.setGravity(Gravity.CENTER);
        root.addView(safety);
        setContentView(scroll);
    }

    private void initVoice() {
        speaker = new TextToSpeech(this, r -> {
            if (r == TextToSpeech.SUCCESS) {
                int fa = speaker.setLanguage(new Locale("fa","IR"));
                persianVoiceAvailable = fa != TextToSpeech.LANG_MISSING_DATA
                        && fa != TextToSpeech.LANG_NOT_SUPPORTED;
                if (!persianVoiceAvailable) speaker.setLanguage(Locale.US);
                speaker.setSpeechRate(.9f);
                say("در خدمت شما هستم سلطان");
            }
        });
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(this);
            speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR");
            speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        } else {
            status.setText("تشخیص گفتار روی گوشی فعال نیست");
            mic.setEnabled(false);
        }
    }

    private void ensureMic() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening();
        else permissions.launch(new String[]{Manifest.permission.RECORD_AUDIO});
    }

    private void requestBasePermissions() {
        ArrayList<String> p = new ArrayList<>();
        p.add(Manifest.permission.RECORD_AUDIO);
        p.add(Manifest.permission.READ_CONTACTS);
        p.add(Manifest.permission.CALL_PHONE);
        p.add(Manifest.permission.READ_PHONE_STATE);
        p.add(Manifest.permission.SEND_SMS);
        if (android.os.Build.VERSION.SDK_INT >= 33) p.add(Manifest.permission.POST_NOTIFICATIONS);
        permissions.launch(p.toArray(new String[0]));
    }

    private void updatePermissionCount() {
        String[] p = {Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.SEND_SMS};
        int count=0; for(String x:p) if(ContextCompat.checkSelfPermission(this,x)==PackageManager.PERMISSION_GRANTED) count++;
        if(permissionCount != null) permissionCount.setText(count + " از ۵ فعال");
    }

    private void startListening() {
        if(recognizer==null) return;
        listening=true; status.setText("در حال شنیدن فرمان سلطان…"); mic.setText("■");
        recognizer.startListening(speechIntent);
    }
    private void stopListening() {
        listening=false; mic.setText("🎙"); status.setText("آماده دریافت دستور");
        if(recognizer!=null) recognizer.stopListening();
    }

    private void command(String raw) {
        String t=raw.trim().toLowerCase(new Locale("fa","IR"));
        liveText.setText(raw); addBubble("سلطان", raw, true);
        if(t.contains("hi mose")||t.contains("های موز")||t.equals("موز")) { reply("چشم سلطان، گوش می‌دهم."); return; }
        if(t.contains("وکیل")) { showLawyerModule(); return; }
        if(t.contains("حسابدار")) { reply("چشم سلطان. حسابدار Mose آماده ثبت هزینه، بدهی و گزارش مالی است."); return; }
        if(t.contains("منشی")) { reply("چشم سلطان. منشی Mose آماده تماس، پیام و برنامه‌ریزی است."); return; }
        if(t.contains("کارمند")) { reply("چشم سلطان. کارمند Mose آماده پیگیری مأموریت است."); return; }
        if(t.contains("واتساپ")) { openPackage("com.whatsapp","واتساپ"); return; }
        if(t.contains("تلگرام")) { openPackage("org.telegram.messenger","تلگرام"); return; }
        if(t.contains("اینستاگرام")) { openPackage("com.instagram.android","اینستاگرام"); return; }
        if(t.contains("نقشه")||t.contains("مپ")) { openPackage("com.google.android.apps.maps","نقشه"); return; }
        if(t.contains("کروم")||t.contains("مرورگر")) { openPackage("com.android.chrome","مرورگر"); return; }
        if(t.contains("دوربین")) { launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),"دوربین"); return; }
        if(t.contains("تنظیمات")) { launch(new Intent(Settings.ACTION_SETTINGS),"تنظیمات"); return; }
        String n=phone(t);
        if(n!=null&&(t.contains("تماس")||t.contains("زنگ"))) { launch(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+n)),"شماره‌گیر"); return; }
        if(n!=null&&(t.contains("پیام")||t.contains("اس ام اس"))) { launch(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"+n)),"پیامک"); return; }
        if(t.contains("تماس")||t.contains("زنگ")) {
            String contactName = contactNameFrom(t);
            if(contactName.isEmpty()) { reply("نام مخاطب را بفرمایید سلطان."); return; }
            String contactPhone = findContactPhone(contactName);
            if(contactPhone==null) reply("سلطان، مخاطب "+contactName+" پیدا نشد.");
            else launch(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+contactPhone)),"شماره‌گیر برای "+contactName);
            return;
        }
        reply("چشم سلطان. فرمان ثبت شد؛ برای اجرای هوشمند این دستور، هسته هوش مصنوعی را در مرحله بعد متصل می‌کنم.");
    }

    private void reply(String message) { addBubble("Mose", message, false); say(message); }
    private void say(String message) {
        status.setText(message);
        if (speaker != null) {
            String spoken = persianVoiceAvailable ? message : "Cheshm, Soltan.";
            speaker.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "mose");
        }
        mic.setText("🎙");
        listening = false;
    }
    private void openPackage(String pkg,String label) {
        Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
        if(i==null) reply(label+" روی گوشی پیدا نشد."); else launch(i,label);
    }
    private void launch(Intent i,String label) {
        try { startActivity(i); reply("چشم سلطان؛ "+label+" باز شد."); }
        catch(ActivityNotFoundException e) { reply("سلطان، نتوانستم "+label+" را باز کنم."); }
    }
    private void openSetting(String action) { try { startActivity(new Intent(action)); } catch(Exception e){ startActivity(new Intent(Settings.ACTION_SETTINGS)); } }
    private String phone(String t) {
        String n=t.replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9').replaceAll("[^0-9+]","");
        Matcher m=Pattern.compile("(?:\\+98|0098|0)?9\\d{9}").matcher(n); return m.find()?m.group():null;
    }

    private void showLawyerModule() {
        liveText.setText("وکیل Mose");
        addBubble("Mose", "چشم سلطان؛ میز وکیل باز شد.", false);
        say("Vakil Mose baz shod, Soltan.");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("⚖ وکیل Mose")
                .setMessage("میز خصوصی پرونده‌ها\n\nاز این بخش می‌توانید یک سند را از حافظه گوشی انتخاب کنید. اطلاعات پرونده در کد عمومی برنامه ذخیره نمی‌شود.")
                .setNegativeButton("بستن", null)
                .setPositiveButton("انتخاب سند", (d, w) -> {
                    Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    pick.setType("*/*");
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    try { startActivity(pick); }
                    catch(Exception e) { reply("سلطان، فایل‌منیجر گوشی باز نشد."); }
                }).create();
        dialog.setOnShowListener(x -> {
            TextView message = dialog.findViewById(android.R.id.message);
            if(message != null) {
                message.setTextDirection(View.TEXT_DIRECTION_RTL);
                message.setGravity(Gravity.RIGHT);
            }
        });
        dialog.show();
    }

    private String contactNameFrom(String t) {
        return t.replace("تماس بگیر","").replace("تماس","")
                .replace("زنگ بزن","").replace("زنگ","")
                .replace("با ","").replace("به ","").replace("رو ","")
                .replace("را ","").trim();
    }

    private String findContactPhone(String name) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissions.launch(new String[]{Manifest.permission.READ_CONTACTS});
            reply("سلطان، ابتدا اجازه مخاطبین را فعال کنید و دوباره فرمان بدهید.");
            return null;
        }
        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        try(Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, new String[]{"%"+name+"%"}, null)) {
            if(cursor!=null && cursor.moveToFirst()) return cursor.getString(0);
        } catch(Exception ignored) {}
        return null;
    }

    private void addBubble(String who,String message,boolean user) {
        TextView b=text(who+"  •  "+message,14,user?Color.WHITE:Color.rgb(215,229,237),Typeface.NORMAL);
        b.setGravity(Gravity.RIGHT); b.setPadding(dp(14),dp(12),dp(14),dp(12));
        b.setBackground(round(user?Color.rgb(35,80,101):Color.rgb(24,48,68),18));
        LinearLayout.LayoutParams lp=params(-1,-2,0,6); if(user) lp.setMargins(dp(34),0,0,dp(6)); else lp.setMargins(0,0,dp(34),dp(6));
        conversation.addView(b,lp);
    }

    private LinearLayout role(String icon,String title,String sub,View.OnClickListener c) {
        LinearLayout box=card(); box.setMinimumWidth(dp(190)); box.setOnClickListener(c);
        TextView i=text(icon,28,GOLD,Typeface.BOLD); box.addView(i);
        box.addView(text(title,16,Color.WHITE,Typeface.BOLD));
        box.addView(text(sub,12,Color.rgb(151,176,190),Typeface.NORMAL));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(198),dp(138)); lp.setMargins(0,0,dp(10),0); box.setLayoutParams(lp); return box;
    }
    private View actionButton(String title,String sub,View.OnClickListener click) {
        LinearLayout b=row(); b.setPadding(dp(12),dp(12),dp(12),dp(12)); b.setBackground(round(Color.rgb(21,47,67),16)); b.setOnClickListener(click);
        LinearLayout words=new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
        TextView t=text(title,15,Color.WHITE,Typeface.BOLD); t.setGravity(Gravity.RIGHT);
        TextView s=text(sub,12,Color.rgb(150,176,190),Typeface.NORMAL); s.setGravity(Gravity.RIGHT);
        words.addView(t); words.addView(s); b.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=text("‹",28,CYAN,Typeface.BOLD); b.addView(arrow,new LinearLayout.LayoutParams(dp(32),-1));
        LinearLayout.LayoutParams lp=params(-1,-2,0,7); b.setLayoutParams(lp); return b;
    }
    private LinearLayout card() { LinearLayout x=new LinearLayout(this); x.setOrientation(LinearLayout.VERTICAL); x.setPadding(dp(18),dp(18),dp(18),dp(18)); x.setBackground(round(CARD,24)); x.setElevation(dp(5)); return x; }
    private LinearLayout row() { LinearLayout x=new LinearLayout(this); x.setOrientation(LinearLayout.HORIZONTAL); x.setGravity(Gravity.CENTER_VERTICAL); return x; }
    private TextView text(String s,float size,int color,int style) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.create("sans",style)); return t; }
    private TextView pill(String s,int color,int bg) { TextView t=text(s,13,color,Typeface.BOLD); t.setGravity(Gravity.CENTER); t.setPadding(dp(12),0,dp(12),0); t.setBackground(round(bg,30)); return t; }
    private GradientDrawable round(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); d.setStroke(dp(1),Color.rgb(34,68,89)); return d; }
    private GradientDrawable oval(int color) { GradientDrawable d=new GradientDrawable(); d.setShape(GradientDrawable.OVAL); d.setColor(color); d.setStroke(dp(7),Color.rgb(19,90,87)); return d; }
    private LinearLayout.LayoutParams params(int w,int h,int top,int bottom) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(0,dp(top),0,dp(bottom)); return p; }
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+.5f); }

    @Override public void onReadyForSpeech(Bundle b){status.setText("فرمان بفرمایید سلطان…");}
    @Override public void onBeginningOfSpeech(){status.setText("صدای شما را می‌شنوم");}
    @Override public void onRmsChanged(float f){}
    @Override public void onBufferReceived(byte[] b){}
    @Override public void onEndOfSpeech(){status.setText("در حال اجرای فرمان…");}
    @Override public void onError(int e){listening=false;mic.setText("🎙");status.setText("متوجه نشدم؛ دوباره فرمان بدهید.");}
    @Override public void onResults(Bundle b){listening=false;mic.setText("🎙");ArrayList<String> m=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(m!=null&&!m.isEmpty())command(m.get(0));}
    @Override public void onPartialResults(Bundle b){ArrayList<String> m=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(m!=null&&!m.isEmpty())liveText.setText(m.get(0));}
    @Override public void onEvent(int i,Bundle b){}
    @Override protected void onDestroy(){if(recognizer!=null)recognizer.destroy();if(speaker!=null){speaker.stop();speaker.shutdown();}super.onDestroy();}
}
