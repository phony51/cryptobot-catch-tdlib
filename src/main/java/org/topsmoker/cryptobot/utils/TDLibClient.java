package org.topsmoker.cryptobot.utils;

import org.drinkless.tdlib.TdApi;
import org.topsmoker.cryptobot.misc.Client;
import org.topsmoker.cryptobot.misc.ExecutionException;
import org.drinkless.tdlib.Client.ResultHandler;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TDLibClient implements Client {

    private final org.drinkless.tdlib.Client client;
    private final ReentrantLock resultLock = new ReentrantLock();
    private final Condition gotResult = resultLock.newCondition();
    private TdApi.Object result;
    private boolean haveResult = false;
    private ResultHandler updateHandler;

    public TDLibClient(ResultHandler updateHandler) {
        this.updateHandler = updateHandler;
        this.client = org.drinkless.tdlib.Client.create(result -> this.updateHandler.onResult(result), null, null);
    }

    public void setUpdateHandler(ResultHandler updateHandler) {
        this.updateHandler = updateHandler;
    }

    @Override
    public void send(TdApi.Function<?> function, org.drinkless.tdlib.Client.ResultHandler resultHandler) {
        client.send(function, resultHandler);
    }


    @Override
    public <T extends TdApi.Object> T execute(TdApi.Function<T> function) throws ExecutionException {
        resultLock.lock();
        try {
            result = null;
            haveResult = false;

            client.send(function, object -> {
                resultLock.lock();
                try {
                    result = object;
                    haveResult = true;
                    gotResult.signal();
                } finally {
                    resultLock.unlock();
                }
            });

            while (!haveResult) {
                try {
                    gotResult.await();
                } catch (InterruptedException e) {
                    throw new ExecutionException(e);
                }
            }
            return (T) result;
        } finally {
            resultLock.unlock();
        }
    }
}

