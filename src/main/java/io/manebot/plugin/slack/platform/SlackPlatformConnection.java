package io.manebot.plugin.slack.platform;


import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import io.manebot.chat.Chat;

import io.manebot.chat.Community;
import io.manebot.platform.AbstractPlatformConnection;
import io.manebot.platform.Platform;
import io.manebot.platform.PlatformUser;

import io.manebot.plugin.Plugin;
import io.manebot.plugin.PluginException;
import io.manebot.plugin.slack.platform.chat.SlackChat;
import io.manebot.plugin.slack.platform.chat.SlackReceivedChatMessage;
import io.manebot.plugin.slack.platform.chat.SlackChatSender;
import io.manebot.plugin.slack.platform.user.SlackPlatformUser;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SlackPlatformConnection extends AbstractPlatformConnection {
    private final Platform platform;
    private final Plugin plugin;

    private SlackSession slackSession;
    private String userId;

    public SlackPlatformConnection(Platform platform, Plugin plugin) {
        this.platform = platform;
        this.plugin = plugin;
    }

    public Platform getPlatform() {
        return platform;
    }

    public SlackSession getSession() {
        return slackSession;
    }

    @Override
    protected SlackPlatformUser loadUserById(String id) {
        return loadUser(slackSession.findUserById(Objects.requireNonNull(id)));
    }

    private SlackPlatformUser loadUser(SlackUser user) {
        return new SlackPlatformUser(this, Objects.requireNonNull(user));
    }

    @Override
    protected Chat loadChatById(String id) {
        return loadChat(slackSession.findChannelByName(Objects.requireNonNull(id)));
    }

    @Override protected Community loadCommunityById(String id) {
        return null;
    }

    private Chat loadChat(SlackChannel channel) {
        return new SlackChat(this, Objects.requireNonNull(channel));
    }

    public SlackPlatformUser getPlatformUser(SlackUser user) {
        return (SlackPlatformUser) super.getCachedUserById(user.getId(), (key) -> loadUser(user));
    }

    public SlackChat getChat(SlackChannel channel) {
        return (SlackChat) super.getCachedChatById(channel.getId(), (key) -> loadChat(channel));
    }

    @Override
    public boolean isConnected() {
        return slackSession != null && slackSession.isConnected();
    }

    @Override
    public void connect() throws PluginException {
        String token = plugin.requireProperty("token");

        slackSession = SlackSessionFactory.createWebSocketSlackSession(token);

        slackSession.addMessagePostedListener((event, slackSession) -> {
            try {
                SlackUser author = event.getUser();

                if (author == null || author.isBot()) return;

                SlackPlatformUser user = (SlackPlatformUser) getPlatformUser(author.getId());

                SlackChat chat = getChat(event.getChannel());

                SlackChatSender chatSender = new SlackChatSender(user, chat);

                SlackReceivedChatMessage chatMessage = new SlackReceivedChatMessage(
                                SlackPlatformConnection.this,
                                chatSender,
                                event
                );

                plugin.getBot().getChatDispatcher().executeAsync(chatMessage);
            } catch (Throwable e) {
                plugin.getLogger().log(Level.WARNING, "Problem handling Discord message", e);
            }
        });

        slackSession.addSlackConnectedListener((event, slackSession) -> {
            userId = event.getSlackConnectedPersona().getId();

            plugin.getLogger().log(Level.INFO, "Connected to slack as " +
                    event.getSlackConnectedPersona().getUserName() + ".");
        });

        slackSession.addSlackDisconnectedListener((event, slackSession) -> {
            plugin.getLogger().log(Level.INFO, "Disconnected from Slack websocket.");
        });

        try {
            slackSession.connect();
        } catch (Throwable e) {
            throw new PluginException("Problem connecting to Slack", e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (isConnected()) slackSession.disconnect();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Problem disconnecting from Slack", e);
        }

        super.disconnect();
    }

    @Override
    public SlackPlatformUser getSelf() {
        return (SlackPlatformUser) getPlatformUser(userId);
    }

    @Override
    public Collection<PlatformUser> getPlatformUsers() {
        return slackSession.getUsers().stream().map(this::getPlatformUser).collect(Collectors.toList());
    }

    @Override
    public Collection<Chat> getChats() {
        return slackSession.getChannels().stream().map(this::getChat).collect(Collectors.toList());
    }

    @Override
    public Collection<String> getPlatformUserIds() {
        return slackSession.getUsers().stream().map(SlackUser::getId).collect(Collectors.toList());
    }

    @Override
    public Collection<String> getChatIds() {
        return slackSession.getChannels().stream().map(SlackChannel::getId).collect(Collectors.toList());
    }

    @Override public Collection<String> getCommunityIds() {
        return Collections.emptyList();
    }

    @Override public Collection<Community> getCommunities() {
        return getCommunityIds().stream().map(this::loadCommunityById).collect(Collectors.toList());
    }
}
