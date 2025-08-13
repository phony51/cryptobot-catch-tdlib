package org.topsmoker.cryptobot.clients;

import lombok.Setter;
import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.topsmoker.cryptobot.utils.SyncClient;


import java.time.LocalDateTime;
import java.util.concurrent.*;


public class InlineChequeHandler implements Client.ResultHandler {
    private final ExecutorService threadPool;
    private final long CHEQUE_POLLING_PERIOD_MILLIS = 15;
    private final long CHEQUE_POLLING_TIMEOUT_S = 2;
    private final int CHEQUE_URL_LENGTH = 35;
    private final int CHEQUE_ID_LENGTH = 12;
    private final int CHEQUE_ID_OFFSET = CHEQUE_URL_LENGTH - CHEQUE_ID_LENGTH;
    private final Cryptobot cryptobot;
    private final ScheduledExecutorService pollingService;
    @Setter
    private Client client;

    private class ChequePollingTask implements Runnable {
        private final static int ACTIVATED_URL_LENGTH = 34;
        private final long messageId;
        private final long chatId;
        private final SyncClient syncClient;

        ChequePollingTask(long chatId, long messageId) {
            this.messageId = messageId;
            this.chatId = chatId;
            this.syncClient = new SyncClient(client);
        }

        private static boolean isActivated(String url) {
            return url.length() == ACTIVATED_URL_LENGTH;
        }

        public void run() {
            try {
                TdApi.Object result = syncClient.execute(new TdApi.GetMessage(chatId, messageId));
                TdApi.InlineKeyboardButton button = ((TdApi.ReplyMarkupInlineKeyboard) ((TdApi.Message) result).replyMarkup).rows[0][0];
                if (!isChequeCreatingButton(button)) {
                    String url = ((TdApi.InlineKeyboardButtonTypeUrl) button.type).url;
                    String chequeId = unsafeExtractChequeId(url);
                    if (chequeId != null) {
                        cryptobot.activate(chequeId);
                        throw new RuntimeException();
                    } else if (isActivated(url)) {
                        throw new RuntimeException();
                    }
                }
            } catch (SyncClient.ExecutionException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public InlineChequeHandler(Cryptobot cryptobot) {
        this.cryptobot = cryptobot;
        this.pollingService = Executors.newSingleThreadScheduledExecutor();
        this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    private boolean isViaCryptobot(TdApi.Message message) {
        return message.viaBotUserId == Cryptobot.USER_ID;
    }

    private boolean isChequeCreatingButton(TdApi.InlineKeyboardButton inlineKeyboardButton) {
        return inlineKeyboardButton.text.charAt(0) == '…';
    }

    private String extractChequeId(String url) {
        if (url.length() == CHEQUE_URL_LENGTH &&
                url.charAt(CHEQUE_ID_OFFSET) == 'C') {
            return url.substring(CHEQUE_ID_OFFSET);
        }
        return null;
    }

    private String unsafeExtractChequeId(String url) {
        if (url.length() == CHEQUE_URL_LENGTH) {
            return url.substring(CHEQUE_ID_OFFSET);
        }
        return null;
    }


    private void onUpdate(TdApi.Object update) {
        switch (update.getConstructor()) {
            case TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                TdApi.Message message = ((TdApi.UpdateNewMessage) update).message;
                if (isViaCryptobot(message) &&
                        message.replyMarkup != null) {
                    TdApi.InlineKeyboardButton button = ((TdApi.ReplyMarkupInlineKeyboard) message.replyMarkup).rows[0][0];
                    if (isChequeCreatingButton(button)) {
                        ScheduledFuture<?> pollingFuture = pollingService.scheduleAtFixedRate(new ChequePollingTask(message.chatId, message.id),
                                0,
                                CHEQUE_POLLING_PERIOD_MILLIS,
                                TimeUnit.MILLISECONDS);
                        pollingService.schedule(() -> {
                            pollingFuture.cancel(true);
                        }, CHEQUE_POLLING_TIMEOUT_S, TimeUnit.SECONDS);
                    } else {
                        String chequeId = extractChequeId(((TdApi.InlineKeyboardButtonTypeUrl) button.type).url);
                        if (chequeId != null) {
                            cryptobot.activate(chequeId);
                        }
                    }
                }
            }
            case TdApi.UpdateMessageEdited.CONSTRUCTOR -> {
                TdApi.ReplyMarkup replyMarkup = ((TdApi.UpdateMessageEdited) update).replyMarkup;
                if (replyMarkup != null && replyMarkup.getConstructor() == TdApi.ReplyMarkupInlineKeyboard.CONSTRUCTOR) {
                    String chequeId = extractChequeId(((TdApi.InlineKeyboardButtonTypeUrl) ((TdApi.ReplyMarkupInlineKeyboard) replyMarkup).
                            rows[0][0].type)
                            .url);
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

