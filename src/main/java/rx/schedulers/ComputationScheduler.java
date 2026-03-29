package rx.schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduler backed by a fixed thread pool sized to the number of available CPU cores.
 * Suitable for CPU-bound computation that should not be parallelised beyond core count.
 * Analogous to {@code Schedulers.computation()} in RxJava.
 */
public class ComputationScheduler implements Scheduler {

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(), r -> {
                Thread t = new Thread(r);
                t.setName("computation-" + t.getId());
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
