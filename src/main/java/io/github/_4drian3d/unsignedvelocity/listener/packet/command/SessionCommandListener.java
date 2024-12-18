package io.github._4drian3d.unsignedvelocity.listener.packet.command;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.chat.CommandHandler;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerCommandPacket;
import io.github._4drian3d.unsignedvelocity.UnSignedVelocity;
import io.github._4drian3d.unsignedvelocity.configuration.Configuration;
import io.github._4drian3d.unsignedvelocity.listener.EventListener;
import io.github._4drian3d.vpacketevents.api.event.PacketReceiveEvent;

import java.util.concurrent.CompletableFuture;

public final class SessionCommandListener implements EventListener, CommandHandler<SessionPlayerCommandPacket> {
    @Inject
    private Configuration configuration;
    @Inject
    private EventManager eventManager;
    @Inject
    private UnSignedVelocity plugin;
    private final VelocityServer proxyServer;

    @Inject
    public SessionCommandListener(ProxyServer proxyServer) {
        this.proxyServer = (VelocityServer) proxyServer;
    }

    @Override
    public void register() {
        eventManager.register(plugin, PacketReceiveEvent.class, this::onCommand);
    }

    @Override
    public boolean canBeLoaded() {
        return configuration.removeSignedCommandInformation();
    }

    public void onCommand(final PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof final SessionPlayerCommandPacket packet)) {
            return;
        }

        final ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
        if (!checkConnection(player)) return;

        event.setResult(ResultedEvent.GenericResult.denied());
        final String commandExecuted = packet.getCommand();

        queueCommandResult(proxyServer, player, (commandEvent, lastSeenMessages) -> {
            final CommandExecuteEvent.CommandResult result = commandEvent.getResult();
            if (result == CommandExecuteEvent.CommandResult.denied()) {
                return CompletableFuture.completedFuture(null);
            }

            final String commandToRun = result.getCommand().orElse(commandExecuted);
            if (result.isForwardToServer()) {
                if (commandToRun.equals(commandExecuted)) {
                    return CompletableFuture.completedFuture(packet);
                } else {
                    return CompletableFuture.completedFuture(player.getChatBuilderFactory()
                            .builder()
                            .setTimestamp(packet.getTimeStamp())
                            .asPlayer(player)
                            .message("/" + commandToRun)
                            .toServer());
                }
            }

            return runCommand(proxyServer, player, commandToRun, hasRun -> {
                if (hasRun) return null;

                if (commandToRun.equals(commandExecuted)) {
                    return packet;
                } else {
                    return player.getChatBuilderFactory()
                            .builder()
                            .setTimestamp(packet.getTimeStamp())
                            .asPlayer(player)
                            .message("/" + commandToRun)
                            .toServer();
                }
            });
        }, commandExecuted, packet.getTimeStamp(), null);
    }

    @Override
    public Class<SessionPlayerCommandPacket> packetClass() {
        return SessionPlayerCommandPacket.class;
    }

    @Override
    public void handlePlayerCommandInternal(SessionPlayerCommandPacket sessionPlayerCommand) {
        // noop
    }
}
