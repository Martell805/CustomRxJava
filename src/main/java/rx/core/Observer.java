package rx.core;

/**
 * Receives items and lifecycle events emitted by an {@link Observable}.
 *
 * @param <T> the type of items observed
 */
public interface Observer<T> {

    /** Called for each item emitted by the source. */
    void onNext(T item);

    /** Called when the source terminates with an error. No further events are emitted after this. */
    void onError(Throwable t);

    /** Called when the source has emitted all items without error. */
    void onComplete();
}
