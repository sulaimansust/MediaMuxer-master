package com.ipvision.test.mediamuxer_master_sunny_merge_on_process;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.text.format.Time;
import android.util.Log;



import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;

/**
 * Created by ip vision on 12/17/2016.
 */

public class RecordScreen {

    public static final int SAMPLE_RATE = 8000;
    protected int bitsPerSamples = 16;
    private boolean mIsRecording;
    String rootDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();


    public void changeRecordingStatus(boolean mIsRecording) {

        this.mIsRecording = mIsRecording;
    }

    public File getFile(final String suffix) {

        Time time = new Time();
        time.setToNow();
        return new File(rootDirectory, time.format("%Y%m%d%H%M%S") + "." + suffix);
    }

    public void startBufferedWrite(final File file, final AudioRecord mRecorder, final short[] mBuffer) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                DataOutputStream output = null;
                try {

                    output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                    while (mIsRecording) {
                        double sum = 0;

                        int readSize = mRecorder.read(mBuffer, 0,
                                mBuffer.length);

                        final int bytesPerSample = bitsPerSamples / 8;
                        final int emptySpace = 64 - bitsPerSamples;
                        int byteIndex = 0;
                        int byteIndex2 = 0;
                        int temp = 0;
                        int mLeftTemp = 0;
                        int mRightTemp = 0;
                        int a = 0;
                        int x = 0;

                        for (int frameIndex = 0; frameIndex < readSize; frameIndex++) {

                           /* for (int c = 0; c < 1; c++) {

                                if (iGain != 1) {

                                    long accumulator = 0;
                                    for (int b = 0; b < bytesPerSample; b++) {

                                        accumulator += ((long) (mBuffer[byteIndex++] & 0xFF)) << (b * 8 + emptySpace);
                                    }

                                    double sample = ((double) accumulator / (double) Long.MAX_VALUE);
                                    sample *= iGain;
                                    int intValue = (int) ((double) sample * (double) Integer.MAX_VALUE);

                                    for (int i = 0; i < bytesPerSample; i++) {
                                        mBuffer[i + byteIndex2] = (byte) (intValue >>> ((i + 2) * 8) & 0xff);
                                    }
                                    byteIndex2 += bytesPerSample;

                                }
                            }// end for(channel)

                            // mBuffer[frameIndex] *=iGain;
                            if (mBuffer[frameIndex] > 32765) {
                                mBuffer[frameIndex] = 32767;

                            } else if (mBuffer[frameIndex] < -32767) {
                                mBuffer[frameIndex] = -32767;
                            }
*/
                            output.writeShort(mBuffer[frameIndex]);
                            sum += mBuffer[frameIndex] * mBuffer[frameIndex];

                        }

                        if (readSize > 0) {
                            final double amplitude = sum / readSize;

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {

                    if (output != null) {
                        try {
                            output.flush();
                        } catch (IOException e) {

                            e.printStackTrace();
                        } finally {
                            try {
                                output.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();
    }


    public void rawToWave(final File rawFile, final File waveFile)
            throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {

            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, SAMPLE_RATE); // sample rate
            writeInt(output, SAMPLE_RATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);

            for (short s : shorts) {

                // Apply Gain
            /*
             * s *= iGain; if(s>32767) { s=32767; } else if(s<-32768) {
             * s=-32768; }
             */
                bytes.putShort(s);
            }
            output.write(bytes.array());
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private void writeInt(final DataOutputStream output, final int value)
            throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value)
            throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value)
            throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean wavToMP4(final File inputAudioWav, final File outputAudioMP4) {


        final String COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm";
        final int COMPRESSED_AUDIO_FILE_BIT_RATE = 8000; // 64kbps
        final int SAMPLING_RATE = 8000;
        final int BUFFER_SIZE = 8000;
        final int CODEC_TIMEOUT_IN_MS = 5000;
        final String LOGTAG = "CONVERT AUDIO";

        try {
            //String filePath = Environment.getExternalStorageDirectory().getPath() + "/" + AUDIO_RECORDING_FILE_NAME;
            //File inputFile = new File(filePath);
            FileInputStream fis = new FileInputStream(inputAudioWav);

//                    File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + COMPRESSED_AUDIO_FILE_NAME);
            if (outputAudioMP4.exists()) outputAudioMP4.delete();

            MediaMuxer mux = new MediaMuxer(outputAudioMP4.toString(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            MediaFormat outputFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE, SAMPLING_RATE, 1);
            outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, COMPRESSED_AUDIO_FILE_BIT_RATE);
            outputFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE);

            MediaCodec codec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE);
            codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();

            ByteBuffer[] codecInputBuffers = codec.getInputBuffers(); // Note: Array of buffers
            ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

            MediaCodec.BufferInfo outBuffInfo = new MediaCodec.BufferInfo();
            byte[] tempBuffer = new byte[BUFFER_SIZE];
            boolean hasMoreData = true;
            double presentationTimeUs = 0;
            int audioTrackIdx = 0;
            int totalBytesRead = 0;
            int percentComplete = 0;
            do {
                int inputBufIndex = 0;
                while (inputBufIndex != -1 && hasMoreData) {
                    inputBufIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_IN_MS);

                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                        dstBuf.clear();

                        int bytesRead = fis.read(tempBuffer, 0, dstBuf.limit());
                        Log.e("bytesRead", "Readed " + bytesRead);
                        if (bytesRead == -1) { // -1 implies EOS
                            hasMoreData = false;
                            codec.queueInputBuffer(inputBufIndex, 0, 0, (long) presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            totalBytesRead += bytesRead;
                            dstBuf.put(tempBuffer, 0, bytesRead);
                            codec.queueInputBuffer(inputBufIndex, 0, BUFFER_SIZE, (long) presentationTimeUs, 0);
                            presentationTimeUs = 1000000l * (totalBytesRead / 2) / SAMPLING_RATE;

                        }
                    }
                }
                // Drain audio
                int outputBufIndex = 0;
                while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    outputBufIndex = codec.dequeueOutputBuffer(outBuffInfo, CODEC_TIMEOUT_IN_MS);
                    if (outputBufIndex >= 0) {
                        ByteBuffer encodedData = codecOutputBuffers[outputBufIndex];
                        encodedData.position(outBuffInfo.offset);
                        encodedData.limit(outBuffInfo.offset + outBuffInfo.size);
                        if ((outBuffInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && outBuffInfo.size != 0) {
                            codec.releaseOutputBuffer(outputBufIndex, false);
                        } else {
                            mux.writeSampleData(audioTrackIdx, codecOutputBuffers[outputBufIndex], outBuffInfo);
                            codec.releaseOutputBuffer(outputBufIndex, false);
                        }
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        outputFormat = codec.getOutputFormat();
                        Log.v(LOGTAG, "Output format changed - " + outputFormat);
                        audioTrackIdx = mux.addTrack(outputFormat);
                        mux.start();
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        Log.e(LOGTAG, "Output buffers changed during encode!");
                    } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // NO OP
                    } else {
                        Log.e(LOGTAG, "Unknown return code from dequeueOutputBuffer - " + outputBufIndex);
                    }
                }
                percentComplete = (int) Math.round(((float) totalBytesRead / (float) inputAudioWav.length()) * 100.0);
                Log.v(LOGTAG, "Conversion % - " + percentComplete);
            } while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            fis.close();
            mux.stop();
            mux.release();
            Log.v(LOGTAG, "Compression done ...");


        } catch (FileNotFoundException e) {
            Log.e(LOGTAG, "File not found!", e);
        } catch (IOException e) {
            Log.e(LOGTAG, "IO exception!", e);
        }

        //mStop = false;
        // Notify UI thread...


        return true;
    }


}
