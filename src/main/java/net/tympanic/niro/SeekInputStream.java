package net.tympanic.niro;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class SeekInputStream extends InputStream {
    private final byte[] buffer;
    private int position = 0;

    public SeekInputStream(byte[] data) {
        this.buffer = data;
    }

    public void seek(long pos) throws IOException {
        if (pos < 0 || pos > buffer.length) {
            throw new IOException("Seek position out of bounds");
        }
        this.position = Math.toIntExact(pos);
    }

    public int getPosition() {
        return position;
    }

    public int getLength() {
        return buffer.length;
    }

    @Override
    public int read() {
        return (position < buffer.length) ? (buffer[position++] & 0xFF) : -1;
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) {
        if (position >= buffer.length) return -1;

        int available = buffer.length - position;
        int bytesRead = Math.min(len, available);
        System.arraycopy(buffer, position, b, off, bytesRead);
        position += bytesRead;
        return bytesRead;
    }

    @Override
    public long skip(long n) {
        long k = Math.min(n, buffer.length - position);
        position += k;
        return k;
    }

    @Override
    public int available() {
        return buffer.length - position;
    }

    @Override
    public void close() {
    }
}

