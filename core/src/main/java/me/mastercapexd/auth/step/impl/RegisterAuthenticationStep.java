package me.mastercapexd.auth.step.impl;

import com.bivashy.auth.api.AuthPlugin;
import com.bivashy.auth.api.account.Account;
import com.bivashy.auth.api.config.PluginConfig;
import com.bivashy.auth.api.server.player.ServerPlayer;
import com.bivashy.auth.api.step.AuthenticationStep;
import com.bivashy.auth.api.step.AuthenticationStepContext;
import com.bivashy.auth.api.step.MessageableAuthenticationStep;

import me.mastercapexd.auth.config.message.server.ServerMessageContext;
import me.mastercapexd.auth.step.AuthenticationStepTemplate;
import me.mastercapexd.auth.step.creators.AuthenticationStepFactoryTemplate;

public class RegisterAuthenticationStep extends AuthenticationStepTemplate implements MessageableAuthenticationStep {
    public static final String STEP_NAME = "REGISTER";
    private static final AuthPlugin PLUGIN = AuthPlugin.instance();
    private final boolean isRegistered;

    public RegisterAuthenticationStep(AuthenticationStepContext context) {
        super(STEP_NAME, context);
        isRegistered = context.getAccount().isRegistered();
    }

    @Override
    public boolean shouldPassToNextStep() {
        boolean isCurrentAccountRegistered = authenticationStepContext.getAccount().isRegistered();
        if (!isRegistered && isCurrentAccountRegistered) {
            Account account = authenticationStepContext.getAccount();
            account.getPlayer().ifPresent(player -> {
                PLUGIN.getAuthenticatingAccountBucket().removeAuthenticatingAccount(account);
                account.setLastIpAddress(player.getPlayerIp());
                account.setLastSessionStartTimestamp(System.currentTimeMillis());
            });
        }
        return isCurrentAccountRegistered;
    }

    @Override
    public boolean shouldSkip() {
        return authenticationStepContext.getAccount().isRegistered();
    }

    @Override
    public void process(ServerPlayer player) {
        Account account = authenticationStepContext.getAccount();
        PluginConfig config = AuthPlugin.instance().getConfig();
        player.sendMessage(config.getServerMessages().getMessage("register-chat", new ServerMessageContext(account)));
        AuthPlugin.instance()
                .getCore()
                .createTitle(config.getServerMessages().getMessage("register-title"))
                .subtitle(config.getServerMessages().getMessage("register-subtitle"))
                .stay(120)
                .send(player);
    }

    public static class RegisterAuthenticationStepFactory extends AuthenticationStepFactoryTemplate {
        public RegisterAuthenticationStepFactory() {
            super(STEP_NAME);
        }

        @Override
        public AuthenticationStep createNewAuthenticationStep(AuthenticationStepContext context) {
            return new RegisterAuthenticationStep(context);
        }
    }
}
