package rx.core;

/**
 * Provides the emission API available inside {@link Observable#create(ObservableOnSubscribe)}.
 *
 * @param <T> the type of items to emit
 */
public interface Emitter<T> {

    /** Emits a single item downstream. */
    void onNext(T item);

    /** Terminates the stream with an error. */
    void onError(Throwable t);

    /** Signals successful completion of the stream. */
    void onComplete();
}
