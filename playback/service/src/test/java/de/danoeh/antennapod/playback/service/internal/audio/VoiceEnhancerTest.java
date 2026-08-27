package de.danoeh.antennapod.playback.service.internal.audio;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class VoiceEnhancerTest {
    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 1;

    @Test
    public void presenceBandIsBoosted() {
        VoiceEnhancer enhancer = createEnabledEnhancer();
        float[] samples = sine(2500f, 0.5f);
        float inputRms = settledRms(samples);
        enhancer.process(samples, 0, samples.length);
        float outputRms = settledRms(samples);
        assertTrue(outputRms > inputRms * 1.3f);
    }

    @Test
    public void rumbleIsQuieterThanPresence() {
        VoiceEnhancer enhancer = createEnabledEnhancer();
        float[] rumble = sine(80f, 0.5f);
        enhancer.process(rumble, 0, rumble.length);
        enhancer.reset();
        float[] presence = sine(2500f, 0.5f);
        enhancer.process(presence, 0, presence.length);
        assertTrue(settledRms(rumble) < settledRms(presence));
    }

    @Test
    public void disabledIsPassthrough() {
        VoiceEnhancer enhancer = new VoiceEnhancer();
        enhancer.configure(SAMPLE_RATE, CHANNELS);
        enhancer.setEnabled(false);
        float[] samples = sine(1000f, 0.2f);
        float[] original = samples.clone();
        enhancer.process(samples, 0, samples.length);
        assertArrayEquals(original, samples, 0f);
    }

    private static VoiceEnhancer createEnabledEnhancer() {
        VoiceEnhancer enhancer = new VoiceEnhancer();
        enhancer.configure(SAMPLE_RATE, CHANNELS);
        enhancer.setEnabled(true);
        return enhancer;
    }

    private static float[] sine(float frequencyHz, float seconds) {
        int n = Math.round(SAMPLE_RATE * seconds);
        float[] samples = new float[n];
        double step = 2.0 * Math.PI * frequencyHz / SAMPLE_RATE;
        for (int i = 0; i < n; i++) {
            samples[i] = 0.5f * (float) Math.sin(step * i);
        }
        return samples;
    }

    private static float settledRms(float[] samples) {
        int start = samples.length / 2;
        double sum = 0;
        int count = samples.length - start;
        for (int i = start; i < samples.length; i++) {
            sum += (double) samples[i] * samples[i];
        }
        return (float) Math.sqrt(sum / count);
    }
}
