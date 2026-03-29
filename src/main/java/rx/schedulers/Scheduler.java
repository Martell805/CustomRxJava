package rx.schedulers;

/**
 * Abstraction over a thread or thread pool that can execute tasks.
 */
public interface Scheduler {

    /** Schedules {@code task} for execution on this scheduler. */
    void execute(Runnable task);

    /** Shuts down any underlying thread pool. */
    void shutdown();
}
