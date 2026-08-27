package de.danoeh.antennapod.playback.service.internal.audio;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class DynamicsCompressorTest {
    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 1;

    @Test
    public void loudSineIsReduced() {
        DynamicsCompressor compressor = createEnabledCompressor();
        float[] samples = sine(-6f, 0.5f);
        float inputRms = rms(samples, 0, samples.length);
        compressor.process(samples, 0, samples.length);
        float outputRms = rms(samples, SAMPLE_RATE / 10, samples.length);
        assertTrue(outputRms < inputRms * 0.9f);
    }

    @Test
    public void quietSineIsUnchanged() {
        DynamicsCompressor compressor = createEnabledCompressor();
        float[] samples = sine(-40f, 0.3f);
        float[] original = samples.clone();
        compressor.process(samples, 0, samples.length);
        assertArrayEquals(original, samples, 1e-6f);
    }

    @Test
    public void disabledIsPassthrough() {
        DynamicsCompressor compressor = new DynamicsCompressor();
        compressor.configure(SAMPLE_RATE, CHANNELS);
        compressor.setEnabled(false);
        float[] samples = sine(-6f, 0.2f);
        float[] original = samples.clone();
        compressor.process(samples, 0, samples.length);
        assertArrayEquals(original, samples, 0f);
    }

    @Test
    public void loudBurstReducesLevelDifference() {
        DynamicsCompressor compressor = createEnabledCompressor();
        float quietSec = 0.4f;
        float[] original = burst(-30f, -3f, quietSec, 0.4f);
        float[] samples = original.clone();
        compressor.process(samples, 0, samples.length);
        int loudStart = Math.round(SAMPLE_RATE * quietSec);
        int settle = SAMPLE_RATE / 10;
        float inputQuiet = rms(original, 0, loudStart);
        float inputLoud = rms(original, loudStart + settle, original.length);
        float outputQuiet = rms(samples, 0, loudStart);
        float outputLoud = rms(samples, loudStart + settle, samples.length);
        float inputRangeDb = (float) (20.0 * Math.log10(inputLoud / inputQuiet));
        float outputRangeDb = (float) (20.0 * Math.log10(outputLoud / outputQuiet));
        assertTrue(outputRangeDb < inputRangeDb);
    }

    private static DynamicsCompressor createEnabledCompressor() {
        DynamicsCompressor compressor = new DynamicsCompressor();
        compressor.configure(SAMPLE_RATE, CHANNELS);
        compressor.setEnabled(true);
        compressor.setThresholdDb(-20f);
        compressor.setRatio(1.8f);
        return compressor;
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

    private static float[] burst(float quietDbfs, float loudDbfs, float quietSec, float loudSec) {
        float[] quiet = sine(quietDbfs, quietSec);
        float[] loud = sine(loudDbfs, loudSec);
        float[] samples = new float[quiet.length + loud.length];
        System.arraycopy(quiet, 0, samples, 0, quiet.length);
        System.arraycopy(loud, 0, samples, quiet.length, loud.length);
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
}

