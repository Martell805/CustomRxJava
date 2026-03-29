package rx.schedulers;

/**
 * Factory providing singleton scheduler instances.
 */
public final class Schedulers {

    private static final Scheduler IO          = new IOThreadScheduler();
    private static final Scheduler COMPUTATION = new ComputationScheduler();
    private static final Scheduler SINGLE      = new SingleThreadScheduler();

    private Schedulers() {}

    /** Returns the shared IO scheduler. */
    public static Scheduler io() {
        return IO;
    }

    /** Returns the shared computation scheduler. */
    public static Scheduler computation() {
        return COMPUTATION;
    }

    /** Returns the shared single-thread scheduler. */
    public static Scheduler single() {
        return SINGLE;
    }
}
