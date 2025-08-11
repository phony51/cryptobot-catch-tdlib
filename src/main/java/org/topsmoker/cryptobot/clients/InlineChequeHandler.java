package org.topsmoker.cryptobot.clients;

import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import lombok.Setter;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class InlineChequeHandler implements Client.ResultHandler {
    private final int CHEQUE_URL_LENGTH = 35;
    private final int CHEQUE_ID_LENGTH = 12;
    protected final int CHEQUE_ID_OFFSET = CHEQUE_URL_LENGTH - CHEQUE_ID_LENGTH;
    private final byte[] CREATING_CALLBACK_DATA = "check-creating".getBytes();
    private final Cryptobot cryptobot;
    private final ChequeMessageCache chequeMessageCache;
    @Setter
    private Client client;
    private final ExecutorService threadPool;

    private static class ChequeMessageCache {
        private final LongHashSet set;
        private int size;

        ChequeMessageCache() {
            this.set = new LongHashSet();
            this.size = 0;
        }

        public ChequeMessageCache withSize(int size) {
            this.size = size;
            return this;
        }

        public void add(long messageId) {
            if (set.size() > size) {
                set.clear();
            }
            set.add(messageId);
        }

        public boolean check(long messageId) {
            return set.contains(messageId);
        }

        public void remove(long messageId) {
            set.remove(messageId);
        }
    }

    public InlineChequeHandler(Cryptobot cryptobot) {
        this.cryptobot = cryptobot;
        this.chequeMessageCache = new ChequeMessageCache().withSize(32);
        this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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
                return getChequeId(url);
            }
        }
        return null;
    }

    private String getChequeId(String url) {
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
                    } else if (isChequeCreatingButton(inlineKeyboardButtonType)) {
                        chequeMessageCache.add(message.id);
                    }
                }
            }
            case TdApi.UpdateMessageEdited.CONSTRUCTOR -> {
                TdApi.UpdateMessageEdited u = ((TdApi.UpdateMessageEdited) update);
                if (chequeMessageCache.check(u.messageId)) {
                    cryptobot.activate(getChequeIdIfReleased(((TdApi.ReplyMarkupInlineKeyboard) u.replyMarkup).rows[0][0].type));
                    chequeMessageCache.remove(u.messageId);
                }
            }
        }
    }

    @Override
    public void onResult(TdApi.Object update) {
        threadPool.submit(() -> onUpdate(update));
    }
}

