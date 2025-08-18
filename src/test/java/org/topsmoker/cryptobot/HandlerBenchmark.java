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
public class HandlerBenchmark {
    private ChequeHandler chequeHandler;


    @Setup
    public void setup() throws Exception {
        chequeHandler = new ChequeHandler(new MockActivator(), null);
        chequeHandler.close();
    }

    @Benchmark
    public boolean benchmarkForwardedCheque() {
        return chequeHandler.findCreatingOrForwardedCheque(Updates.getForwardedCheque());
    }


    @Benchmark
    public boolean benchmarkRegexCheque() {
        return chequeHandler.findChequeIdInMessage(Updates.getRegexCheque());
    }


    @Benchmark
    public boolean benchmarkInlineCheque() {
        return chequeHandler.findCreatedCheque(Updates.getInlineCheque());
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
