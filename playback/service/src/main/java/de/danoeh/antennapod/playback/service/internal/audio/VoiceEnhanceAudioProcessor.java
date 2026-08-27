package de.danoeh.antennapod.playback.service.internal.audio;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.BaseAudioProcessor;
import androidx.media3.common.util.UnstableApi;

import java.nio.ByteBuffer;

@OptIn(markerClass = UnstableApi.class)
public class VoiceEnhanceAudioProcessor extends BaseAudioProcessor {
    private final VoiceEnhancer enhancer = new VoiceEnhancer();
    private float[] scratch;

    public void setEnabled(boolean enabled) {
        enhancer.setEnabled(enabled);
    }

    @Override
    protected AudioProcessor.AudioFormat onConfigure(AudioProcessor.AudioFormat inputAudioFormat)
            throws AudioProcessor.UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT
                && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw new AudioProcessor.UnhandledAudioFormatException(inputAudioFormat);
        }
        enhancer.configure(inputAudioFormat.sampleRate, inputAudioFormat.channelCount);
        return inputAudioFormat;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        int remaining = inputBuffer.remaining();
        if (remaining == 0) {
            return;
        }
        ByteBuffer output = replaceOutputBuffer(remaining);
        if (!enhancer.isEnabled()) {
            output.put(inputBuffer);
            output.flip();
            return;
        }
        int sampleCount = remaining / PcmConversion.bytesPerSample(inputAudioFormat.encoding);
        ensureScratch(sampleCount);
        PcmConversion.decode(inputBuffer, inputAudioFormat.encoding, scratch, sampleCount);
        enhancer.process(scratch, 0, sampleCount);
        PcmConversion.encode(scratch, sampleCount, output, inputAudioFormat.encoding);
        output.flip();
    }

    @Override
    protected void onFlush() {
        enhancer.reset();
    }

    @Override
    protected void onReset() {
        enhancer.reset();
        scratch = null;
    }

    private void ensureScratch(int sampleCount) {
        if (scratch == null || scratch.length < sampleCount) {
            scratch = new float[sampleCount];
        }
    }
}
