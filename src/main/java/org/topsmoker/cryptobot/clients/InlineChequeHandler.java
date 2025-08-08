package org.topsmoker.cryptobot.clients;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Setter;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.util.Arrays;


public class InlineChequeHandler implements Client.ResultHandler {
    private final int CHEQUE_URL_LENGTH = 35;
    private final int CHEQUE_ID_LENGTH = 12;
    protected final int CHEQUE_ID_OFFSET = CHEQUE_URL_LENGTH - CHEQUE_ID_LENGTH;
    private final byte[] CREATING_CALLBACK_DATA = "check-creating".getBytes();
    private final Cryptobot cryptobot;
    private final Cache<Long, TdApi.Message> chequeMessageLRUCache;
    @Setter
    private Client client;

    public InlineChequeHandler(Cryptobot cryptobot) {
        this.cryptobot = cryptobot;
        this.chequeMessageLRUCache = Caffeine.newBuilder()
                .maximumSize(30)
                .build();
    }

    private boolean isViaCryptobot(TdApi.Message message) {
        return message.viaBotUserId == Cryptobot.USER_ID;
    }

    private boolean isChequeCreatingButton(TdApi.InlineKeyboardButtonType inlineKeyboardButtonType) {
        return inlineKeyboardButtonType.getConstructor() == TdApi.InlineKeyboardButtonTypeCallback.CONSTRUCTOR &&
                Arrays.equals(((TdApi.InlineKeyboardButtonTypeCallback) inlineKeyboardButtonType).data, CREATING_CALLBACK_DATA);
    }

    private String getChequeIdIfReleased(TdApi.InlineKeyboardButtonType inlineKeyboardButtonType) {
        if (inlineKeyboardButtonType.getConstructor() == TdApi.InlineKeyboardButtonTypeUrl.CONSTRUCTOR) {
            String url = ((TdApi.InlineKeyboardButtonTypeUrl) inlineKeyboardButtonType).url;
            if (url.length() == CHEQUE_URL_LENGTH &&
                    url.charAt(CHEQUE_ID_OFFSET) == 'C') {
                return url.substring(CHEQUE_ID_OFFSET);
            }
        }
        return null;
    }

    @Override
    public void onResult(TdApi.Object update) {
        switch (update.getConstructor()) {
            case TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                TdApi.Message message = ((TdApi.UpdateNewMessage) update).message;
                if (isViaCryptobot(message) &&
                        message.replyMarkup != null) {
                    TdApi.InlineKeyboardButtonType inlineKeyboardButtonType = ((TdApi.ReplyMarkupInlineKeyboard) message.replyMarkup).rows[0][0].type;
                    String chequeId = getChequeIdIfReleased(inlineKeyboardButtonType);
                    if (chequeId != null) {
                        cryptobot.activate(chequeId);
                        System.out.println("Cheque ID: " + chequeId);
                    } else if (isChequeCreatingButton(inlineKeyboardButtonType)) {
                        chequeMessageLRUCache.put(message.id, message);
                    }
                }
            }
            case TdApi.UpdateMessageEdited.CONSTRUCTOR -> {
                TdApi.Message chequeMessage = chequeMessageLRUCache.getIfPresent(((TdApi.UpdateMessageEdited) update).messageId);

                if (chequeMessage != null) {
                    client.send(new TdApi.GetMessage(chequeMessage.chatId, chequeMessage.id), result -> cryptobot.activate(getChequeIdIfReleased(((TdApi.ReplyMarkupInlineKeyboard) ((TdApi.Message) result).replyMarkup).rows[0][0].type)));
                    chequeMessageLRUCache.invalidate(((TdApi.UpdateMessageEdited) update).messageId);
                }
            }
        }
    }
}

