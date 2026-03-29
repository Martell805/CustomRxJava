package rx.schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduler backed by a single dedicated thread.
 * Guarantees sequential execution of all tasks.
 * Analogous to {@code Schedulers.single()} in RxJava.
 */
public class SingleThreadScheduler implements Scheduler {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setName("single");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}
