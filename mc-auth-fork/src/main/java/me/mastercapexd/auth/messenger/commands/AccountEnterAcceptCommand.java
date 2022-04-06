package me.mastercapexd.auth.messenger.commands;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Predicate;

import me.mastercapexd.auth.Auth;
import me.mastercapexd.auth.account.Account;
import me.mastercapexd.auth.config.PluginConfig;
import me.mastercapexd.auth.link.LinkCommandActorWrapper;
import me.mastercapexd.auth.link.LinkType;
import me.mastercapexd.auth.link.entryuser.LinkEntryUser;
import me.mastercapexd.auth.proxy.ProxyPlugin;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Dependency;
import revxrsal.commands.orphan.OrphanCommand;

public class AccountEnterAcceptCommand implements OrphanCommand {

	@Dependency
	private ProxyPlugin plugin;
	@Dependency
	private PluginConfig config;
	
	@Default
	public void onAccept(LinkCommandActorWrapper actorWrapper, LinkType linkType) {
		Predicate<LinkEntryUser> filter = entryUser -> {
			return entryUser.getLinkUserInfo().getLinkUserId().equals(actorWrapper.userId())
					&& entryUser.getLinkType().equals(linkType)
					&& Duration.of(System.currentTimeMillis() - entryUser.getConfirmationStartTime(), ChronoUnit.MILLIS)
							.getSeconds() <= config.getVKSettings().getEnterSettings().getEnterDelay();
		};

		List<LinkEntryUser> accounts = Auth.getLinkEntryAuth().getLinkUsers(filter);
		if (accounts.isEmpty()) {
//			sendMessage(e.getPeer(),
//					receptioner.getConfig().getVKSettings().getVKMessages().getMessage("enter-no-enter")); all link type message support
			return;
		}
		Auth.getLinkEntryAuth().removeLinkUsers((entryUser) -> {
			boolean isUserValid = filter.test(entryUser);
			if(isUserValid)
				entryUser.setConfirmed(true);
			return isUserValid;
		});

		Account account = accounts.stream().findFirst().orElse(null).getAccount();
		String stepName = config
				.getAuthenticationStepName(account.getCurrentConfigurationAuthenticationStepCreatorIndex());
		account.nextAuthenticationStep(
				plugin.getAuthenticationContextFactoryDealership().createContext(stepName, account));
	}
	
}
