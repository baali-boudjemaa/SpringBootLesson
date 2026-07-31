package com.example.mef.demo.dashboard.common;

import javafx.concurrent.Task;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Collapses the repeated "background Task + setOnSucceeded/setOnFailed +
 * daemon thread" pattern that appeared ~8 times in the old
 * DashboardController into a single reusable call.
 *
 * Usage:
 * <pre>
 * AsyncTasks.run(
 *     () -> dao.findAll(...),          // background work (any thread)
 *     result -> renderRows(result),    // onSuccess (runs on FX thread)
 *     err -> DialogUtil.error(...)     // onFailure (runs on FX thread)
 * );
 * </pre>
 */
public final class AsyncTasks {

    private AsyncTasks() {
    }

    /**
     * Runs {@code work} on a daemon background thread. On success,
     * {@code onSuccess} is invoked on the JavaFX application thread with
     * the produced value. On failure, {@code onFailure} is invoked on the
     * JavaFX application thread with the thrown exception.
     */
    public static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onFailure.accept(task.getException()));
        startDaemonThread(task);
    }

    /**
     * Variant for background work that produces no value (Void), e.g. an
     * insert/update/delete/backup/restore call.
     */
    public static void run(Runnable work, Runnable onSuccess, Consumer<Throwable> onFailure) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                work.run();
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onFailure.accept(task.getException()));
        startDaemonThread(task);
    }

    private static void startDaemonThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }
}