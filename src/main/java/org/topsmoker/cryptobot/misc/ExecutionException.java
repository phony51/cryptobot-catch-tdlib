package org.topsmoker.cryptobot.misc;

import org.drinkless.tdlib.TdApi;

public class ExecutionException extends RuntimeException {
    public ExecutionException(TdApi.Error error) {
        super(error.message);
    }
    public ExecutionException(Exception e) {
        super(e);
    }
}
