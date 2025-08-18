package org.topsmoker.cryptobot.cheques;

import org.drinkless.tdlib.TdApi;
import org.topsmoker.cryptobot.misc.Client;
import org.topsmoker.cryptobot.misc.ExecutionException;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.topsmoker.cryptobot.cheques.Helper.*;

public class PollingService implements AutoCloseable {
    private final ScheduledExecutorService scheduledExecutor;
    private final Client client;
    private final Activator activator;
    private final long pollingPeriodMs;
    private final long pollingTimeoutMs;

    class PollingTask implements Runnable {
        private final AtomicReference<TdApi.GetMessage> getMessage = new AtomicReference<>(new TdApi.GetMessage());
        private final long messageId;
        private final long chatId;

        PollingTask(long chatId, long messageId) {
            this.messageId = messageId;
            this.chatId = chatId;
        }


        public void run() {
            try {
                TdApi.GetMessage request = getMessage.get();
                request.chatId = chatId;
                request.messageId = messageId;
                TdApi.Message result = client.execute(request);
                TdApi.InlineKeyboardButton button = ((TdApi.ReplyMarkupInlineKeyboard) result.replyMarkup).rows[0][0];
                if (!isChequeCreatingButton(button)) {
                    String url = ((TdApi.InlineKeyboardButtonTypeUrl) button.type).url;
                    String chequeId = unsafeExtractChequeId(url);
                    if (chequeId != null) {
                        activator.activate(chequeId);
                        throw new RuntimeException();
                    } else if (isActivated(url)) {
                        throw new RuntimeException();
                    }
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public PollingService(Client client, Activator activator, long pollingPeriodMs, long pollingTimeoutMs) {
        this.client = client;
        this.activator = activator;
        this.pollingPeriodMs = pollingPeriodMs;
        this.pollingTimeoutMs = pollingTimeoutMs;
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public void poll(long chatId, long messageId) {
        ScheduledFuture<?> task = scheduledExecutor.scheduleAtFixedRate(new PollingTask(chatId, messageId),
                0, pollingPeriodMs,
                TimeUnit.MILLISECONDS);
        scheduledExecutor.schedule(() -> {
            task.cancel(true);
        }, pollingTimeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws Exception {
        scheduledExecutor.close();
    }
}
