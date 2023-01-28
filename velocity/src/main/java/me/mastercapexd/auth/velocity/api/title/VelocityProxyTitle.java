package me.mastercapexd.auth.velocity.api.title;

import java.time.Duration;

import me.mastercapexd.auth.proxy.api.title.ProxyTitle;
import me.mastercapexd.auth.proxy.message.ProxyComponent;
import me.mastercapexd.auth.proxy.player.ProxyPlayer;
import me.mastercapexd.auth.velocity.component.VelocityComponent;
import me.mastercapexd.auth.velocity.player.VelocityProxyPlayer;
import net.kyori.adventure.title.Title;

public class VelocityProxyTitle extends ProxyTitle {
    private static final int MILLIS_PER_TICK = 1000 / 20;

    public VelocityProxyTitle(ProxyComponent title){
        title(title);
    }

    public VelocityProxyTitle() {
    }

    @Override
    public ProxyTitle fadeIn(int ticks) {
        return super.fadeIn(ticks * MILLIS_PER_TICK);
    }

    @Override
    public ProxyTitle stay(int ticks) {
        return super.stay(ticks * MILLIS_PER_TICK);
    }

    @Override
    public ProxyTitle fadeOut(int ticks) {
        return super.fadeOut(ticks * MILLIS_PER_TICK);
    }

    @Override
    public ProxyTitle send(ProxyPlayer... players) {
        Title createdTitle = Title.title(title.as(VelocityComponent.class).component(),
                subtitle.as(VelocityComponent.class).component(),
                Title.Times.of(Duration.ofMillis(fadeIn), Duration.ofMillis(stay), Duration.ofMillis(fadeOut)));
        for (ProxyPlayer player : players)
            player.as(VelocityProxyPlayer.class).getPlayer().showTitle(createdTitle);
        return this;
    }
}
