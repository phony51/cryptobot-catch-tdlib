package org.topsmoker.cryptobot;


import org.topsmoker.cryptobot.mocks.MockActivator;
import org.openjdk.jmh.annotations.*;
import org.topsmoker.cryptobot.cheques.ChequeHandler;
import org.topsmoker.cryptobot.cases.Updates;

import java.util.concurrent.*;


@State(Scope.Benchmark)
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 4, time = 5)
@Fork(2)
public class FiltersBenchmark {
    private ChequeHandler chequeHandler;

    @Setup
    public void setup() throws Exception {
        chequeHandler = new ChequeHandler(new MockActivator(),null);
        chequeHandler.close();
    }

    @Benchmark
    public boolean benchmarkForwardedCheque() {
        return chequeHandler.findCreatingOrForwardedCheque(Updates.getForwardedCheque().message);
    }


    @Benchmark
    public boolean benchmarkRegexCheque() {
        return chequeHandler.findChequeIdInMessage(Updates.getRegexCheque().message);
    }


    @Benchmark
    public boolean benchmarkInlineCheque() {
        return chequeHandler.findCreatedCheque(Updates.getInlineCheque().replyMarkup);
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
