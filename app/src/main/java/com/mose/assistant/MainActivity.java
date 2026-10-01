package com.mose.assistant;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements RecognitionListener {
    private SpeechRecognizer recognizer;
    private Intent speechIntent;
    private TextToSpeech speaker;
    private TextView status;
    private TextView transcript;
    private Button listenButton;
    private boolean listening;

    private final ActivityResultLauncher<String> micPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startListening();
                else say("برای شنیدن صدای شما، اجازه میکروفن لازم است.");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();

        speaker = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                speaker.setLanguage(new Locale("fa", "IR"));
                speaker.setSpeechRate(0.92f);
                say("سلام مسیب، من موز هستم. چه کاری انجام بدهم؟");
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
            status.setText("تشخیص گفتار روی این گوشی در دسترس نیست");
            listenButton.setEnabled(false);
        }

        listenButton.setOnClickListener(v -> {
            if (listening) stopListening();
            else ensureMicAndListen();
        });
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(48, 72, 48, 48);
        root.setBackgroundColor(Color.rgb(7, 27, 42));

        TextView logo = new TextView(this);
        logo.setText("M");
        logo.setTextSize(72);
        logo.setTextColor(Color.rgb(25, 211, 174));
        logo.setGravity(Gravity.CENTER);
        root.addView(logo, new LinearLayout.LayoutParams(-1, 180));

        TextView title = new TextView(this);
        title.setText("Hi Mose");
        title.setTextSize(34);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("دستیار شخصی و صوتی شما");
        subtitle.setTextSize(17);
        subtitle.setTextColor(Color.rgb(168, 190, 201));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 50);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setText("برای شروع، دکمه را لمس کنید و بگویید «Hi Mose»");
        status.setTextSize(16);
        status.setTextColor(Color.rgb(210, 224, 230));
        status.setGravity(Gravity.CENTER);
        status.setPadding(24, 24, 24, 24);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        transcript = new TextView(this);
        transcript.setText("");
        transcript.setTextSize(20);
        transcript.setTextColor(Color.WHITE);
        transcript.setGravity(Gravity.CENTER);
        transcript.setPadding(24, 28, 24, 28);
        root.addView(transcript, new LinearLayout.LayoutParams(-1, 0, 1));

        listenButton = new Button(this);
        listenButton.setText("🎙  گوش می‌دهم");
        listenButton.setTextSize(19);
        listenButton.setAllCaps(false);
        root.addView(listenButton, new LinearLayout.LayoutParams(-1, 150));

        TextView hint = new TextView(this);
        hint.setText("نمونه: «واتساپ را باز کن»  •  «به شماره 0912… پیام بده»  •  «دوربین را باز کن»");
        hint.setTextSize(13);
        hint.setTextColor(Color.rgb(128, 157, 170));
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, 30, 0, 0);
        root.addView(hint, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    private void ensureMicAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) startListening();
        else micPermission.launch(Manifest.permission.RECORD_AUDIO);
    }

    private void startListening() {
        if (recognizer == null) return;
        listening = true;
        status.setText("در حال شنیدن…");
        listenButton.setText("توقف");
        recognizer.startListening(speechIntent);
    }

    private void stopListening() {
        listening = false;
        listenButton.setText("🎙  گوش می‌دهم");
        status.setText("آماده");
        if (recognizer != null) recognizer.stopListening();
    }

    private void handleCommand(String raw) {
        String text = raw.trim().toLowerCase(new Locale("fa", "IR"));
        transcript.setText(raw);

        if (text.contains("hi mose") || text.contains("های موز") || text.equals("موز")) {
            say("بفرمایید، گوش می‌دهم.");
            return;
        }
        if (text.contains("واتساپ")) { openPackage("com.whatsapp", "واتساپ"); return; }
        if (text.contains("تلگرام")) { openPackage("org.telegram.messenger", "تلگرام"); return; }
        if (text.contains("اینستاگرام")) { openPackage("com.instagram.android", "اینستاگرام"); return; }
        if (text.contains("نقشه") || text.contains("مپ")) { openPackage("com.google.android.apps.maps", "نقشه"); return; }
        if (text.contains("کروم") || text.contains("مرورگر")) { openPackage("com.android.chrome", "مرورگر"); return; }
        if (text.contains("دوربین")) {
            launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), "دوربین");
            return;
        }
        if (text.contains("تنظیمات")) {
            launch(new Intent(Settings.ACTION_SETTINGS), "تنظیمات");
            return;
        }

        String number = extractPhone(text);
        if (number != null && (text.contains("تماس") || text.contains("زنگ"))) {
            Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            launch(dial, "شماره‌گیر");
            return;
        }
        if (number != null && (text.contains("پیام") || text.contains("اس ام اس"))) {
            Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
            launch(sms, "پیامک");
            return;
        }

        say("فرمان را شنیدم: " + raw + ". در نسخه بعدی، پاسخ هوشمند و پرونده‌های شخصی شما هم متصل می‌شود.");
    }

    private String extractPhone(String text) {
        String normalized = text
                .replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4')
                .replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9');
        Matcher matcher = Pattern.compile("(?:\\+98|0098|0)?9\\d{9}").matcher(normalized.replaceAll("[^0-9+]", ""));
        return matcher.find() ? matcher.group() : null;
    }

    private void openPackage(String packageName, String label) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            say(label + " روی گوشی پیدا نشد.");
            return;
        }
        launch(intent, label);
    }

    private void launch(Intent intent, String label) {
        try {
            startActivity(intent);
            say(label + " باز شد.");
        } catch (ActivityNotFoundException error) {
            say("نتوانستم " + label + " را باز کنم.");
        }
    }

    private void say(String message) {
        status.setText(message);
        if (speaker != null) speaker.speak(message, TextToSpeech.QUEUE_FLUSH, null, "mose");
    }

    @Override public void onReadyForSpeech(Bundle params) { status.setText("بگویید…"); }
    @Override public void onBeginningOfSpeech() { status.setText("صدای شما را می‌شنوم"); }
    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() { status.setText("در حال پردازش…"); }
    @Override public void onError(int error) {
        listening = false;
        listenButton.setText("🎙  دوباره گوش کن");
        status.setText(error == SpeechRecognizer.ERROR_NO_MATCH ? "متوجه نشدم؛ دوباره بگویید." : "شنیدن متوقف شد؛ دوباره امتحان کنید.");
    }
    @Override public void onResults(Bundle results) {
        listening = false;
        listenButton.setText("🎙  گوش می‌دهم");
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) handleCommand(matches.get(0));
    }
    @Override public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) transcript.setText(matches.get(0));
    }
    @Override public void onEvent(int eventType, Bundle params) {}

    @Override protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        if (speaker != null) {
            speaker.stop();
            speaker.shutdown();
        }
        super.onDestroy();
    }
}
