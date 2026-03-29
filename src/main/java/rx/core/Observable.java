package rx.core;

import rx.disposable.Disposable;
import rx.disposable.SimpleDisposable;
import rx.schedulers.Scheduler;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a lazy, composable stream of items.
 *
 * @param <T> the type of items emitted
 */
public class Observable<T> {

    private final ObservableOnSubscribe<T> source;

    private Observable(ObservableOnSubscribe<T> source) {
        this.source = source;
    }

    /**
     * Creates an Observable from a subscription function.
     *
     * @param source function that emits items via the provided {@link Emitter}
     * @param <T>    the item type
     * @return a new Observable
     */
    public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        return new Observable<>(source);
    }

    /**
     * Subscribes an observer to this Observable and returns a {@link Disposable}
     * that can be used to cancel the subscription.
     *
     * @param observer the downstream observer
     * @return a Disposable representing the subscription
     */
    public Disposable subscribe(Observer<T> observer) {
        SimpleDisposable disposable = new SimpleDisposable();

        Emitter<T> emitter = new Emitter<T>() {
            @Override
            public void onNext(T item) {
                if (!disposable.isDisposed()) {
                    observer.onNext(item);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (!disposable.isDisposed()) {
                    observer.onError(t);
                }
            }

            @Override
            public void onComplete() {
                if (!disposable.isDisposed()) {
                    observer.onComplete();
                }
            }
        };

        try {
            source.subscribe(emitter);
        } catch (Exception e) {
            emitter.onError(e);
        }

        return disposable;
    }

    /**
     * Convenience overload that accepts lambda-style callbacks.
     *
     * @param onNext     called for each item
     * @param onError    called on error
     * @param onComplete called on completion
     * @return a Disposable representing the subscription
     */
    public Disposable subscribe(
            java.util.function.Consumer<T> onNext,
            java.util.function.Consumer<Throwable> onError,
            Runnable onComplete) {

        return subscribe(new Observer<T>() {
            @Override public void onNext(T item)     { onNext.accept(item); }
            @Override public void onError(Throwable t) { onError.accept(t); }
            @Override public void onComplete()        { onComplete.run(); }
        });
    }

    /**
     * Transforms each item emitted by this Observable by applying {@code mapper}.
     *
     * @param mapper transformation function
     * @param <R>    the result type
     * @return a new Observable emitting transformed items
     */
    public <R> Observable<R> map(Function<T, R> mapper) {
        return Observable.create(emitter ->
                this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        try {
                            emitter.onNext(mapper.apply(item));
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    }

                    @Override public void onError(Throwable t) { emitter.onError(t); }
                    @Override public void onComplete()          { emitter.onComplete(); }
                }));
    }

    /**
     * Filters items emitted by this Observable, forwarding only those satisfying {@code predicate}.
     *
     * @param predicate test applied to each item
     * @return a new Observable emitting only matching items
     */
    public Observable<T> filter(Predicate<T> predicate) {
        return Observable.create(emitter ->
                this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        try {
                            if (predicate.test(item)) {
                                emitter.onNext(item);
                            }
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    }

                    @Override public void onError(Throwable t) { emitter.onError(t); }
                    @Override public void onComplete()          { emitter.onComplete(); }
                }));
    }

    /**
     * Transforms each item into an Observable and merges the resulting streams sequentially.
     *
     * @param mapper function that maps each item to an inner Observable
     * @param <R>    the result type
     * @return a new Observable emitting items from all inner Observables
     */
    public <R> Observable<R> flatMap(Function<T, Observable<R>> mapper) {
        return Observable.create(emitter ->
                this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        try {
                            mapper.apply(item).subscribe(new Observer<R>() {
                                @Override public void onNext(R r)          { emitter.onNext(r); }
                                @Override public void onError(Throwable t) { emitter.onError(t); }
                                @Override public void onComplete()          {}
                            });
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    }

                    @Override public void onError(Throwable t) { emitter.onError(t); }
                    @Override public void onComplete()          { emitter.onComplete(); }
                }));
    }

    /**
     * Specifies the {@link Scheduler} on which the subscription (source emission) will run.
     *
     * @param scheduler the scheduler for the upstream subscription
     * @return a new Observable whose source runs on {@code scheduler}
     */
    public Observable<T> subscribeOn(Scheduler scheduler) {
        return Observable.create(emitter ->
                scheduler.execute(() -> {
                    try {
                        source.subscribe(emitter);
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                }));
    }

    /**
     * Specifies the {@link Scheduler} on which downstream observer callbacks will run.
     *
     * @param scheduler the scheduler for downstream notifications
     * @return a new Observable that delivers notifications on {@code scheduler}
     */
    public Observable<T> observeOn(Scheduler scheduler) {
        return Observable.create(emitter ->
                this.subscribe(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        scheduler.execute(() -> emitter.onNext(item));
                    }

                    @Override
                    public void onError(Throwable t) {
                        scheduler.execute(() -> emitter.onError(t));
                    }

                    @Override
                    public void onComplete() {
                        scheduler.execute(emitter::onComplete);
                    }
                }));
    }
}
