package io.github._4drian3d.unsignedvelocity.listener.packet.chat;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChatPacket;
import io.github._4drian3d.unsignedvelocity.UnSignedVelocity;
import io.github._4drian3d.unsignedvelocity.configuration.Configuration;
import io.github._4drian3d.unsignedvelocity.listener.EventListener;
import io.github._4drian3d.vpacketevents.api.event.PacketReceiveEvent;

import java.util.concurrent.ExecutionException;

public final class KeyedChatListener implements EventListener {
    @Inject
    private EventManager eventManager;
    @Inject
    private UnSignedVelocity plugin;
    @Inject
    private Configuration configuration;

    @Override
    public void register() {
        eventManager.register(plugin, PacketReceiveEvent.class, this::onChat);
    }

    private void onChat(final PacketReceiveEvent event) {
        // Packet sent by players with version 1.19 and 1.19.1
        if (!(event.getPacket() instanceof final KeyedPlayerChatPacket chatPacket)) {
            return;
        }

        final ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
        if (!checkConnection(player)) return;

        event.setResult(ResultedEvent.GenericResult.denied());
        final String chatMessage = chatPacket.getMessage();

        player.getChatQueue().queuePacket(chatState ->
        {
            try {
                return eventManager.fire(new PlayerChatEvent(player, chatMessage))
                .thenApply(PlayerChatEvent::getResult)
                .thenApply(result -> {
                    if (!result.isAllowed()) {
                        return null;
                    }
                    final boolean isModified = result.getMessage()
                            .map(str -> !str.equals(chatMessage))
                            .orElse(false);

                    if (isModified) {
                        return player.getChatBuilderFactory()
                                .builder()
                                .message(result.getMessage().orElseThrow())
                                .setTimestamp(chatPacket.getExpiry())
                                .toServer();
                    }
                    return chatPacket;
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });



    }

    @Override
    public boolean canBeLoaded() {
        return configuration.applyChatMessages();
    }
}
