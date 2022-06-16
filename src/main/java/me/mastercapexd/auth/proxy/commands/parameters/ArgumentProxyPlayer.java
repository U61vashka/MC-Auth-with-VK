package me.mastercapexd.auth.proxy.commands.parameters;

import me.mastercapexd.auth.proxy.player.ProxyPlayer;

import java.net.InetSocketAddress;
import java.util.UUID;

public class ArgumentProxyPlayer implements ProxyPlayer {
    private final ProxyPlayer player;

    public ArgumentProxyPlayer(ProxyPlayer player) {
        this.player = player;
    }

    @Override
    public void disconnect(String reason) {
        player.disconnect(reason);
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(message);
    }

    @Override
    public String getNickname() {
        return player.getNickname();
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return player.getRemoteAddress();
    }

    @Override
    public <T> T getRealPlayer() {
        return player.getRealPlayer();
    }

}
