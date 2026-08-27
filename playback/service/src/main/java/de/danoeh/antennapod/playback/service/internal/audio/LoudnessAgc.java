package de.danoeh.antennapod.playback.service.internal.audio;

public class LoudnessAgc {
    public static final float TARGET_DBFS = -18f;
    public static final float MAX_GAIN_DB = 12f;
    public static final float MIN_GAIN_DB = -10f;
    public static final float GATE_DBFS = -50f;
    private static final float WINDOW_MS = 50f;
    private static final float ATTACK_MS = 20f;
    private static final float RELEASE_MS = 2000f;
    private static final float LIMITER_KNEE = 0.95f;

    private volatile boolean enabled;
    private volatile float targetDbfs = TARGET_DBFS;
    private volatile float maxGainDb = MAX_GAIN_DB;
    private int channelCount = 1;
    private int windowSizeFrames;
    private int framesInWindow;
    private double sumSquares;
    private float currentGainDb;
    private float currentGainLin = 1f;
    private float attackCoef;
    private float releaseCoef;

    public void configure(int sampleRate, int channelCount) {
        this.channelCount = Math.max(1, channelCount);
        windowSizeFrames = Math.max(1, Math.round(sampleRate * WINDOW_MS / 1000f));
        float windowSec = windowSizeFrames / (float) sampleRate;
        attackCoef = 1f - (float) Math.exp(-windowSec / (ATTACK_MS / 1000f));
        releaseCoef = 1f - (float) Math.exp(-windowSec / (RELEASE_MS / 1000f));
        reset();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setTargetDbfs(float targetDbfs) {
        this.targetDbfs = targetDbfs;
    }

    public void setMaxGainDb(float maxGainDb) {
        this.maxGainDb = maxGainDb;
    }

    public float getCurrentGainDb() {
        return currentGainDb;
    }

    public void reset() {
        framesInWindow = 0;
        sumSquares = 0;
        currentGainDb = 0f;
        currentGainLin = 1f;
    }

    public void process(float[] samples, int offset, int length) {
        if (!enabled || windowSizeFrames <= 0) {
            return;
        }
        int end = offset + length;
        for (int i = offset; i < end; i += channelCount) {
            int remaining = end - i;
            int channelsThisFrame = Math.min(channelCount, remaining);
            for (int ch = 0; ch < channelsThisFrame; ch++) {
                float x = samples[i + ch];
                sumSquares += (double) x * x;
                samples[i + ch] = softLimit(x * currentGainLin);
            }
            framesInWindow++;
            if (framesInWindow >= windowSizeFrames) {
                updateGain();
            }
        }
    }

    private void updateGain() {
        int sampleCount = framesInWindow * channelCount;
        float rms = sampleCount == 0 ? 0f : (float) Math.sqrt(sumSquares / sampleCount);
        float levelDb = rms < 1e-9f ? -100f : (float) (20.0 * Math.log10(rms));
        float desiredDb = currentGainDb;
        if (levelDb >= GATE_DBFS) {
            desiredDb = clamp(targetDbfs - levelDb, MIN_GAIN_DB, maxGainDb);
        }
        float coef = desiredDb < currentGainDb ? attackCoef : releaseCoef;
        currentGainDb += coef * (desiredDb - currentGainDb);
        currentGainLin = (float) Math.pow(10.0, currentGainDb / 20.0);
        framesInWindow = 0;
        sumSquares = 0;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float softLimit(float x) {
        float ax = Math.abs(x);
        if (ax <= LIMITER_KNEE) {
            return x;
        }
        float sign = x < 0f ? -1f : 1f;
        float excess = ax - LIMITER_KNEE;
        float compressed = LIMITER_KNEE
                + (1f - LIMITER_KNEE) * (float) Math.tanh(excess / (1f - LIMITER_KNEE));
        return sign * compressed;
    }
}
