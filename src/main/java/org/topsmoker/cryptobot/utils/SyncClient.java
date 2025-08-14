package org.topsmoker.cryptobot.utils;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.TdApi.Function;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SyncClient {
    private final Client client;
    private final ReentrantLock resultLock = new ReentrantLock();
    private final Condition gotResult = resultLock.newCondition();
    private TdApi.Object result;
    private boolean haveResult = false;

    public SyncClient(Client client) {
        this.client = client;
    }

    public TdApi.Object execute(Function<?> query) throws InterruptedException {
        resultLock.lock();
        try {
            result = null;
            haveResult = false;

            client.send(query, object -> {
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
                gotResult.await();
            }

            return result;
        } finally {
            resultLock.unlock();
        }
    }
}