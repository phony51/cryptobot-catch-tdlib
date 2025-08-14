package org.topsmoker.cryptobot.clients;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.topsmoker.cryptobot.utils.FutureClient;


import java.util.concurrent.*;


public class InlineChequeHandler implements Client.ResultHandler, AutoCloseable {
    private final ExecutorService threadPool;
    private final int CHEQUE_URL_LENGTH = 35;
    private final int CHEQUE_ID_LENGTH = 12;
    private final int CHEQUE_ID_OFFSET = CHEQUE_URL_LENGTH - CHEQUE_ID_LENGTH;
    private final Cryptobot cryptobot;
    private final ScheduledExecutorService pollingService;
    private final long pollingPeriodMs;
    private final long pollingTimeoutMs;
    private FutureClient futureClient;

    @Override
    public void close() throws Exception {
        pollingService.shutdown();
        threadPool.shutdown();
    }

    private class ChequePollingTask implements Runnable {
        private final static int ACTIVATED_URL_LENGTH = 34;
        private final long messageId;
        private final long chatId;

        ChequePollingTask(long chatId, long messageId) {
            this.messageId = messageId;
            this.chatId = chatId;
        }

        private static boolean isActivated(String url) {
            return url.length() == ACTIVATED_URL_LENGTH;
        }

        public void run() {
            try {
                TdApi.Object result = futureClient.execute(new TdApi.GetMessage(chatId, messageId)).get();
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
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public void setClient(Client client) {
        this.futureClient = new FutureClient(client);
    }

    public InlineChequeHandler(Cryptobot cryptobot, long pollingPeriodMs, long pollingTimeoutMs) {
        this.cryptobot = cryptobot;
        this.pollingPeriodMs = pollingPeriodMs;
        this.pollingTimeoutMs = pollingTimeoutMs;
        this.pollingService = Executors.newSingleThreadScheduledExecutor();
        this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
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

    private void handleNewMessage(TdApi.UpdateNewMessage updateNewMessage) {
        TdApi.Message message = updateNewMessage.message;
        if (isViaCryptobot(message) &&
                message.replyMarkup != null) {
            TdApi.InlineKeyboardButton button = ((TdApi.ReplyMarkupInlineKeyboard) message.replyMarkup).rows[0][0];
            if (isChequeCreatingButton(button)) {
                ScheduledFuture<?> pollingFuture = pollingService.scheduleAtFixedRate(new ChequePollingTask(message.chatId, message.id),
                        0,
                        pollingPeriodMs,
                        TimeUnit.MILLISECONDS);
                pollingService.schedule(() -> {
                    pollingFuture.cancel(true);
                }, pollingTimeoutMs, TimeUnit.MILLISECONDS);
            } else {
                String chequeId = extractChequeId(((TdApi.InlineKeyboardButtonTypeUrl) button.type).url);
                if (chequeId != null) {
                    cryptobot.activate(chequeId);
                }
            }
        }
    }

    private void handleMessageEdited(TdApi.UpdateMessageEdited updateMessageEdited) {
        TdApi.ReplyMarkup replyMarkup = updateMessageEdited.replyMarkup;
        if (replyMarkup != null && replyMarkup.getConstructor() == TdApi.ReplyMarkupInlineKeyboard.CONSTRUCTOR) {
            String chequeId = extractChequeId(((TdApi.InlineKeyboardButtonTypeUrl) ((TdApi.ReplyMarkupInlineKeyboard) replyMarkup).
                    rows[0][0].type)
                    .url);
            if (chequeId != null) {
                cryptobot.activate(chequeId);
            }
        }
    }

    @Override
    public void onResult(TdApi.Object update) {
        switch (update.getConstructor()) {
            case TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                TdApi.UpdateNewMessage updateNewMessage = (TdApi.UpdateNewMessage) update;
                threadPool.submit(() -> handleNewMessage(updateNewMessage));
            }
            case TdApi.UpdateMessageEdited.CONSTRUCTOR -> {
                TdApi.UpdateMessageEdited updateMessageEdited = (TdApi.UpdateMessageEdited) update;
                threadPool.submit(() -> handleMessageEdited(updateMessageEdited));
            }
        }
    }

}

