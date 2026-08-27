package de.danoeh.antennapod.playback.service.internal.audio;

public class VoiceEnhancer {
    private static final float HIGH_PASS_HZ = 100f;
    private static final float HIGH_PASS_Q = 0.70710678f;
    private static final float PEAK_HZ = 2500f;
    private static final float PEAK_Q = 0.7f;
    private static final float PEAK_GAIN_DB = 5f;

    private volatile boolean enabled;
    private int channelCount = 1;
    private BiquadFilter[] highPass;
    private BiquadFilter[] peaking;

    public void configure(int sampleRate, int channelCount) {
        this.channelCount = Math.max(1, channelCount);
        highPass = new BiquadFilter[this.channelCount];
        peaking = new BiquadFilter[this.channelCount];
        for (int ch = 0; ch < this.channelCount; ch++) {
            highPass[ch] = new BiquadFilter();
            highPass[ch].setHighPass(sampleRate, HIGH_PASS_HZ, HIGH_PASS_Q);
            peaking[ch] = new BiquadFilter();
            peaking[ch].setPeaking(sampleRate, PEAK_HZ, PEAK_Q, PEAK_GAIN_DB);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void reset() {
        if (highPass == null) {
            return;
        }
        for (int ch = 0; ch < channelCount; ch++) {
            highPass[ch].reset();
            peaking[ch].reset();
        }
    }

    public void process(float[] samples, int offset, int length) {
        if (!enabled || highPass == null) {
            return;
        }
        int end = offset + length;
        for (int i = offset; i < end; i += channelCount) {
            int remaining = end - i;
            int channelsThisFrame = Math.min(channelCount, remaining);
            for (int ch = 0; ch < channelsThisFrame; ch++) {
                float x = samples[i + ch];
                samples[i + ch] = peaking[ch].process(highPass[ch].process(x));
            }
        }
    }
}
