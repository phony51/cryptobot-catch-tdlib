package org.topsmoker.cryptobot.clients;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.topsmoker.cryptobot.utils.SyncClient;


import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChequeHandler implements Client.ResultHandler, AutoCloseable {
    private final ExecutorService inlineThreadPool;
    private final ExecutorService regexThreadPool;
    private final ScheduledExecutorService pollingService;

    private final int CHEQUE_URL_LENGTH = 35;
    private final int CHEQUE_ID_LENGTH = 12;
    private final int CHEQUE_ID_OFFSET = CHEQUE_URL_LENGTH - CHEQUE_ID_LENGTH;
    private final Pattern chequeIdPattern = Pattern.compile("CQ[A-z\\d]{10}");
    private final Cryptobot cryptobot;
    private final long pollingPeriodMs;
    private final long pollingTimeoutMs;
    private SyncClient syncClient;

    @Override
    public void close() throws Exception {
        pollingService.shutdown();
        inlineThreadPool.shutdown();
        regexThreadPool.shutdown();
    }

    private class ChequePollingTask implements Runnable {
        private final static int ACTIVATED_URL_LENGTH = 34;
        private final AtomicReference<TdApi.GetMessage> getMessage = new AtomicReference<>(new TdApi.GetMessage());
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
                TdApi.GetMessage request = getMessage.get();
                request.chatId = chatId;
                request.messageId = messageId;
                TdApi.Object result = syncClient.execute(request);
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
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public void setClient(Client client) {
        this.syncClient = new SyncClient(client);
    }

    public ChequeHandler(Cryptobot cryptobot,
                         long pollingPeriodMs, long pollingTimeoutMs,
                         int inlineThreadsCount, int regexThreadsCount) {
        this.cryptobot = cryptobot;
        this.pollingPeriodMs = pollingPeriodMs;
        this.pollingTimeoutMs = pollingTimeoutMs;
        this.pollingService = Executors.newSingleThreadScheduledExecutor();
        this.inlineThreadPool = Executors.newFixedThreadPool(inlineThreadsCount);
        this.regexThreadPool = Executors.newFixedThreadPool(regexThreadsCount);

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

    private void findChequeIdInMessage(TdApi.UpdateNewMessage updateNewMessage) {
        if (updateNewMessage.message.content.getConstructor() == TdApi.MessageText.CONSTRUCTOR) {
            Matcher m = chequeIdPattern.matcher(((TdApi.MessageText) updateNewMessage.message.content).text.text);
            if (m.find()) {
                cryptobot.activate(m.toMatchResult().group());
            }
        }
    }

    private void findCreatingOrForwardedCheques(TdApi.UpdateNewMessage updateNewMessage) {
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

    private void findCreatedCheques(TdApi.UpdateMessageEdited updateMessageEdited) {
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
                inlineThreadPool.submit(() -> findCreatingOrForwardedCheques(updateNewMessage));
                regexThreadPool.submit(() -> findChequeIdInMessage(updateNewMessage));
            }
            case TdApi.UpdateMessageEdited.CONSTRUCTOR -> {
                TdApi.UpdateMessageEdited updateMessageEdited = (TdApi.UpdateMessageEdited) update;
                inlineThreadPool.submit(() -> findCreatedCheques(updateMessageEdited));
            }
        }
    }

}

