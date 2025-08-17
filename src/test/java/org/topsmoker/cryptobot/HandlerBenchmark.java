package org.topsmoker.cryptobot;


import org.openjdk.jmh.infra.Control;
import org.topsmoker.cryptobot.mocks.MockActivator;
import org.openjdk.jmh.annotations.*;
import org.topsmoker.cryptobot.cheques.ChequeHandler;
import org.topsmoker.cryptobot.cases.Updates;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(2)
public class HandlerBenchmark {
    private ChequeHandler chequeHandler;
    private MockActivator activator;
    private final int inlineThreadsCount = 1;
    private final int regexThreadsCount = 2;

    @Setup
    public void setupBenchmark() {
        activator = new MockActivator();
    }

    @Setup(Level.Iteration)
    public void setup() {
        chequeHandler = new ChequeHandler(
                activator,
                null,
                inlineThreadsCount,
                regexThreadsCount
        );
    }

    @Benchmark
    public int benchmarkForwardedCheque(Control control) throws Exception {
        while (!control.stopMeasurement) {
            chequeHandler.onResult(Updates.getForwardedCheque());
        }
        chequeHandler.close();
        return 0;
    }

    @Benchmark
    public int benchmarkRegexCheque(Control control) throws Exception {
        while (!control.stopMeasurement) {
            chequeHandler.onResult(Updates.getForwardedCheque());
        }
        chequeHandler.close();
        return 0;
    }


//    @Benchmark
//    public int benchmarkInlineCheque(Control control) throws Exception {
//        while (!control.stopMeasurement) {
//            chequeHandler.onResult(Updates.getForwardedCheque());
//        }
//        chequeHandler.close();
//        return 0;
//    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
