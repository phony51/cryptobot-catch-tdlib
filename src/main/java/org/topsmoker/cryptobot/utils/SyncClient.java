package org.topsmoker.cryptobot.utils;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;
import org.drinkless.tdlib.TdApi.Function;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SyncClient {
    private final Client client;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition gotResponse = lock.newCondition();
    private volatile boolean haveResponse;
    private final AtomicReference<TdApi.Object> lastResponse = new AtomicReference<>();

    public static class ExecutionException extends Exception {
        public final TdApi.Error error;

        ExecutionException(TdApi.Error var1) {
            super(var1.code + ": " + var1.message);
            this.error = var1;
        }
    }

    public SyncClient(Client client) {
        this.client = client;
    }

    public TdApi.Object execute(Function<?> query) throws ExecutionException {
        lock.lock();
        try {
            haveResponse = false;
            lastResponse.set(null);

            client.send(query, result -> {
                lock.lock();
                try {
                    lastResponse.set(result);
                    haveResponse = true;
                    gotResponse.signalAll();
                } finally {
                    lock.unlock();
                }
            });

            while (!haveResponse) {
                try {
                    gotResponse.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Operation interrupted", e);
                }
            }
            TdApi.Object result = lastResponse.get();
            if (result.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                throw new ExecutionException((TdApi.Error) result);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}