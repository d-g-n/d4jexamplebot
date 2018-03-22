package com.github.decyg;

import com.github.decyg.lavaplayer.GuildMusicManager;
import com.github.decyg.lavaplayer.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

import java.util.*;

/**
 * Created by declan on 04/04/2017.
 */
public class CommandHandler {

    // A static map of commands mapping from command string to the functional impl
    private static Map<String, Command> commandMap = new HashMap<>();

    private static final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();;;
    private static final Map<Long, GuildMusicManager> musicManagers  = new HashMap<>();;

    // Statically populate the commandMap with the intended functionality
    // Might be better practise to do this from an instantiated objects constructor
    static {

        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        // If the IUser that called this is in a voice channel, join them
        commandMap.put("joinvoice", (event, args) -> {

            IVoiceChannel userVoiceChannel = event.getAuthor().getVoiceStateForGuild(event.getGuild()).getChannel();

            if(userVoiceChannel == null)
                return;

            userVoiceChannel.join();

        });

        commandMap.put("leavevoice", (event, args) -> {

            IVoiceChannel botVoiceChannel = event.getClient().getOurUser().getVoiceStateForGuild(event.getGuild()).getChannel();

            if(botVoiceChannel == null)
                return;

            TrackScheduler scheduler = getGuildAudioPlayer(event.getGuild()).getScheduler();
            scheduler.getQueue().clear();
            scheduler.nextTrack();

            botVoiceChannel.leave();

        });

        // Plays the first song found containing the first arg
        commandMap.put("playsong", (event, args) -> {

            IVoiceChannel botVoiceChannel = event.getClient().getOurUser().getVoiceStateForGuild(event.getGuild()).getChannel();

            if(botVoiceChannel == null) {
                BotUtils.sendMessage(event.getChannel(), "Not in a voice channel, join one and then use joinvoice");
                return;
            }

            // Turn the args back into a string separated by space
            String searchStr = String.join(" ", args);

            loadAndPlay(event.getChannel(), searchStr);


        });

        // Skips the current song
        commandMap.put("skipsong", (event, args) -> {

            skipTrack(event.getChannel());

        });

        commandMap.put("exampleembed", (event, args) -> {

            EmbedBuilder builder = new EmbedBuilder();

            builder.appendField("fieldTitleInline", "fieldContentInline", true);
            builder.appendField("fieldTitleInline2", "fieldContentInline2", true);
            builder.appendField("fieldTitleNotInline", "fieldContentNotInline", false);
            builder.appendField(":tada: fieldWithCoolThings :tada:", "[hiddenLink](http://i.imgur.com/Y9utuDe.png)", false);

            builder.withAuthorName("authorName");
            builder.withAuthorIcon("http://i.imgur.com/PB0Soqj.png");
            builder.withAuthorUrl("http://i.imgur.com/oPvYFj3.png");

            builder.withColor(255, 0, 0);
            builder.withDesc("withDesc");
            builder.withDescription("withDescription");
            builder.withTitle("withTitle");
            builder.withTimestamp(100);
            builder.withUrl("http://i.imgur.com/IrEVKQq.png");
            builder.withImage("http://i.imgur.com/agsp5Re.png");

            builder.withFooterIcon("http://i.imgur.com/Ch0wy1e.png");
            builder.withFooterText("footerText");
            builder.withFooterIcon("http://i.imgur.com/TELh8OT.png");
            builder.withThumbnail("http://www.gstatic.com/webp/gallery/1.webp");

            builder.appendDesc(" + appendDesc");
            builder.appendDescription(" + appendDescription");

            RequestBuffer.request(() -> event.getChannel().sendMessage(builder.build()));

        });

    }

    private static synchronized GuildMusicManager getGuildAudioPlayer(IGuild guild) {
        long guildId = guild.getLongID();
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());

        return musicManager;
    }

    private static void loadAndPlay(final IChannel channel, final String trackUrl) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                BotUtils.sendMessage(channel, "Adding to queue " + track.getInfo().title);

                play(musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                BotUtils.sendMessage(channel, "Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")");

                play(musicManager, firstTrack);
            }

            @Override
            public void noMatches() {
                BotUtils.sendMessage(channel, "Nothing found by " + trackUrl);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                BotUtils.sendMessage(channel, "Could not play: " + exception.getMessage());
            }
        });
    }

    private static void play(GuildMusicManager musicManager, AudioTrack track) {

        musicManager.getScheduler().queue(track);
    }

    private static void skipTrack(IChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.getScheduler().nextTrack();

        BotUtils.sendMessage(channel, "Skipped to next track.");
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event) {

        // Note for error handling, you'll probably want to log failed commands with a logger or sout
        // In most cases it's not advised to annoy the user with a reply incase they didn't intend to trigger a
        // command anyway, such as a user typing ?notacommand, the bot should not say "notacommand" doesn't exist in
        // most situations. It's partially good practise and partially developer preference

        // Given a message "/test arg1 arg2", argArray will contain ["/test", "arg1", "arg"]
        String[] argArray = event.getMessage().getContent().split(" ");

        // First ensure at least the command and prefix is present, the arg length can be handled by your command func
        if(argArray.length == 0)
            return;

        // Check if the first arg (the command) starts with the prefix defined in the utils class
        if(!argArray[0].startsWith(BotUtils.BOT_PREFIX))
            return;

        // Extract the "command" part of the first arg out by ditching the amount of characters present in the prefix
        String commandStr = argArray[0].substring(BotUtils.BOT_PREFIX.length());

        // Load the rest of the args in the array into a List for safer access
        List<String> argsList = new ArrayList<>(Arrays.asList(argArray));
        argsList.remove(0); // Remove the command

        // Instead of delegating the work to a switch, automatically do it via calling the mapping if it exists

        if(commandMap.containsKey(commandStr))
            commandMap.get(commandStr).runCommand(event, argsList);

    }

}
