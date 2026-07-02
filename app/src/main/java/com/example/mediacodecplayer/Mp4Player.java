package com.example.mediacodecplayer;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Mp4Player {
    private String TAG = "Mp4Player";
    private final String filePath;
    private final Surface surface;
    private MediaExtractor extractor;
    private MediaCodec decoder;

    private volatile boolean isPaused = false;
    private volatile boolean isStopped = false;
    private final Object lock = new Object();

    public Mp4Player(String filePath, Surface surface) {
        this.filePath = filePath;
        this.surface = surface;
    }

    public void play(AssetFileDescriptor afd) throws IOException {
        extractor = new MediaExtractor();
        extractor.setDataSource(afd.getFileDescriptor(),
                afd.getStartOffset(),
                afd.getLength());
        // Find video track
        int videoTrackIndex = -1;
        MediaFormat format = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat trackFormat = extractor.getTrackFormat(i);
            String mime = trackFormat.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("video/")) {
                videoTrackIndex = i;
                format = trackFormat;
                break;
            }
        }

        if (videoTrackIndex < 0 || format == null) {
            throw new IOException("No video track found");
        }

        extractor.selectTrack(videoTrackIndex);

        String mime = format.getString(MediaFormat.KEY_MIME);
        decoder = MediaCodec.createDecoderByType(mime);
        decoder.configure(format, surface, null, 0);
        decoder.start();

        decode();
    }

    private void decode() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        boolean inputDone = false;
        boolean outputDone = false;
        long timeoutUs = 10_000;
        int index = 0;

        while (!outputDone && !isStopped) {

            // Pause check
            synchronized (lock) {
                while (isPaused && !isStopped) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            if (isStopped) break;

            // Feed input
            if (!inputDone) {
                Log.d(TAG, "decode: " + index);
                int inputIndex = decoder.dequeueInputBuffer(timeoutUs);
                if (inputIndex >= 0) {
                    ByteBuffer buffer = decoder.getInputBuffer(inputIndex);
                    int sampleSize = extractor.readSampleData(buffer, 0);

                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        decoder.queueInputBuffer(inputIndex, 0, sampleSize,
                                extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }
            index++;

            // Drain output
            int outputIndex = decoder.dequeueOutputBuffer(bufferInfo, timeoutUs);
            if (outputIndex >= 0) {
                boolean isConfig = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
                boolean render = !isConfig && bufferInfo.size > 0;
                decoder.releaseOutputBuffer(outputIndex, render);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputDone = true;
                }
            }
        }

        release();
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public void stop() {
        isStopped = true;
        resume(); // unblock if waiting
    }

    public void release() {
        if (decoder != null) {
            decoder.stop();
            decoder.release();
            decoder = null;
        }
        if (extractor != null) {
            extractor.release();
            extractor = null;
        }
    }

}