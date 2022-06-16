package me.mastercapexd.auth.config.vk;

import com.ubivashka.configuration.annotations.ConfigField;
import com.ubivashka.configuration.holders.ConfigurationSectionHolder;
import me.mastercapexd.auth.config.ConfigurationHolder;
import me.mastercapexd.auth.config.message.vk.VKMessages;
import me.mastercapexd.auth.config.messenger.*;
import me.mastercapexd.auth.link.user.info.identificator.LinkUserIdentificator;
import me.mastercapexd.auth.link.user.info.identificator.UserNumberIdentificator;
import me.mastercapexd.auth.proxy.ProxyPlugin;

import java.util.List;

public class VKSettings implements ConfigurationHolder, MessengerSettings {
    @ConfigField
    private boolean enabled = false;
    @ConfigField("confirmation")
    private DefaultConfirmationSettings confirmationSettings;
    @ConfigField("restore")
    private DefaultRestoreSettings restoreSettings;
    @ConfigField("enter")
    private DefaultEnterSettings enterSettings;
    @ConfigField("vk-commands")
    private DefaultCommandPaths commandPaths;
    @ConfigField("custom-commands")
    private DefaultMessengerCustomCommands commands;
    @ConfigField("max-vk-link")
    private Integer maxVkLinkCount = 0;
    @ConfigField("vk-messages")
    private VKMessages messages;
    @ConfigField("keyboards")
    private VKKeyboards keyboards;
    @ConfigField("admin-accounts")
    private List<Integer> adminAccounts;

    public VKSettings() {
    }

    public VKSettings(ConfigurationSectionHolder sectionHolder) {
        ProxyPlugin.instance().getConfigurationProcessor().resolve(sectionHolder, this);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public DefaultConfirmationSettings getConfirmationSettings() {
        return confirmationSettings;
    }

    @Override
    public MessengerCustomCommands getCustomCommands() {
        return commands;
    }

    @Override
    public DefaultEnterSettings getEnterSettings() {
        return enterSettings;
    }

    @Override
    public boolean isAdministrator(LinkUserIdentificator identificator) {
        if (identificator == null || !identificator.isNumber())
            return false;
        return adminAccounts.contains((int) identificator.asNumber());
    }

    public boolean isAdministrator(int userId) {
        return isAdministrator(new UserNumberIdentificator(userId));
    }

    @Override
    public DefaultRestoreSettings getRestoreSettings() {
        return restoreSettings;
    }

    @Override
    public DefaultCommandPaths getCommandPaths() {
        return commandPaths;
    }

    @Override
    public int getMaxLinkCount() {
        return maxVkLinkCount;
    }

    @Override
    public VKMessages getMessages() {
        return messages;
    }

    @Override
    public VKKeyboards getKeyboards() {
        return keyboards;
    }
}
