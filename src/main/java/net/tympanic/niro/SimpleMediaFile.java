package net.tympanic.niro;

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class SimpleMediaFile {
    // Header constants
    public static final String MAGIC = "SMF1";
    public static final byte VERSION = 0x01;

    // Fields
    public byte Flags;
    public short Reserved = 0; // Always 0

    // Text section
    public String Title;
    public String Artist;
    public String Album;

    // Binary sections
    public byte[] ImageData;
    public byte[] AudioData;

    // Flags
    public static final byte FLAG_TEXT = 0x01;
    public static final byte FLAG_IMAGE = 0x02;
    public static final byte FLAG_AUDIO = 0x04;
    public static final byte FLAG_SINGLE = 0x08;

    public void writeTo(File file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            out.writeBytes(MAGIC);
            out.writeByte(VERSION);
            out.writeByte(Flags);
            out.writeShort(Reserved);

            if ((Flags & FLAG_TEXT) != 0) {
                writeString(out, Title);
                writeString(out, Artist);
                if ((Flags & FLAG_SINGLE) == 0) {
                    writeString(out, Album);
                }
            }

            if ((Flags & FLAG_IMAGE) != 0) {
                out.writeInt(ImageData.length);
                out.write(ImageData);
            }

            if ((Flags & FLAG_AUDIO) != 0) {
                out.writeInt(AudioData.length);
                out.write(AudioData);
            }
        }
    }

    public static SimpleMediaFile readFrom(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            byte[] magicBytes = new byte[4];
            in.readFully(magicBytes);
            String magic = new String(magicBytes, StandardCharsets.US_ASCII);
            if (!MAGIC.equals(magic)) throw new IOException("Invalid magic header");

            byte version = in.readByte();
            if (version != VERSION) throw new IOException("Unsupported version");

            SimpleMediaFile smf = new SimpleMediaFile();
            smf.Flags = in.readByte();
            smf.Reserved = in.readShort();

            if ((smf.Flags & FLAG_TEXT) != 0) {
                smf.Title = readString(in);
                smf.Artist = readString(in);
                if ((smf.Flags & FLAG_SINGLE) == 0) {
                    smf.Album = readString(in);
                }
            }

            if ((smf.Flags & FLAG_IMAGE) != 0) {
                int imageSize = in.readInt();
                smf.ImageData = in.readNBytes(imageSize);
            }

            if ((smf.Flags & FLAG_AUDIO) != 0) {
                int audioSize = in.readInt();
                smf.AudioData = in.readNBytes(audioSize);
            }

            return smf;
        }
    }

    private static void writeString(DataOutputStream out, String str) throws IOException {
        byte[] data = str.getBytes(StandardCharsets.UTF_8);
        out.writeShort(data.length);
        out.write(data);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        byte[] data = in.readNBytes(length);
        return new String(data, StandardCharsets.UTF_8);
    }

    public static SimpleMediaFile create(String title, String artist, String album, byte[] image, byte[] audio) {
        SimpleMediaFile smf = new SimpleMediaFile();
        smf.Title = title;
        smf.Artist = artist;
        smf.Album = album;
        smf.ImageData = image;
        smf.AudioData = audio;
        smf.Flags = 0;
        if (title != null && artist != null) smf.Flags |= FLAG_TEXT;
        if (image != null) smf.Flags |= FLAG_IMAGE;
        if (audio != null) smf.Flags |= FLAG_AUDIO;
        if (album == null) smf.Flags |= FLAG_SINGLE;
        return smf;
    }
}

