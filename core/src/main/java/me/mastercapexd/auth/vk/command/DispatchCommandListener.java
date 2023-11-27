package me.mastercapexd.auth.vk.command;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.bivashy.auth.api.AuthPlugin;
import com.bivashy.auth.api.config.link.command.LinkCustomCommandSettings;
import com.bivashy.auth.api.link.command.context.CustomCommandExecutionContext;
import com.bivashy.lamp.commands.vk.VkCommandHandler;
import com.bivashy.lamp.commands.vk.core.BaseVkActor;
import com.bivashy.lamp.commands.vk.core.VkHandler;
import com.bivashy.lamp.commands.vk.message.ButtonDispatchSource;
import com.bivashy.lamp.commands.vk.message.DispatchSource;
import com.bivashy.lamp.commands.vk.message.MessageDispatchSource;
import com.bivashy.lamp.commands.vk.objects.CallbackButton;
import com.bivashy.messenger.common.identificator.Identificator;
import com.bivashy.messenger.common.message.Message;
import com.bivashy.messenger.common.message.Message.MessageBuilder;
import com.bivashy.messenger.vk.message.keyboard.VkKeyboard;
import com.google.gson.Gson;
import com.ubivashka.vk.api.parsers.objects.CallbackButtonEvent;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.messages.Keyboard;

import me.mastercapexd.auth.hooks.VkPluginHook;
import me.mastercapexd.auth.link.vk.VKCommandActorWrapper;
import me.mastercapexd.auth.link.vk.VKLinkType;
import me.mastercapexd.auth.messenger.commands.custom.BaseCustomCommandExecutionContext;
import me.mastercapexd.auth.messenger.commands.parser.SimpleStringTokenizer;
import revxrsal.commands.command.ArgumentStack;
import revxrsal.commands.command.CommandActor;

public abstract class DispatchCommandListener {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    private static final VKLinkType LINK_TYPE = VKLinkType.getInstance();
    private static final VkPluginHook VK_HOOK = AuthPlugin.instance().getHook(VkPluginHook.class);
    private static final Gson GSON = new Gson();
    private static final double CONVERSATION_PEER_ID_OFFSET = 2e9;

    protected void onMessage(com.vk.api.sdk.objects.messages.Message vkMessage, int peerId) {
        EXECUTOR_SERVICE.execute(() -> VkHandler.getInstances().forEach((commandHandler) -> {
            handleCommandDispatch(commandHandler, new MessageDispatchSource(vkMessage));

            LINK_TYPE.getSettings().getCustomCommands().execute(new BaseCustomCommandExecutionContext(vkMessage.getText())).forEach(customCommand -> {
                Message message = createMessage(customCommand);
                message.send(Identificator.of(peerId));
            });
        }));
    }

    protected void onButtonClick(CallbackButtonEvent buttonEvent) {
        EXECUTOR_SERVICE.execute(() -> VkHandler.getInstances().forEach((commandHandler) -> {
            responseToButtonClick(buttonEvent);

            CallbackButton callbackButton = GSON.fromJson(GSON.toJson(buttonEvent), CallbackButton.class);
            handleCommandDispatch(commandHandler, new ButtonDispatchSource(callbackButton));
            CustomCommandExecutionContext executionContext = new BaseCustomCommandExecutionContext(buttonEvent.getPayload());
            executionContext.setButtonExecution(true);
            LINK_TYPE.getSettings()
                    .getCustomCommands()
                    .execute(executionContext)
                    .forEach(customCommand -> {
                        Message message = createMessage(customCommand);
                        message.send(Identificator.of(buttonEvent.getPeerID()));
                    });
        }));
    }

    private void responseToButtonClick(CallbackButtonEvent event) {
        try {
            VK_HOOK.getClient()
                    .messages()
                    .sendMessageEventAnswer(VK_HOOK.getActor(), event.getEventID(), event.getUserID(), event.getPeerID())
                    .execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleCommandDispatch(VkCommandHandler handler, DispatchSource source) {
        if (LINK_TYPE.getSettings().shouldDisableConversationCommands() && isConversationPeerId(source.getPeerId()))
            return;
        CommandActor commandActor = new VKCommandActorWrapper(new BaseVkActor(source, handler));
        ArgumentStack argumentStack = ArgumentStack.copyExact(SimpleStringTokenizer.parse(source.getText()));
        if (argumentStack.isEmpty())
            return;
        handler.dispatch(commandActor, argumentStack);
    }

    private Message createMessage(LinkCustomCommandSettings customCommand) {
        MessageBuilder builder = LINK_TYPE.newMessageBuilder(customCommand.getAnswer());
        if (customCommand.getSectionHolder().contains("keyboard"))
            builder.keyboard(new VkKeyboard(GSON.fromJson(customCommand.getSectionHolder().getString("keyboard"), Keyboard.class)));
        return builder.build();
    }

    private boolean isConversationPeerId(int peerId) {
        return peerId > CONVERSATION_PEER_ID_OFFSET;
    }

}
