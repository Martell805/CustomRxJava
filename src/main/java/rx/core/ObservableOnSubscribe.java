package rx.core;

/**
 * A function that subscribes an {@link Emitter} to a data source.
 *
 * @param <T> the type of items emitted
 */
@FunctionalInterface
public interface ObservableOnSubscribe<T> {

    /** Called when an observer subscribes; implementations should emit items via {@code emitter}. */
    void subscribe(Emitter<T> emitter) throws Exception;
}
