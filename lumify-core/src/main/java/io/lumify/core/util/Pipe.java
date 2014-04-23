package io.lumify.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Pipe {
    private static ExecutorService executor = Executors.newFixedThreadPool(5);
    private Semaphore completionSemaphore = new Semaphore(1);

    public Pipe pipe(final InputStream in, final OutputStream out, final StatusHandler statusHandler) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                int read;
                byte[] buffer = new byte[1 * 1024 * 1024];
                try {
                    completionSemaphore.acquire();
                    while ((read = in.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                } catch (IOException e) {
                    statusHandler.handleException(e);
                } catch (InterruptedException e) {
                    statusHandler.handleException(e);
                } finally {
                    statusHandler.handleComplete();
                    completionSemaphore.release();
                }
            }
        });
        return this;
    }

    public void waitForCompletion(long timeout, TimeUnit units) throws InterruptedException {
        completionSemaphore.tryAcquire(timeout, units);
    }

    public static class StatusHandler {
        public void handleException(Exception e) {

        }

        public void handleComplete() {

        }
    }
}
