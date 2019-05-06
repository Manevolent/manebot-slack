package io.manebot.plugin.slack.platform.chat;

import com.ullink.slack.simpleslackapi.SlackChannel;
import io.manebot.chat.Chat;
import io.manebot.chat.ChatMessage;
import io.manebot.chat.TextFormat;
import io.manebot.platform.Platform;

import io.manebot.platform.PlatformUser;
import io.manebot.plugin.slack.platform.SlackPlatformConnection;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SlackChat implements Chat {
    private final SlackPlatformConnection connection;
    private final SlackChannel slackChannel;

    public SlackChat(SlackPlatformConnection connection, SlackChannel slackChannel) {
        this.connection = connection;
        this.slackChannel = slackChannel;
    }

    @Override
    public Platform getPlatform() {
        return connection.getPlatform();
    }

    @Override
    public String getId() {
        return slackChannel.getId();
    }

    @Override
    public void setName(String name) throws UnsupportedOperationException {
        slackChannel.setId(name);
    }

    @Override
    public boolean isConnected() {
        return connection.getPlatform().isConnected();
    }

    @Override
    public void removeMember(String platformId) {
        connection.getApi().kickUserFromChannel(slackChannel.getId(), platformId);
    }

    @Override
    public void addMember(String platformId) {
        connection.getApi().inviteUserToChannel(slackChannel.getId(), platformId);
    }

    @Override
    public Collection<ChatMessage> getLastMessages(int max) {
        return null;
    }

    @Override
    public Collection<PlatformUser> getPlatformUsers() {
        return slackChannel.getMembers().stream().map(connection::getPlatformUser).collect(Collectors.toList());
    }

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public boolean canChangeTypingStatus() {
        return false;
    }

    @Override
    public void setTyping(boolean typing) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTyping() {
        return false;
    }

    public void setTopic(String topic) {
        connection.getApi().setChannelTopic(getId(), topic);
    }

    public String getTopic() {
        Topic topic = slackChannel.getTopic();
        return topic == null ? null : topic.getValue();
    }

    @Override
    public TextFormat getFormat() {
        return null;
    }

    @Override
    public Collection<ChatMessage> sendMessage(Consumer<ChatMessage.Builder> function) {
        return null;
    }

    @Override
    public boolean canSendEmbeds() {
        return true;
    }

    @Override
    public boolean canSendEmoji() {
        return true;
    }
}