package com.x2ge.mqtt.utils;

import java.util.concurrent.*;

public abstract class AsyncTask<T> implements RunnableFuture<T>, Callable<T> {

    private static volatile Executor sDefaultExecutor = Executors.newCachedThreadPool();

    private FutureTask<T> futureTask;

    private Callback callback;

    private boolean hasExecuted = false;

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

    public final AsyncTask<T> execute() {
        setHasExecuted(true);
        try {
            sDefaultExecutor.execute(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public final AsyncTask<T> executeOnExecutor(Executor exec) {
        setHasExecuted(true);
        try {
            exec.execute(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public boolean isHasExecuted() {
        return hasExecuted;
    }

    public void setHasExecuted(boolean hasExecuted) {
        this.hasExecuted = hasExecuted;
    }

    protected void done() {

    }

    AsyncTask<T> setCallback(Callback callback) {
        this.callback = callback;
        return this;
    }

    interface Callback {
        void onDone();
    }

}
