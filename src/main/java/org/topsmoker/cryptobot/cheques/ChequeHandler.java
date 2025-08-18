package org.topsmoker.cryptobot.cheques;

import org.drinkless.tdlib.TdApi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.topsmoker.cryptobot.cheques.Helper.*;


public class ChequeHandler implements org.drinkless.tdlib.Client.ResultHandler, AutoCloseable {
    private final PollingService pollingService;
    private final boolean usePolling;
    private final Pattern chequeIdPattern;
    private final Activator activator;

    @Override
    public void close() throws Exception {
        if (usePolling) {
            pollingService.close();
        }
    }

    public ChequeHandler(Activator activator,
                         PollingService pollingService) {
        this.activator = activator;
        if (pollingService != null) {
            this.pollingService = pollingService;
            this.usePolling = true;
        } else {
            this.pollingService = null;
            this.usePolling = false;
        }
        this.chequeIdPattern = Pattern.compile("CQ[A-Za-z0-9]{10}");
    }


    public boolean findChequeIdInMessage(TdApi.UpdateNewMessage updateNewMessage) {
        if (updateNewMessage.message.content.getConstructor() == TdApi.MessageText.CONSTRUCTOR) {
            Matcher m = chequeIdPattern.matcher(((TdApi.MessageText) updateNewMessage.message.content).text.text);
            if (m.find()) {
                activator.activate(m.toMatchResult().group());
                return true;
            }
        }
        return false;
    }

    public boolean findCreatingOrForwardedCheque(TdApi.UpdateNewMessage updateNewMessage) {
        TdApi.Message message = updateNewMessage.message;
        if (isViaCryptobot(message) &&
                message.replyMarkup != null) {
            TdApi.InlineKeyboardButton button = ((TdApi.ReplyMarkupInlineKeyboard) message.replyMarkup).rows[0][0];
            if (usePolling && isChequeCreatingButton(button)) {
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
        return false;
    }

    public boolean findCreatedCheque(TdApi.UpdateMessageEdited updateMessageEdited) {
        TdApi.ReplyMarkup replyMarkup = updateMessageEdited.replyMarkup;
        if (replyMarkup != null && replyMarkup.getConstructor() == TdApi.ReplyMarkupInlineKeyboard.CONSTRUCTOR) {
            String chequeId = extractChequeId(((TdApi.InlineKeyboardButtonTypeUrl) ((TdApi.ReplyMarkupInlineKeyboard) replyMarkup).
                    rows[0][0].type)
                    .url);
            if (chequeId != null) {
                activator.activate(chequeId);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResult(TdApi.Object update) {
        switch (update.getConstructor()) {
            case TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                TdApi.UpdateNewMessage updateNewMessage = (TdApi.UpdateNewMessage) update;
                if (!findCreatingOrForwardedCheque(updateNewMessage)) {
                    findChequeIdInMessage(updateNewMessage);
                }
            }
            case TdApi.UpdateMessageEdited.CONSTRUCTOR -> findCreatedCheque((TdApi.UpdateMessageEdited) update);
        }
    }

}

