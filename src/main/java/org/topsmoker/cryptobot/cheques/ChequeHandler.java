package org.topsmoker.cryptobot.cheques;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.topsmoker.cryptobot.cheques.Helper.*;


public class ChequeHandler implements Client.ResultHandler, AutoCloseable {
    private final PollingService pollingService;
    private final boolean usePolling;
    private final Activator activator;
    private final Pattern chequePattern;
    ExecutorService updatesExecutor;


    @Override
    public void close() throws Exception {
        updatesExecutor.close();
        if (usePolling) {
            pollingService.close();
        }
    }

    public ChequeHandler(Activator activator,
                         PollingService pollingService) {
        this.activator = activator;
        this.chequePattern = Pattern.compile("Q[A-Za-z0-9]{10}");
        if (pollingService != null) {
            this.pollingService = pollingService;
            this.usePolling = true;
        } else {
            this.pollingService = null;
            this.usePolling = false;
        }
        updatesExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }


    public boolean findChequeIdInMessage(TdApi.Message message) {
        if (message.content.getConstructor() == TdApi.MessageText.CONSTRUCTOR) {
            Matcher m = chequePattern.matcher(((TdApi.MessageText) message.content).text.text);
            if (m.find()) {
                activator.activate("C" + m.group()); // micro optimization
                return true;
            }
        }
        return false;
    }

    public boolean findCreatingOrForwardedCheque(TdApi.Message message) {
        if (message.replyMarkup != null &&
                isViaCryptobot(message)) {
            TdApi.ReplyMarkup replyMarkup = message.replyMarkup;
            if (replyMarkup.getConstructor() == TdApi.ReplyMarkupInlineKeyboard.CONSTRUCTOR) {
                TdApi.InlineKeyboardButton button = ((TdApi.ReplyMarkupInlineKeyboard) replyMarkup).rows[0][0];
                if (isChequeCreatingButton(button) && usePolling) {
                    pollingService.poll(message.chatId, message.id);
                    return true;
                } else {
                    String chequeId = extractChequeId(((TdApi.InlineKeyboardButtonTypeUrl) button.type).url);
                    if (chequeId != null) {
                        activator.activate(chequeId);
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public boolean findCreatedCheque(TdApi.ReplyMarkup replyMarkup) {
        if (replyMarkup != null && replyMarkup.getConstructor() == TdApi.ReplyMarkupInlineKeyboard.CONSTRUCTOR) {
            TdApi.InlineKeyboardButton button = ((TdApi.ReplyMarkupInlineKeyboard) replyMarkup).rows[0][0];
            if (button.type.getConstructor() == TdApi.InlineKeyboardButtonTypeUrl.CONSTRUCTOR) {
                String chequeId = extractChequeId(((TdApi.InlineKeyboardButtonTypeUrl) button.type).url);
                if (chequeId != null) {
                    activator.activate(chequeId);
                    return true;
                }
            }
        }
        return false;
    }

    public void processUpdate(TdApi.Object update) {
        switch (update.getConstructor()) {
            case TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                TdApi.UpdateNewMessage updateNewMessage = (TdApi.UpdateNewMessage) update;
                if (!findCreatingOrForwardedCheque(updateNewMessage.message)) {
                    findChequeIdInMessage(updateNewMessage.message);
                }
            }
            case TdApi.UpdateChatLastMessage.CONSTRUCTOR -> findCreatedCheque(((TdApi.UpdateChatLastMessage) update).lastMessage.replyMarkup);
            case TdApi.UpdateMessageEdited.CONSTRUCTOR -> findCreatedCheque(((TdApi.UpdateMessageEdited) update).replyMarkup);
        }
    }


    @Override
    public void onResult(TdApi.Object update) {
        updatesExecutor.execute(() -> processUpdate(update));
    }
}

