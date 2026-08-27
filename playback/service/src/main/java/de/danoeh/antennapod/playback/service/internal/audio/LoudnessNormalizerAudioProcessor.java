package de.danoeh.antennapod.playback.service.internal.audio;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.BaseAudioProcessor;
import androidx.media3.common.util.UnstableApi;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import java.nio.ByteBuffer;

@OptIn(markerClass = UnstableApi.class)
public class LoudnessNormalizerAudioProcessor extends BaseAudioProcessor {
    private final DynamicsCompressor compressor = new DynamicsCompressor();
    private final LoudnessAgc agc = new LoudnessAgc();
    private float[] scratch;

    public void applyLevel(String level) {
        boolean enabled = !UserPreferences.AUDIO_NORMALIZATION_OFF.equals(level);
        compressor.setEnabled(enabled);
        agc.setEnabled(enabled);
        if (UserPreferences.AUDIO_NORMALIZATION_HIGH.equals(level)) {
            compressor.setRatio(4f);
            compressor.setThresholdDb(-24f);
            agc.setTargetDbfs(-14f);
            agc.setMaxGainDb(24f);
        } else if (UserPreferences.AUDIO_NORMALIZATION_MEDIUM.equals(level)) {
            compressor.setRatio(2.5f);
            compressor.setThresholdDb(-22f);
            agc.setTargetDbfs(-16f);
            agc.setMaxGainDb(18f);
        } else {
            compressor.setRatio(1.8f);
            compressor.setThresholdDb(-20f);
            agc.setTargetDbfs(-18f);
            agc.setMaxGainDb(12f);
        }
    }

    public float getCurrentGainDb() {
        return agc.getCurrentGainDb();
    }

    @Override
    protected AudioProcessor.AudioFormat onConfigure(AudioProcessor.AudioFormat inputAudioFormat)
            throws AudioProcessor.UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT
                && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw new AudioProcessor.UnhandledAudioFormatException(inputAudioFormat);
        }
        compressor.configure(inputAudioFormat.sampleRate, inputAudioFormat.channelCount);
        agc.configure(inputAudioFormat.sampleRate, inputAudioFormat.channelCount);
        return inputAudioFormat;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        int remaining = inputBuffer.remaining();
        if (remaining == 0) {
            return;
        }
        ByteBuffer output = replaceOutputBuffer(remaining);
        if (!agc.isEnabled()) {
            output.put(inputBuffer);
            output.flip();
            return;
        }
        int sampleCount = remaining / PcmConversion.bytesPerSample(inputAudioFormat.encoding);
        ensureScratch(sampleCount);
        PcmConversion.decode(inputBuffer, inputAudioFormat.encoding, scratch, sampleCount);
        compressor.process(scratch, 0, sampleCount);
        agc.process(scratch, 0, sampleCount);
        PcmConversion.encode(scratch, sampleCount, output, inputAudioFormat.encoding);
        output.flip();
    }

    @Override
    protected void onFlush() {
        compressor.reset();
        agc.reset();
    }

    @Override
    protected void onReset() {
        compressor.reset();
        agc.reset();
        scratch = null;
    }

    private void ensureScratch(int sampleCount) {
        if (scratch == null || scratch.length < sampleCount) {
            scratch = new float[sampleCount];
        }
    }
}
