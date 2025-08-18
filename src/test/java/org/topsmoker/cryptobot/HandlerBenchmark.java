package org.topsmoker.cryptobot;


import org.topsmoker.cryptobot.mocks.MockActivator;
import org.openjdk.jmh.annotations.*;
import org.topsmoker.cryptobot.cheques.ChequeHandler;
import org.topsmoker.cryptobot.cases.Updates;
import java.util.concurrent.TimeUnit;


@State(Scope.Benchmark)
@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
@Threads(4)
@Fork(2)
public class HandlerBenchmark {
    private ChequeHandler chequeHandler;


    @Setup
    public void setup() throws Exception {
        chequeHandler = new ChequeHandler(new MockActivator(), null, 1);
        chequeHandler.close();
    }

    @Benchmark
    public boolean benchmarkForwardedCheque() {
        return chequeHandler.findCreatingOrForwardedCheque(Updates.getForwardedCheque());
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
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
