package net.tympanic.niro;

import meteordevelopment.discordipc.connection.WinConnection;
import net.tympanic.niro.jogg.Page;
import net.tympanic.niro.jogg.SyncState;

import java.io.*;
import java.nio.file.Files;
import java.time.Instant;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;

/*
    playlists
    choose from files on pc or album art of songs for image on playlist or use first song

    integrate lucida and soulseek

    https://en.wikipedia.org/wiki/Nightcore implement https://discord.com/channels/1266530470213058610/1295014379762483242/1386893994990243970

    yt-dlp integration https://discord.com/channels/1266530470213058610/1295014379762483242/1386894481764388975

    lyrics reader and display, reading lyric files (synced to time)

    to anyone reading this shit ignore all the comments its from testing
*/
public class Niro {

    public static void main(String[] args) throws IOException {
//        byte[] image = Files.readAllBytes(new File("data/cover.webp").toPath());
//        byte[] audio = Files.readAllBytes(new File("data/song.opus").toPath());
//
//
//        SimpleMediaFile smf = SimpleMediaFile.create("I Don't Work", "penguinband", "Semi-Aquatic and Armed", image, audio);
//        smf.writeTo(new File("data/i-dont-work.smf1"));

        SimpleMediaFile smf = SimpleMediaFile.readFrom(new File("data/i-dont-work.smf1"));
        OpusPlayer player = new OpusPlayer();
        player.play(smf.AudioData);
        while (player.isPlaying()) {
            player.update();
        }

//        RichPresence presence = new RichPresence();
//
//        DiscordIPC.start(1386314342902792314L, () -> {
//            System.out.println("Logged in account: " + DiscordIPC.getUser().username);
//            presence.setLargeImage("test", "test");
//            presence.setSmallImage("test2", "test2");
//            presence.setStart(Instant.now().getEpochSecond());
//            DiscordIPC.setActivity(presence);
//
//            System.out.println(DiscordIPC.isConnected());
//        });

//        OpusPlayer player = new OpusPlayer();
//        player.play(new File("7bthqo.opus"));
//        player.seekTo(60*2+20);
//        long start = System.currentTimeMillis();
//        System.out.println(getDurationFormatted(player.getDurationSeconds()));
//        player.exitCallback = ()->{
//            System.out.println(getDurationFormatted(player.getCurrentTimeSeconds()));
//            player.seekTo(0);
//        };
//        while (player.isPlaying()) {
//            player.update();
//        }
//        System.out.println((System.currentTimeMillis()-start)/1000);

    }

    public static String getDurationFormatted(Double durationSeconds) {
        if (durationSeconds < 0) return "Unknown";

        int minutes = (int) (durationSeconds / 60);
        int seconds = (int) (durationSeconds % 60);

        return minutes + "m " + seconds + "s";
    }
}
