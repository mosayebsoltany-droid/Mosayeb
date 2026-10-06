package com.mose.assistant;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Locale;

public class WakeWordService extends Service implements RecognitionListener {
    private static final String CHANNEL = "mose_wake";
    private static final int NOTICE = 4102;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private Intent listenIntent;
    private boolean destroyed;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTICE, notification("برای شنیدن «Hi Mose» آماده است"));
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(this);
            listenIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR");
            listenIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            listenIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            restart(700);
        }
    }

    private void restart(long delay) {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            if (destroyed || recognizer == null) return;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                update("مجوز میکروفن را در برنامه فعال کنید");
                restart(5000);
                return;
            }
            try { recognizer.startListening(listenIntent); }
            catch (Exception e) { restart(1500); }
        }, delay);
    }

    private void inspect(Bundle results) {
        ArrayList<String> choices = results == null ? null :
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (choices != null) {
            for (String phrase : choices) {
                String s = phrase.toLowerCase(Locale.ROOT).replace("-", " ").trim();
                if (s.contains("hi mose") || s.contains("hey mose") ||
                        s.contains("های موس") || s.contains("هی موس") ||
                        s.contains("موس باز شو") || s.contains("موز باز شو")) {
                    wake();
                    return;
                }
            }
        }
    }

    private void wake() {
        update("فرمان شنیده شد؛ Mose باز شد");
        Intent open = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("wake_phrase", true);
        startActivity(open);
        restart(2500);
    }

    private Notification notification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Hi Mose فعال است")
                .setContentText(text)
                .setContentIntent(pending)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    private void update(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTICE, notification(text));
    }

    private void createChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(new NotificationChannel(
                CHANNEL, "شنیدن Hi Mose", NotificationManager.IMPORTANCE_LOW));
    }

    @Override public int onStartCommand(Intent intent, int flags, int id) {
        restart(500);
        return START_STICKY;
    }

    @Override public void onResults(Bundle b) { inspect(b); restart(400); }
    @Override public void onPartialResults(Bundle b) { inspect(b); }
    @Override public void onError(int e) { restart(e == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 1800 : 700); }
    @Override public void onEndOfSpeech() { restart(500); }
    @Override public void onReadyForSpeech(Bundle b) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float rms) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEvent(int eventType, Bundle params) {}

    @Override public void onDestroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) recognizer.destroy();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
