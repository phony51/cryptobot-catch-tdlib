package org.topsmoker.cryptobot.clients;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class InlineChequeHandler implements Client.ResultHandler {
    private final int CHEQUE_URL_LENGTH = 35;
    private final int CHEQUE_ID_LENGTH = 12;
    protected final int CHEQUE_ID_OFFSET = CHEQUE_URL_LENGTH - CHEQUE_ID_LENGTH;
//    private final byte[] CREATING_CALLBACK_DATA = "check-creating".getBytes();
    private final Cryptobot cryptobot;
//    @Setter
//    private Client client;
    private final ExecutorService threadPool;


    public InlineChequeHandler(Cryptobot cryptobot) {
        this.cryptobot = cryptobot;
        this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    private boolean isViaCryptobot(TdApi.Message message) {
        return message.viaBotUserId == Cryptobot.USER_ID;
    }

//    private boolean isChequeCreatingButton(TdApi.InlineKeyboardButtonType inlineKeyboardButtonType) {
//        return inlineKeyboardButtonType.getConstructor() == TdApi.InlineKeyboardButtonTypeCallback.CONSTRUCTOR &&
//                Arrays.equals(((TdApi.InlineKeyboardButtonTypeCallback) inlineKeyboardButtonType).data, CREATING_CALLBACK_DATA);
//    }

    private String getChequeIdIfReleased(TdApi.InlineKeyboardButtonType inlineKeyboardButtonType) {
        if (inlineKeyboardButtonType.getConstructor() == TdApi.InlineKeyboardButtonTypeUrl.CONSTRUCTOR) {
            String url = ((TdApi.InlineKeyboardButtonTypeUrl) inlineKeyboardButtonType).url;
            if (url.length() == CHEQUE_URL_LENGTH &&
                    url.charAt(CHEQUE_ID_OFFSET) == 'C') {
                return extractChequeId(url);
            }
        }
        return null;
    }

    private String extractChequeId(String url) {
        return url.substring(CHEQUE_ID_OFFSET);
    }

    private void onUpdate(TdApi.Object update) {
        switch (update.getConstructor()) {
            case TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                TdApi.Message message = ((TdApi.UpdateNewMessage) update).message;
                if (isViaCryptobot(message) &&
                        message.replyMarkup != null) {
                    TdApi.InlineKeyboardButtonType inlineKeyboardButtonType = ((TdApi.ReplyMarkupInlineKeyboard) message.replyMarkup).rows[0][0].type;
                    String chequeId = getChequeIdIfReleased(inlineKeyboardButtonType);
                    if (chequeId != null) {
                        cryptobot.activate(chequeId);
                    }
                }
            }
            case TdApi.UpdateMessageEdited.CONSTRUCTOR -> {
                TdApi.ReplyMarkup replyMarkup = ((TdApi.UpdateMessageEdited) update).replyMarkup;
                if (replyMarkup != null && replyMarkup.getConstructor() == TdApi.ReplyMarkupInlineKeyboard.CONSTRUCTOR) {
                    String chequeId = getChequeIdIfReleased(((TdApi.ReplyMarkupInlineKeyboard) replyMarkup).rows[0][0].type);
                    if (chequeId != null) {
                        cryptobot.activate(chequeId);
                    }
                }
            }
        }
    }

    @Override
    public void onResult(TdApi.Object update) {
        threadPool.submit(() -> onUpdate(update));
    }
}

