package rx;

import org.junit.jupiter.api.Test;
import rx.core.Observable;
import rx.disposable.Disposable;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservableTest {

    @Test
    void onNext_emitsAllItems() {
        List<Integer> result = new ArrayList<>();

        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        }).subscribe(result::add, Throwable::printStackTrace, () -> {});

        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    void onComplete_isCalled() {
        AtomicBoolean completed = new AtomicBoolean(false);

        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onComplete();
        }).subscribe(item -> {}, err -> {}, () -> completed.set(true));

        assertTrue(completed.get());
    }

    @Test
    void onError_isCalled_whenSourceThrows() {
        AtomicReference<Throwable> caught = new AtomicReference<>();

        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            throw new RuntimeException("source error");
        }).subscribe(item -> {}, caught::set, () -> {});

        assertNotNull(caught.get());
        assertEquals("source error", caught.get().getMessage());
    }

    @Test
    void onError_isCalled_viaEmitterOnError() {
        AtomicReference<Throwable> caught = new AtomicReference<>();

        Observable.<Integer>create(emitter -> {
            emitter.onError(new IllegalStateException("explicit error"));
        }).subscribe(item -> {}, caught::set, () -> {});

        assertNotNull(caught.get());
        assertInstanceOf(IllegalStateException.class, caught.get());
    }

    @Test
    void dispose_preventsSubsequentEmissions() {
        List<Integer> result = new ArrayList<>();

        Observable<Integer> source = Observable.create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
        });

        Disposable disposable = source.subscribe(item -> {
            result.add(item);
        }, err -> {}, () -> {});

        disposable.dispose();
        assertTrue(disposable.isDisposed());
    }

    @Test
    void dispose_isDisposed_returnsTrueAfterDispose() {
        Disposable disposable = Observable.<Integer>create(emitter -> {})
                .subscribe(item -> {}, err -> {}, () -> {});

        assertFalse(disposable.isDisposed());
        disposable.dispose();
        assertTrue(disposable.isDisposed());
    }

    @Test
    void map_transformsItems() {
        List<String> result = new ArrayList<>();

        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onNext(2);
            emitter.onNext(3);
            emitter.onComplete();
        }).map(i -> "item-" + i)
          .subscribe(result::add, err -> {}, () -> {});

        assertEquals(List.of("item-1", "item-2", "item-3"), result);
    }

    @Test
    void map_forwardsError_whenMapperThrows() {
        AtomicReference<Throwable> caught = new AtomicReference<>();

        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onComplete();
        }).map(i -> { throw new RuntimeException("map error"); })
          .subscribe(item -> {}, caught::set, () -> {});

        assertNotNull(caught.get());
        assertEquals("map error", caught.get().getMessage());
    }

    @Test
    void filter_keepsMatchingItems() {
        List<Integer> result = new ArrayList<>();

        Observable.<Integer>create(emitter -> {
            for (int i = 1; i <= 6; i++) emitter.onNext(i);
            emitter.onComplete();
        }).filter(i -> i % 2 == 0)
          .subscribe(result::add, err -> {}, () -> {});

        assertEquals(List.of(2, 4, 6), result);
    }

    @Test
    void filter_forwardsError_whenPredicateThrows() {
        AtomicReference<Throwable> caught = new AtomicReference<>();

        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onComplete();
        }).filter(i -> { throw new RuntimeException("predicate error"); })
          .subscribe(item -> {}, caught::set, () -> {});

        assertNotNull(caught.get());
        assertEquals("predicate error", caught.get().getMessage());
    }

    @Test
    void flatMap_forwardsError_fromInnerObservable() {
        AtomicReference<Throwable> caught = new AtomicReference<>();

        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onComplete();
        }).flatMap(i -> Observable.<Integer>create(inner -> {
            throw new RuntimeException("inner error");
        })).subscribe(item -> {}, caught::set, () -> {});

        assertNotNull(caught.get());
        assertEquals("inner error", caught.get().getMessage());
    }

    @Test
    void mapAndFilter_chainedTogether() {
        List<String> result = new ArrayList<>();

        Observable.<Integer>create(emitter -> {
            for (int i = 1; i <= 5; i++) emitter.onNext(i);
            emitter.onComplete();
        }).filter(i -> i % 2 != 0)
          .map(i -> "odd-" + i)
          .subscribe(result::add, err -> {}, () -> {});

        assertEquals(List.of("odd-1", "odd-3", "odd-5"), result);
    }

    @Test
    void subscribeOn_runsSourceOnDifferentThread() throws InterruptedException {
        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable.<Integer>create(emitter -> {
            threadName.set(Thread.currentThread().getName());
            emitter.onNext(1);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
          .subscribe(
              item -> {},
              err  -> latch.countDown(),
              latch::countDown);

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(threadName.get().startsWith("io-"));
    }

    @Test
    void subscribeOn_computation_runsOnComputationThread() throws InterruptedException {
        AtomicReference<String> threadName = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable.<Integer>create(emitter -> {
            threadName.set(Thread.currentThread().getName());
            emitter.onNext(1);
            emitter.onComplete();
        }).subscribeOn(Schedulers.computation())
          .subscribe(item -> {}, err -> latch.countDown(), latch::countDown);

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(threadName.get().startsWith("computation-"));
    }

    @Test
    void observeOn_deliversItemsOnSpecifiedThread() throws InterruptedException {
        AtomicReference<String> observeThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable.<Integer>create(emitter -> {
            emitter.onNext(42);
            emitter.onComplete();
        }).observeOn(Schedulers.single())
          .subscribe(
              item -> observeThread.set(Thread.currentThread().getName()),
              err  -> latch.countDown(),
              latch::countDown);

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("single", observeThread.get());
    }

    @Test
    void subscribeOn_and_observeOn_useDifferentThreads() throws InterruptedException {
        AtomicReference<String> subscribeThread = new AtomicReference<>();
        AtomicReference<String> observeThread   = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable.<Integer>create(emitter -> {
            subscribeThread.set(Thread.currentThread().getName());
            emitter.onNext(1);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
          .observeOn(Schedulers.single())
          .subscribe(
              item -> observeThread.set(Thread.currentThread().getName()),
              err  -> latch.countDown(),
              latch::countDown);

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(subscribeThread.get().startsWith("io-"));
        assertEquals("single", observeThread.get());
        assertNotEquals(subscribeThread.get(), observeThread.get());
    }

    @Test
    void error_propagatesThroughMapAndFilter() {
        AtomicReference<Throwable> caught = new AtomicReference<>();

        Observable.<Integer>create(emitter -> {
            emitter.onNext(1);
            emitter.onError(new RuntimeException("chain error"));
        }).map(i -> i * 2)
          .filter(i -> i > 0)
          .subscribe(item -> {}, caught::set, () -> {});

        assertNotNull(caught.get());
        assertEquals("chain error", caught.get().getMessage());
    }
}
