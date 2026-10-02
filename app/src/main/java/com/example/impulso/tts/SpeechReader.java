package com.example.impulso.tts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SpeechReader {
    public static final class VoiceOption {
        public final String name;
        public final String label;
        public final boolean network;

        public VoiceOption(String name, String label, boolean network) {
            this.name = name;
            this.label = label;
            this.network = network;
        }
    }

    public interface Ready {
        void onReady(boolean spanishAvailable);
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private boolean ready;
    private boolean spanishAvailable;
    private Runnable failure;

    public void init(Context context, Ready callback) {
        shutdown();
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            ready = status == TextToSpeech.SUCCESS && tts != null;
            spanishAvailable = ready && applySpanish();
            if (ready) {
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Runnable fail = failure;
                        if (fail != null) {
                            main.post(fail);
                        }
                    }
                });
            }
            if (callback != null) {
                main.post(() -> callback.onReady(spanishAvailable));
            }
        });
    }

    public boolean isReady() {
        return ready;
    }

    public boolean spanishAvailable() {
        return spanishAvailable;
    }

    public List<VoiceOption> voices() {
        List<VoiceOption> options = new ArrayList<>();
        if (!ready || tts == null || tts.getVoices() == null) {
            return options;
        }
        for (Voice voice : tts.getVoices()) {
            if (voice.getLocale() == null || !"es".equals(voice.getLocale().getLanguage())) {
                continue;
            }
            String label = voice.getLocale().getDisplayName();
            options.add(new VoiceOption(voice.getName(), label, voice.isNetworkConnectionRequired()));
        }
        return options;
    }

    public boolean voiceNeedsNetwork(String voiceName) {
        if (!ready || tts == null || voiceName == null || voiceName.isEmpty() || tts.getVoices() == null) {
            return false;
        }
        for (Voice voice : tts.getVoices()) {
            if (voiceName.equals(voice.getName())) {
                return voice.isNetworkConnectionRequired();
            }
        }
        return false;
    }

    public void speakThen(String first, String second, long pauseMs, float rate, String voiceName, Runnable onFailure) {
        this.failure = onFailure;
        if (!ready || tts == null) {
            if (onFailure != null) {
                onFailure.run();
            }
            return;
        }
        prepareVoice(rate, voiceName);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                if ("impulso-1".equals(utteranceId) && second != null && !second.isEmpty()) {
                    main.postDelayed(() -> {
                        if (tts != null) {
                            tts.speak(second, TextToSpeech.QUEUE_FLUSH, null, "impulso-2");
                        }
                    }, pauseMs);
                }
            }

            @Override
            public void onError(String utteranceId) {
                Runnable fail = failure;
                if (fail != null) {
                    main.post(fail);
                }
            }
        });
        tts.speak(first, TextToSpeech.QUEUE_FLUSH, null, "impulso-1");
    }

    public void speak(String text, float rate, String voiceName, Runnable onFailure) {
        this.failure = onFailure;
        if (!ready || tts == null) {
            if (onFailure != null) {
                onFailure.run();
            }
            return;
        }
        prepareVoice(rate, voiceName);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "impulso");
    }

    private void prepareVoice(float rate, String voiceName) {
        if (voiceName != null && !voiceName.isEmpty() && tts.getVoices() != null) {
            for (Voice voice : tts.getVoices()) {
                if (voiceName.equals(voice.getName())) {
                    tts.setVoice(voice);
                    break;
                }
            }
        }
        tts.setSpeechRate(rate);
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void shutdown() {
        ready = false;
        spanishAvailable = false;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    private boolean applySpanish() {
        if (tts == null) {
            return false;
        }
        return supported(tts.setLanguage(new Locale("es", "ES")))
                || supported(tts.setLanguage(new Locale("es", "MX")))
                || supported(tts.setLanguage(new Locale("es")));
    }

    private static boolean supported(int result) {
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
    }
}
