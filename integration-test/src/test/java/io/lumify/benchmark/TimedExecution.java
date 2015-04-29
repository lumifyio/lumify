package io.lumify.benchmark;

import com.google.common.base.Stopwatch;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;

import io.lumify.it.TestClassAndMethod;
import java.util.concurrent.Callable;

public class TimedExecution {
    private final TestClassAndMethod testClassAndMethod;
    private final LumifyLogger logger;

    public TimedExecution(TestClassAndMethod testClassAndMethod) {
        this.testClassAndMethod = testClassAndMethod;
        this.logger = LumifyLoggerFactory.getLogger(this.getClass());
    }

    public <T> Result<T> call(Callable<T> callable) throws Exception {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        T result = callable.call();
        stopwatch.stop();
        long elapsedMillis = stopwatch.elapsedMillis();
        logger.info("%s#%s: %d ms", testClassAndMethod.getClassName(), testClassAndMethod.getMethodName(),
                elapsedMillis);
        return new Result<>(stopwatch.elapsedMillis(), result);
    }

    public static class Result<T> {
        final long timeMillis;
        final T result;

        Result(long timeMillis, T result) {
            this.timeMillis = timeMillis;
            this.result = result;
        }
    }
}
