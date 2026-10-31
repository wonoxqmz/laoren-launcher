package com.lcj.oldfriend;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

/** 语音播报（系统 TTS）。老机器上没有 TTS 就静默失败，绝不打扰。 */
public class Tts {
    private static TextToSpeech tts;
    private static boolean ready = false;
    private static boolean failed = false;

    public static void init(final Context c) {
        if (tts != null) return;
        try {
            tts = new TextToSpeech(c.getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override public void onInit(int status) {
                    ready = (status == TextToSpeech.SUCCESS);
                    if (ready && tts != null) {
                        try { tts.setLanguage(Locale.CHINA); } catch (Exception ignored) {}
                    }
                }
            });
        } catch (Exception e) { failed = true; }
    }

    public static void speak(Context c, String text) {
        if (text == null || text.length() == 0) return;
        init(c);
        if (ready && tts != null) {
            try { tts.speak(text, TextToSpeech.QUEUE_ADD, null); } catch (Exception ignored) {}
        }
    }

    /** 点按播报：只在设置开启时读。 */
    public static void click(Context c, String text) {
        if (Prefs.ttsClick(c)) speak(c, text);
    }

    public static void destroy() {
        if (tts != null) {
            try { tts.shutdown(); } catch (Exception ignored) {}
            tts = null; ready = false;
        }
    }
}
