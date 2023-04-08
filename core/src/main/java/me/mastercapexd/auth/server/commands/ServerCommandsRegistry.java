package me.mastercapexd.auth.server.commands;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;

import com.bivashy.auth.api.AuthPlugin;
import com.bivashy.auth.api.bucket.LinkConfirmationBucket;
import com.bivashy.auth.api.config.PluginConfig;
import com.bivashy.auth.api.config.message.server.ServerMessages;
import com.bivashy.auth.api.database.AccountDatabase;
import com.bivashy.auth.api.link.LinkType;
import com.bivashy.auth.api.link.user.LinkUser;
import com.bivashy.auth.api.link.user.confirmation.LinkConfirmationUser;
import com.bivashy.auth.api.link.user.info.LinkUserIdentificator;
import com.bivashy.auth.api.link.user.info.impl.UserNumberIdentificator;
import com.bivashy.auth.api.type.LinkConfirmationType;

import me.mastercapexd.auth.link.telegram.TelegramLinkType;
import me.mastercapexd.auth.link.vk.VKLinkType;
import me.mastercapexd.auth.messenger.commands.annotation.CommandKey;
import me.mastercapexd.auth.server.commands.annotations.GoogleUse;
import me.mastercapexd.auth.server.commands.annotations.TelegramUse;
import me.mastercapexd.auth.server.commands.annotations.VkUse;
import me.mastercapexd.auth.server.commands.exception.SendComponentException;
import me.mastercapexd.auth.server.commands.parameters.DoublePassword;
import me.mastercapexd.auth.server.commands.parameters.NewPassword;
import me.mastercapexd.auth.server.commands.parameters.RegisterPassword;
import me.mastercapexd.auth.shared.commands.LinkCodeCommand;
import me.mastercapexd.auth.shared.commands.MessengerLinkCommandTemplate;
import me.mastercapexd.auth.shared.commands.TelegramLinkCommand;
import me.mastercapexd.auth.shared.commands.VKLinkCommand;
import me.mastercapexd.auth.shared.commands.annotation.DefaultForOrphan;
import me.mastercapexd.auth.shared.commands.annotation.LinkCommand;
import me.mastercapexd.auth.shared.commands.parameter.MessengerLinkContext;
import revxrsal.commands.CommandHandler;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.dynamic.Annotations;
import revxrsal.commands.command.ArgumentStack;
import revxrsal.commands.orphan.OrphanCommand;
import revxrsal.commands.orphan.Orphans;

public abstract class ServerCommandsRegistry {
    protected AuthPlugin plugin = AuthPlugin.instance();
    protected CommandHandler commandHandler;

    public ServerCommandsRegistry(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;

        registerCommandContexts();
        registerDependencies();
    }

    private void registerCommandContexts() {
        PluginConfig config = plugin.getConfig();

        commandHandler.registerValueResolver(LinkUserIdentificator.class, context -> null);
        commandHandler.registerValueResolver(UserNumberIdentificator.class, context -> new UserNumberIdentificator(context.popInt()));

        commandHandler.registerValueResolver(MessengerLinkContext.class, context -> {
            String code = context.popForParameter();

            Optional<LinkConfirmationUser> confirmationUserOptional = plugin.getLinkConfirmationBucket()
                    .findFirst(user -> user.getConfirmationCode().equals(code));

            if (!confirmationUserOptional.isPresent())
                throw new SendComponentException(config.getServerMessages().getSubMessages("link-code").getMessage("no-code"));

            LinkConfirmationUser confirmationUser = confirmationUserOptional.get();

            if (System.currentTimeMillis() > confirmationUser.getLinkTimeoutTimestamp())
                throw new SendComponentException(config.getServerMessages().getSubMessages("link-code").getMessage("timed-out"));

            LinkUser linkUser = confirmationUser.getLinkTarget()
                    .findFirstLinkUserOrNew(user -> user.getLinkType().equals(confirmationUser.getLinkType()), confirmationUser.getLinkType());

            if (!linkUser.isIdentifierDefaultOrNull())
                throw new SendComponentException(config.getServerMessages().getSubMessages("link-code").getMessage("already-linked"));

            return new MessengerLinkContext(code, confirmationUser);
        });

        commandHandler.registerValueResolver(DoublePassword.class, context -> {
            ArgumentStack arguments = context.arguments();
            String oldPassword = arguments.pop();
            if (arguments.isEmpty())
                throw new SendComponentException(config.getServerMessages().getMessage("enter-new-password"));
            String newPassword = arguments.pop();
            DoublePassword password = new DoublePassword(oldPassword, newPassword);
            if (oldPassword.equals(newPassword))
                throw new SendComponentException(config.getServerMessages().getMessage("nothing-to-change"));

            if (newPassword.length() < config.getPasswordMinLength())
                throw new SendComponentException(config.getServerMessages().getMessage("password-too-short"));

            if (newPassword.length() > config.getPasswordMaxLength())
                throw new SendComponentException(config.getServerMessages().getMessage("password-too-long"));
            return password;
        });

        commandHandler.registerValueResolver(NewPassword.class, context -> {
            String newRawPassword = context.pop();
            if (newRawPassword.length() < config.getPasswordMinLength())
                throw new SendComponentException(config.getServerMessages().getMessage("password-too-short"));

            if (newRawPassword.length() > config.getPasswordMaxLength())
                throw new SendComponentException(config.getServerMessages().getMessage("password-too-long"));
            return new NewPassword(newRawPassword);
        });

        commandHandler.registerValueResolver(RegisterPassword.class, context -> {
            ArgumentStack arguments = context.arguments();
            String registerPassword = arguments.pop();

            if (config.isPasswordConfirmationEnabled()) {
                if (arguments.isEmpty())
                    throw new SendComponentException(config.getServerMessages().getMessage("confirm-password"));
                String confirmationPassword = arguments.pop();
                if (!confirmationPassword.equals(registerPassword))
                    throw new SendComponentException(config.getServerMessages().getMessage("confirm-failed"));
            }

            if (registerPassword.length() < config.getPasswordMinLength())
                throw new SendComponentException(config.getServerMessages().getMessage("password-too-short"));

            if (registerPassword.length() > config.getPasswordMaxLength())
                throw new SendComponentException(config.getServerMessages().getMessage("password-too-long"));
            return new RegisterPassword(registerPassword);
        });

        commandHandler.registerCondition((actor, command, arguments) -> {
            if (!command.hasAnnotation(GoogleUse.class))
                return;
            if (!config.getGoogleAuthenticatorSettings().isEnabled())
                throw new SendComponentException(config.getServerMessages().getSubMessages("google").getMessage("disabled"));
        });

        commandHandler.registerCondition((actor, command, arguments) -> {
            if (!command.hasAnnotation(VkUse.class))
                return;
            if (!config.getVKSettings().isEnabled())
                throw new SendComponentException(config.getServerMessages().getSubMessages("vk").getMessage("disabled"));
        });

        commandHandler.registerCondition((actor, command, arguments) -> {
            if (!command.hasAnnotation(TelegramUse.class))
                return;
            if (!config.getTelegramSettings().isEnabled())
                throw new SendComponentException(config.getServerMessages().getSubMessages("telegram").getMessage("disabled"));
        });

        commandHandler.registerAnnotationReplacer(DefaultForOrphan.class, (annotatedElement, annotation) -> {
            if (!(annotatedElement instanceof Method))
                return Collections.emptyList();
            Method annotatedMethod = (Method) annotatedElement;
            Class<?> declaringClass = annotatedMethod.getDeclaringClass();
            if (!declaringClass.isAnnotationPresent(CommandKey.class))
                return Collections.emptyList();
            if (!declaringClass.isAnnotationPresent(LinkCommand.class))
                return Collections.emptyList();
            LinkType linkType = plugin.getLinkTypeProvider()
                    .getLinkType(declaringClass.getAnnotation(LinkCommand.class).value())
                    .orElseThrow(NullPointerException::new);
            CommandKey commandKey = declaringClass.getAnnotation(CommandKey.class);
            DefaultFor defaultForAnnotation = Annotations.create(DefaultFor.class, "value",
                    linkType.getSettings().getProxyCommandPaths().getCommandPath(commandKey.value()).getCommandPaths());
            return Collections.singletonList(defaultForAnnotation);
        });
    }

    private void registerDependencies() {
        commandHandler.registerDependency(LinkConfirmationBucket.class, plugin.getLinkConfirmationBucket());
        commandHandler.registerDependency(PluginConfig.class, plugin.getConfig());
        commandHandler.registerDependency(ServerMessages.class, plugin.getConfig().getServerMessages());
        commandHandler.registerDependency(AccountDatabase.class, plugin.getAccountDatabase());
        commandHandler.registerDependency(AuthPlugin.class, plugin);
    }

    protected void registerCommands() {
        commandHandler.register(new AuthCommand(), new LoginCommand(), new RegisterCommand(), new ChangePasswordCommand(), new GoogleCodeCommand(),
                new GoogleCommand(), new GoogleUnlinkCommand(), new LogoutCommand());

        registerLinkCommand(VKLinkType.getInstance(), new VKLinkCommand(LinkConfirmationType.FROM_LINK, VKLinkType.getInstance().getServerMessages()),
                new VkLinkCodeCommand(LinkConfirmationType.FROM_GAME));
        registerLinkCommand(TelegramLinkType.getInstance(),
                new TelegramLinkCommand(LinkConfirmationType.FROM_LINK, TelegramLinkType.getInstance().getServerMessages()),
                new TelegramLinkCodeCommand(LinkConfirmationType.FROM_GAME));
    }

    private void registerLinkCommand(LinkType linkType, OrphanCommand linkCommand, OrphanCommand codeCommand) {
        if (!linkType.getSettings().isEnabled())
            return;
        linkType.getSettings().getLinkConfirmationTypes().forEach(confirmationType -> {
            if (confirmationType == LinkConfirmationType.FROM_LINK)
                commandHandler.register(Orphans.path(makeServerCommandPaths(linkType, MessengerLinkCommandTemplate.CONFIGURATION_KEY)).handler(linkCommand));
            if (confirmationType == LinkConfirmationType.FROM_GAME)
                commandHandler.register(Orphans.path(makeServerCommandPaths(linkType, LinkCodeCommand.CONFIGURATION_KEY)).handler(codeCommand));
        });
    }

    private String[] makeServerCommandPaths(LinkType linkType, String commandPathKey) {
        return linkType.getSettings().getProxyCommandPaths().getCommandPath(commandPathKey).getCommandPaths();
    }
}
