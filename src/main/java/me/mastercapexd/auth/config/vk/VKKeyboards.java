package me.mastercapexd.auth.config.vk;

import com.google.gson.Gson;
import com.ubivashka.configuration.holders.ConfigurationSectionHolder;
import com.ubivashka.messenger.vk.message.keyboard.VkKeyboard;
import com.ubivaska.messenger.common.keyboard.Keyboard;
import me.mastercapexd.auth.config.ConfigurationHolder;
import me.mastercapexd.auth.config.messenger.MessengerKeyboards;
import me.mastercapexd.auth.utils.CollectionUtils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VKKeyboards implements ConfigurationHolder, MessengerKeyboards {
    private static final Gson GSON = new Gson();
    private final Map<String, String> jsonKeyboards;

    public VKKeyboards(ConfigurationSectionHolder sectionHolder) {
        jsonKeyboards = sectionHolder.getKeys().stream().collect(Collectors.toMap(Function.identity(), (key) -> sectionHolder.getString(key)));
    }

    @Override
    public Keyboard createKeyboard(String key, String... placeholders) {
        String rawJson = jsonKeyboards.get(key);
        for (Entry<String, String> entry : CollectionUtils.createStringMap(placeholders).entrySet())
            rawJson = rawJson.replaceAll(entry.getKey(), entry.getValue());
        return new VkKeyboard(GSON.fromJson(rawJson, com.vk.api.sdk.objects.messages.Keyboard.class));
    }
}
