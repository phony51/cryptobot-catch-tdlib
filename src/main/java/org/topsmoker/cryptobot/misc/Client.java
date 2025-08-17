package org.topsmoker.cryptobot.misc;

import org.drinkless.tdlib.TdApi;

public interface Client {
    void send(TdApi.Function<?> function, org.drinkless.tdlib.Client.ResultHandler resultHandler);
    <T extends TdApi.Object> T execute(TdApi.Function<T> function) throws ExecutionException;
}
