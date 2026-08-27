package de.danoeh.antennapod.playback.service.internal.audio;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LoudnessAgcTest {
    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 1;

    @Test
    public void quietSineIsBoosted() {
        LoudnessAgc agc = createEnabledAgc();
        float[] samples = sine(-30f, 3f);
        float inputRms = rms(samples, SAMPLE_RATE, samples.length);
        agc.process(samples, 0, samples.length);
        float outputRms = rms(samples, SAMPLE_RATE, samples.length);
        assertTrue(outputRms > inputRms * 1.4f);
    }

    @Test
    public void loudSineIsReducedWithoutClipping() {
        LoudnessAgc agc = createEnabledAgc();
        float[] samples = sine(-3f, 1f);
        float inputRms = rms(samples, 0, samples.length);
        agc.process(samples, 0, samples.length);
        float outputRms = rms(samples, SAMPLE_RATE / 5, samples.length);
        assertTrue(outputRms < inputRms * 0.7f);
        assertTrue(peak(samples) < 1f);
    }

    @Test
    public void silenceDoesNotBoostToMaxGain() {
        LoudnessAgc agc = createEnabledAgc();
        float[] samples = sine(-60f, 3f);
        agc.process(samples, 0, samples.length);
        assertTrue(Math.abs(agc.getCurrentGainDb()) < 1f);
    }

    @Test
    public void disabledIsPassthrough() {
        LoudnessAgc agc = new LoudnessAgc();
        agc.configure(SAMPLE_RATE, CHANNELS);
        agc.setEnabled(false);
        float[] samples = sine(-12f, 0.2f);
        float[] original = samples.clone();
        agc.process(samples, 0, samples.length);
        assertArrayEquals(original, samples, 0f);
        assertEquals(0f, agc.getCurrentGainDb(), 0f);
    }

    @Test
    public void higherMaxGainBoostsQuietSineMore() {
        LoudnessAgc limited = createEnabledAgc();
        limited.setMaxGainDb(12f);
        LoudnessAgc stronger = createEnabledAgc();
        stronger.setMaxGainDb(18f);
        float[] limitedSamples = sine(-32f, 3f);
        float[] strongerSamples = sine(-32f, 3f);
        limited.process(limitedSamples, 0, limitedSamples.length);
        stronger.process(strongerSamples, 0, strongerSamples.length);
        float limitedRms = rms(limitedSamples, SAMPLE_RATE, limitedSamples.length);
        float strongerRms = rms(strongerSamples, SAMPLE_RATE, strongerSamples.length);
        assertTrue(strongerRms > limitedRms);
    }

    private static LoudnessAgc createEnabledAgc() {
        LoudnessAgc agc = new LoudnessAgc();
        agc.configure(SAMPLE_RATE, CHANNELS);
        agc.setEnabled(true);
        return agc;
    }

    private static float[] sine(float dbfs, float seconds) {
        int n = Math.round(SAMPLE_RATE * seconds);
        float[] samples = new float[n];
        float amplitude = (float) Math.pow(10.0, dbfs / 20.0);
        double step = 2.0 * Math.PI * 1000.0 / SAMPLE_RATE;
        for (int i = 0; i < n; i++) {
            samples[i] = amplitude * (float) Math.sin(step * i);
        }
        return samples;
    }

    private static float rms(float[] samples, int start, int end) {
        double sum = 0;
        int count = Math.max(0, end - start);
        for (int i = start; i < end; i++) {
            sum += (double) samples[i] * samples[i];
        }
        return count == 0 ? 0f : (float) Math.sqrt(sum / count);
    }

    private static float peak(float[] samples) {
        float max = 0f;
        for (float sample : samples) {
            max = Math.max(max, Math.abs(sample));
        }
        return max;
    }
}
