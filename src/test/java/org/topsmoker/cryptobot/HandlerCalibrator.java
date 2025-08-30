package org.topsmoker.cryptobot;

import org.topsmoker.cryptobot.cases.Updates;
import org.topsmoker.cryptobot.cheques.ChequeHandler;
import org.topsmoker.cryptobot.mocks.MockActivator;

import java.util.stream.LongStream;


public class HandlerCalibrator {
    private static ChequeHandler chequeHandler;
    private static final int updatesCount = 10000;
    private static final int iterations = 5000;


    public static void benchmark() throws Exception {
        for (int i = 0; i < updatesCount; i++) {
            chequeHandler.onResult(Updates.getForwardedCheque());
            chequeHandler.onResult(Updates.getRegexCheque());
            if (i % 10 == 0) {
                chequeHandler.onResult(Updates.getInlineCheque());
            }
        }
        chequeHandler.close();
    }

    public static void main(String[] args) throws Exception {
        long[] results = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            chequeHandler = new ChequeHandler(new MockActivator(), null);
            long start = System.nanoTime();
            benchmark();
            results[i] = (System.nanoTime() - start) / updatesCount;
        }
        LongStream.of(results).average().ifPresent(result -> {
            System.out.println(result / 1000 + "us per update"); // NOT REAL
        });
    }
}