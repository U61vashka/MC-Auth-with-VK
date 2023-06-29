package me.mastercapexd.auth.velocity.commands;

import java.util.Collections;
import java.util.Optional;

import com.bivashy.auth.api.AuthPlugin;
import com.bivashy.auth.api.account.Account;
import com.bivashy.auth.api.config.PluginConfig;
import com.bivashy.auth.api.model.PlayerIdSupplier;
import com.bivashy.auth.api.server.command.ServerCommandActor;
import com.bivashy.auth.api.server.player.ServerPlayer;
import com.bivashy.auth.api.shared.commands.MessageableCommandActor;
import com.velocitypowered.api.proxy.Player;

import me.mastercapexd.auth.server.commands.ServerCommandsRegistry;
import me.mastercapexd.auth.server.commands.annotations.AuthenticationAccount;
import me.mastercapexd.auth.server.commands.annotations.AuthenticationStepCommand;
import me.mastercapexd.auth.server.commands.annotations.Permission;
import me.mastercapexd.auth.server.commands.exception.SendComponentException;
import me.mastercapexd.auth.server.commands.parameters.ArgumentServerPlayer;
import me.mastercapexd.auth.velocity.VelocityAuthPluginBootstrap;
import me.mastercapexd.auth.velocity.commands.exception.VelocityExceptionHandler;
import me.mastercapexd.auth.velocity.player.VelocityServerPlayer;
import revxrsal.commands.annotation.dynamic.Annotations;
import revxrsal.commands.process.ContextResolver.ContextResolverContext;
import revxrsal.commands.velocity.VelocityCommandActor;
import revxrsal.commands.velocity.annotation.CommandPermission;
import revxrsal.commands.velocity.core.VelocityHandler;

public class VelocityCommandRegistry extends ServerCommandsRegistry {
    private final PluginConfig config;

    public VelocityCommandRegistry(VelocityAuthPluginBootstrap pluginBootstrap, AuthPlugin authPlugin) {
        super(new VelocityHandler(pluginBootstrap, pluginBootstrap.getProxyServer()).setExceptionHandler(
                new VelocityExceptionHandler(authPlugin.getConfig().getServerMessages())).disableStackTraceSanitizing());
        this.config = authPlugin.getConfig();
        commandHandler.registerContextResolver(ServerPlayer.class, this::resolveServerPlayer);
        commandHandler.registerContextResolver(PlayerIdSupplier.class, context -> PlayerIdSupplier.of(resolveServerPlayer(context).getNickname()));
        commandHandler.registerContextResolver(MessageableCommandActor.class, this::resolveServerCommandActor);
        commandHandler.registerContextResolver(ServerCommandActor.class, this::resolveServerCommandActor);
        commandHandler.registerValueResolver(ArgumentServerPlayer.class, (context) -> {
            String value = context.pop();
            Optional<ServerPlayer> player = authPlugin.getCore().getPlayer(value);
            if (!player.isPresent())
                throw new SendComponentException(config.getServerMessages().getMessage("player-offline"));
            return new ArgumentServerPlayer(player.get());
        });
        commandHandler.registerCondition((actor, command, arguments) -> {
            if (!actor.as(VelocityCommandActor.class).isPlayer())
                return;
            ServerPlayer player = new VelocityServerPlayer(actor.as(VelocityCommandActor.class).getAsPlayer());
            String accountId = config.getActiveIdentifierType().getId(player);
            if (!authPlugin.getAuthenticatingAccountBucket().isAuthenticating(player))
                return;
            if (!command.hasAnnotation(AuthenticationStepCommand.class))
                return;
            Account account = authPlugin.getAuthenticatingAccountBucket().getAuthenticatingAccountNullable(player);
            if (account.getCurrentAuthenticationStep() == null)
                return;
            String stepName = command.getAnnotation(AuthenticationStepCommand.class).stepName();
            if (account.getCurrentAuthenticationStep().getStepName().equals(stepName))
                return;
            throw new SendComponentException(
                    config.getServerMessages().getSubMessages("authentication-step-usage").getMessage(account.getCurrentAuthenticationStep().getStepName()));
        });
        commandHandler.registerContextResolver(Account.class, (context) -> {
            ServerPlayer player = new VelocityServerPlayer(context.actor().as(VelocityCommandActor.class).getAsPlayer());
            if (player.getRealPlayer() == null)
                throw new SendComponentException(config.getServerMessages().getMessage("players-only"));
            String id = config.getActiveIdentifierType().getId(player);
            if (!authPlugin.getAuthenticatingAccountBucket().isAuthenticating(player))
                throw new SendComponentException(config.getServerMessages().getMessage("already-logged-in"));

            Account account = authPlugin.getAuthenticatingAccountBucket().getAuthenticatingAccountNullable(player);
            if (!account.isRegistered() && !context.parameter().hasAnnotation(AuthenticationAccount.class))
                throw new SendComponentException(config.getServerMessages().getMessage("account-not-found"));
            if (account.isRegistered() && context.parameter().hasAnnotation(AuthenticationAccount.class))
                throw new SendComponentException(config.getServerMessages().getMessage("account-exists"));
            return account;
        });

        commandHandler.registerAnnotationReplacer(Permission.class, (element, annotation) -> {
            CommandPermission commandPermissionAnnotation = Annotations.create(CommandPermission.class, "value", annotation.value());
            return Collections.singletonList(commandPermissionAnnotation);
        });

        commandHandler.registerExceptionHandler(SendComponentException.class,
                (actor, componentException) -> new VelocityServerCommandActor(actor.as(VelocityCommandActor.class)).reply(componentException.getComponent()));
        registerCommands();
    }

    private VelocityServerCommandActor resolveServerCommandActor(ContextResolverContext context) {
        return new VelocityServerCommandActor(context.actor().as(VelocityCommandActor.class));
    }

    private VelocityServerPlayer resolveServerPlayer(ContextResolverContext context) {
        Player player = context.actor().as(VelocityCommandActor.class).getAsPlayer();
        if (player == null)
            throw new SendComponentException(config.getServerMessages().getMessage("players-only"));
        return new VelocityServerPlayer(player);
    }
}
