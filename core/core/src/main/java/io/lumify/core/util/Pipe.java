package io.lumify.core.util;

import io.lumify.core.exception.LumifyException;

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
        final boolean[] threadStarted = new boolean[1];
        threadStarted[0] = false;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int read;
                    byte[] buffer = new byte[1 * 1024 * 1024];
                    completionSemaphore.acquire();
                    threadStarted[0] = true;
                    while ((read = in.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                } catch (IOException e) {
                    statusHandler.handleException(e);
                } catch (InterruptedException e) {
                    statusHandler.handleException(e);
                } finally {
                    threadStarted[0] = true; // if something fails before we get a chance to set this to true make sure it happens
                    statusHandler.handleComplete();
                    completionSemaphore.release();
                }
            }
        });

        // It could be possible for the process to exit before this thread gets started resulting in this thread
        //   to not read the output stream. This waiting is to make sure the thread is started before we return.
        //   http://stackoverflow.com/questions/2150723/process-waitfor-threads-and-inputstreams (see resolution)
        while (!threadStarted[0]) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new LumifyException("Could not sleep", e);
            }
        }
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
