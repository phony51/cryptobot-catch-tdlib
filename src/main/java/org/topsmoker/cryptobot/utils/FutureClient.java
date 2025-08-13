package org.topsmoker.cryptobot.utils;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.TdApi.Function;

import java.util.concurrent.CompletableFuture;

public class FutureClient {
    private final Client client;

    public static class ExecutionException extends Exception {
        public final TdApi.Error error;

        ExecutionException(TdApi.Error error) {
            super(error.code + ": " + error.message);
            this.error = error;
        }
    }

    public FutureClient(Client client) {
        this.client = client;
    }

    public CompletableFuture<TdApi.Object> execute(Function<?> query) {
        CompletableFuture<TdApi.Object> future = new CompletableFuture<>();

        client.send(query, result -> {
            if (result.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                future.completeExceptionally(new ExecutionException(((TdApi.Error) result)));
            } else {
                future.complete(result);
            }
        });

        return future;
    }
}