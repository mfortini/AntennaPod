package de.danoeh.antennapod.playback.service.internal.audio;

import androidx.media3.common.C;

import java.nio.ByteBuffer;

final class PcmConversion {
    private PcmConversion() {
    }

    static int bytesPerSample(int encoding) {
        return encoding == C.ENCODING_PCM_FLOAT ? 4 : 2;
    }

    static void decode(ByteBuffer input, int encoding, float[] out, int sampleCount) {
        if (encoding == C.ENCODING_PCM_FLOAT) {
            for (int i = 0; i < sampleCount; i++) {
                out[i] = input.getFloat();
            }
        } else {
            for (int i = 0; i < sampleCount; i++) {
                out[i] = input.getShort() / 32768f;
            }
        }
    }

    static void encode(float[] in, int sampleCount, ByteBuffer output, int encoding) {
        if (encoding == C.ENCODING_PCM_FLOAT) {
            for (int i = 0; i < sampleCount; i++) {
                output.putFloat(in[i]);
            }
        } else {
            for (int i = 0; i < sampleCount; i++) {
                float s = in[i];
                if (s > 1f) {
                    s = 1f;
                } else if (s < -1f) {
                    s = -1f;
                }
                output.putShort((short) Math.round(s * 32767f));
            }
        }
    }
}
