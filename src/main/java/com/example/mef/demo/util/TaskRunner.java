package com.example.mef.demo.util;



import javafx.concurrent.Task;

/**
 * Runs JavaFX {@link Task}s on a daemon thread.
 * Extracted from DashboardController.startDaemonThread(Runnable)
 * so every extracted view class can share the same helper instead
 * of depending on the controller.
 */
public final class TaskRunner {

    private TaskRunner() {
    }

    /** Starts any Runnable (including a Task) on a daemon thread. */
    public static void run(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Convenience overload: starts the given Task on a daemon thread
     * and returns it, so callers can chain setOnSucceeded/setOnFailed
     * before calling this method, e.g.:
     *
     * <pre>
     * Task&lt;List&lt;Row&gt;&gt; task = ...;
     * task.setOnSucceeded(e -&gt; ...);
     * task.setOnFailed(e -&gt; ...);
     * TaskRunner.start(task);
     * </pre>
     */
    public static <T> Task<T> start(Task<T> task) {
        run(task);
        return task;
    }
}