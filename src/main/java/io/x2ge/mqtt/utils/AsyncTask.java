package io.x2ge.mqtt.utils;

import java.util.concurrent.*;

public abstract class AsyncTask<T> implements RunnableFuture<T>, Callable<T> {

    private static volatile Executor sDefaultExecutor = Executors.newCachedThreadPool();

    private FutureTask<T> futureTask;

    public AsyncTask() {
        this.futureTask = new FutureTask<T>(this) {
            @Override
            protected void done() {
                AsyncTask.this.done();
                if (!isCancelled() && callback != null) {
                    callback.onDone();
                }
            }
        };
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return futureTask.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return futureTask.isCancelled();
    }

    @Override
    public boolean isDone() {
        return futureTask.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return futureTask.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return futureTask.get(timeout, unit);
    }

    @Override
    public void run() {
        futureTask.run();
    }

    protected void done() {

    }

    public AsyncTask<T> execute() {
        try {
            sDefaultExecutor.execute(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public AsyncTask<T> executeOnExecutor(Executor executor) {
        try {
            executor.execute(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    private Callback callback;

    public AsyncTask<T> setCallback(Callback callback) {
        this.callback = callback;
        return this;
    }

    public interface Callback {
        void onDone();
    }

}
