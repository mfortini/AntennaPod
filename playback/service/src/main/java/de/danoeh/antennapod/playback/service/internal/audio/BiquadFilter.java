package de.danoeh.antennapod.playback.service.internal.audio;

public class BiquadFilter {
    private float b0;
    private float b1;
    private float b2;
    private float a1;
    private float a2;
    private float z1;
    private float z2;

    public void setHighPass(float sampleRate, float cutoffHz, float q) {
        double w0 = 2.0 * Math.PI * cutoffHz / sampleRate;
        double cosw0 = Math.cos(w0);
        double alpha = Math.sin(w0) / (2.0 * q);
        double a0 = 1.0 + alpha;
        b0 = (float) ((1.0 + cosw0) / 2.0 / a0);
        b1 = (float) (-(1.0 + cosw0) / a0);
        b2 = (float) ((1.0 + cosw0) / 2.0 / a0);
        a1 = (float) (-2.0 * cosw0 / a0);
        a2 = (float) ((1.0 - alpha) / a0);
        reset();
    }

    public void setPeaking(float sampleRate, float centerHz, float q, float gainDb) {
        double a = Math.pow(10.0, gainDb / 40.0);
        double w0 = 2.0 * Math.PI * centerHz / sampleRate;
        double cosw0 = Math.cos(w0);
        double alpha = Math.sin(w0) / (2.0 * q);
        double a0 = 1.0 + alpha / a;
        b0 = (float) ((1.0 + alpha * a) / a0);
        b1 = (float) (-2.0 * cosw0 / a0);
        b2 = (float) ((1.0 - alpha * a) / a0);
        a1 = (float) (-2.0 * cosw0 / a0);
        a2 = (float) ((1.0 - alpha / a) / a0);
        reset();
    }

    public void reset() {
        z1 = 0f;
        z2 = 0f;
    }

    public float process(float x) {
        float y = b0 * x + z1;
        z1 = b1 * x - a1 * y + z2;
        z2 = b2 * x - a2 * y;
        return y;
    }
}
