package rx.schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduler backed by a fixed thread pool sized to the number of available CPU cores.
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
