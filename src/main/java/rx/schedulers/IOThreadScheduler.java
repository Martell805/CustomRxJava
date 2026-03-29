package rx.schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduler backed by a cached thread pool.
 * Suitable for IO-bound work where threads spend most of their time waiting.
 * Analogous to {@code Schedulers.io()} in RxJava.
 */
public class IOThreadScheduler implements Scheduler {

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setName("io-" + t.getId());
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
