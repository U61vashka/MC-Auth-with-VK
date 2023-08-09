package me.mastercapexd.auth.velocity.api.bossbar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.bivashy.auth.api.server.bossbar.ServerBossbar;
import com.bivashy.auth.api.server.message.AdventureServerComponent;
import com.bivashy.auth.api.server.message.ServerComponent;
import com.bivashy.auth.api.server.player.ServerPlayer;

import me.mastercapexd.auth.velocity.player.VelocityServerPlayer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class VelocityServerBossbar extends ServerBossbar {
    private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.gson();
    private final List<ServerPlayer> bossBarPlayers = new ArrayList<>();
    private final BossBar bossBar;

    public VelocityServerBossbar(ServerComponent component) {
        title(component);
        BossBar.Color bossBarColor = BossBar.Color.values()[color.ordinal()];
        BossBar.Overlay bossBarOverlay = BossBar.Overlay.values()[segmentStyle.ordinal()];
        bossBar = BossBar.bossBar(GSON_COMPONENT_SERIALIZER.deserialize(component.jsonText()), progress, bossBarColor, bossBarOverlay).progress(progress);
    }

    @Override
    public ServerBossbar send(ServerPlayer... viewers) {
        for (ServerPlayer player : viewers) {
            player.as(VelocityServerPlayer.class).getPlayer().showBossBar(bossBar);
            bossBarPlayers.add(player);
        }
        return this;
    }

    @Override
    public ServerBossbar remove(ServerPlayer... viewers) {
        for (ServerPlayer player : viewers) {
            player.as(VelocityServerPlayer.class).getPlayer().hideBossBar(bossBar);
            bossBarPlayers.remove(player);
        }
        return this;
    }

    @Override
    public ServerBossbar update() {
        BossBar.Color bossBarColor = BossBar.Color.values()[color.ordinal()];
        BossBar.Overlay bossBarOverlay = BossBar.Overlay.values()[segmentStyle.ordinal()];

        if (title instanceof AdventureServerComponent) {
            bossBar.name(((AdventureServerComponent) title).component());
        } else {
            bossBar.name(GSON_COMPONENT_SERIALIZER.deserialize(title.jsonText()));
        }
        bossBar.color(bossBarColor).overlay(bossBarOverlay).progress(progress);
        return this;
    }

    @Override
    public ServerBossbar removeAll() {
        remove(bossBarPlayers.toArray(new ServerPlayer[0]));
        return this;
    }

    @Override
    public Collection<ServerPlayer> players() {
        return Collections.unmodifiableList(bossBarPlayers);
    }
}
