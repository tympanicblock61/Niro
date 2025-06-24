package net.tympanic.niro;

import io.github.jaredmdobson.concentus.OpusDecoder;
import io.github.jaredmdobson.concentus.OpusException;
import net.tympanic.niro.jogg.Packet;
import net.tympanic.niro.jogg.Page;
import net.tympanic.niro.jogg.StreamState;
import net.tympanic.niro.jogg.SyncState;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.exit;

public class OpusPlayer {
    private long totalSamplesDecoded = 0;
    private final int sampleRate = 48000;
    private byte[] currentBytes = null;
    private SeekInputStream stream;
    private OpusDecoder decoder;
    private SourceDataLine line;
    private SyncState syncState;
    private StreamState streamState;
    private final Page page = new Page();
    private final Packet packet = new Packet();
    private boolean streamInitialized = false;
    private boolean ready = false;
    public boolean playing = false;

    private final List<Long> granulePositions = new ArrayList<>();
    private final List<Long> pageOffsets = new ArrayList<>();
    private double durationSeconds = -1;

    private final short[] decoded = new short[1920 * 2];
    private final byte[] pcmBuffer = new byte[1920 * 2 * 2];

    public Runnable exitCallback = null;

    public void play(byte[] bytes) {
        this.currentBytes = bytes;
        play(new SeekInputStream(bytes));
    }

    public void play(File file) {
        try {
            InputStream input = new BufferedInputStream(new FileInputStream(file));
            this.currentBytes = input.readAllBytes();
            play(this.currentBytes);
        } catch (IOException e) {
            System.err.println("Error while playing opus file "+file.getAbsolutePath()+": "+e.getMessage());
        }
    }

    public void play(SeekInputStream input) {
        try {
            stop();
            stream = input;

            decoder = new OpusDecoder(sampleRate, 2);

            AudioFormat format = new AudioFormat(sampleRate, 16, 2, true, false);
            line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();

            syncState = new SyncState();
            syncState.init();

            streamState = new StreamState();
            streamInitialized = false;

            totalSamplesDecoded = 0;

            granulePositions.clear();
            pageOffsets.clear();

            durationSeconds = getOpusDuration();
            stream.seek(0);

            playing = true;
            ready = true;

        } catch (Exception e) {
            System.err.println("Error while initializing playback: " + e.getMessage());
            ready = false;
        }
    }

    public void update() {
        if (!playing || !ready) return;
        syncState.buffer(4096);
        byte[] buffer = syncState.data;
        int offset = syncState.getBufferOffset();
        int bytesRead;
        try {
            bytesRead = stream.read(buffer, offset, 4096);
        } catch (Exception e) {
            System.err.println("Error while reading from stream: "+e.getMessage());
            return;
        }
        System.out.println("byte read: "+bytesRead);
        if (bytesRead <= 0) {
            if (exitCallback != null) exitCallback.run();
            return;
        }

        syncState.wrote(bytesRead);

        while (syncState.pageout(page) == 1) {
            if (!streamInitialized) {
                streamState.init(page.serialno());
                streamInitialized = true;
            }

            streamState.pagein(page);

            while (streamState.packetout(packet) == 1) {
                try {
                    int frameSize = decoder.decode(
                            packet.packet_base,
                            packet.packet,
                            packet.bytes,
                            decoded,
                            0,
                            960,
                            false
                    );
                    totalSamplesDecoded += frameSize;

                    int bytesToWrite = frameSize * 2 * 2;

                    for (int i = 0; i < frameSize * 2; i++) {
                        pcmBuffer[2 * i]     = (byte) (decoded[i] & 0xFF);
                        pcmBuffer[2 * i + 1] = (byte) ((decoded[i] >> 8) & 0xFF);
                    }

                    line.write(pcmBuffer, 0, bytesToWrite);
                } catch (OpusException e) {
                    System.err.println("Decode error: " + e.getMessage());
                    System.err.println("Decode error on packet of size " + packet.bytes + " at granule " + page.granulepos());
                }

            }
        }
    }

    public void stop() {
        playing = false;
        ready = false;

        try {
            if (line != null) {
                line.drain();
                line.stop();
                line.close();
                line = null;
            }
        } catch (Exception ignored) {}

        try {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        } catch (Exception ignored) {}

        if (syncState != null) syncState.clear();
        if (streamState != null) streamState.clear();
    }

    public boolean isPlaying() {
        return ready && playing;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public double getCurrentTimeSeconds() {
        return totalSamplesDecoded / (double) sampleRate;
    }

    public double getOpusDuration() {
        try {
            SyncState syncState = new SyncState();
            syncState.init();

            Page page = new Page();
            long lastGranule = 0;
            long totalBytesRead = 0;

            while (true) {
                syncState.buffer(4096);
                byte[] buffer = syncState.data;
                int offset = syncState.getBufferOffset();

                int bytesRead = stream.read(buffer, offset, 4096);
                if (bytesRead <= 0) break;

                long offsetBefore = totalBytesRead;
                totalBytesRead += bytesRead;

                syncState.wrote(bytesRead);

                while (syncState.pageout(page) == 1) {
                    long granule = page.granulepos();
                    if (granule >= 0) {
                        lastGranule = granule;
                        granulePositions.add(granule);
                        pageOffsets.add(offsetBefore);
                    }
                }
            }

            syncState.clear();
            return lastGranule > 0 ? lastGranule / 48000.0 : -1;
        } catch (Exception e) {
            System.err.println("Failed to calculate duration: " + e.getMessage());
            return -1;
        }
    }

    public void seekTo(double seconds) {
        playing = false;

        if (stream == null || pageOffsets.isEmpty()) {
            System.err.println("Cannot seek: missing source or offset cache.");
            return;
        }

        long targetSamples = (long) (seconds * sampleRate);
        int closestIndex = -1;

        for (int i = 0; i < granulePositions.size(); i++) {
            if (granulePositions.get(i) >= targetSamples) {
                closestIndex = i;
                break;
            }
        }

        if (closestIndex == -1) {
            System.err.println("Seek target out of range.");
            return;
        }

        try {
            stream.seek(pageOffsets.get(closestIndex));
            System.out.println(pageOffsets.get(closestIndex));
            totalSamplesDecoded = granulePositions.get(closestIndex);
        } catch (Exception e) {
            System.err.println("Seek error: " + e.getMessage());
        }

        playing = true;
    }
}