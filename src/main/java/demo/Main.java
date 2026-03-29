package demo;

import rx.core.Observable;
import rx.disposable.Disposable;
import rx.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        demo1_basicStream();
        demo2_mapAndFilter();
        demo3_flatMap();
        demo4_schedulers();
        demo5_disposable();
        demo6_errorHandling();
    }

    static void demo1_basicStream() {
        System.out.println("[Demo 1] Basic stream");

        Observable.<String>create(emitter -> {
            emitter.onNext("alpha");
            emitter.onNext("beta");
            emitter.onNext("gamma");
            emitter.onComplete();
        }).subscribe(
            item -> System.out.println("  onNext: " + item),
            err  -> System.out.println("  onError: " + err.getMessage()),
            ()   -> System.out.println("  onComplete")
        );
    }

    static void demo2_mapAndFilter() {
        System.out.println("\n[Demo 2] map + filter");

        Observable.<Integer>create(emitter -> {
            for (int i = 1; i <= 8; i++) emitter.onNext(i);
            emitter.onComplete();
        }).filter(i -> i % 2 == 0)
          .map(i -> i * i)
          .subscribe(
              item -> System.out.println("  " + item),
              err  -> System.out.println("  onError: " + err.getMessage()),
              ()   -> System.out.println("  onComplete")
          );
    }

    static void demo3_flatMap() {
        System.out.println("\n[Demo 3] flatMap");

        Observable.<String>create(emitter -> {
            emitter.onNext("foo");
            emitter.onNext("bar");
            emitter.onComplete();
        }).flatMap(s -> Observable.create(inner -> {
            inner.onNext(s.toUpperCase());
            inner.onNext(s + "!");
            inner.onComplete();
        })).subscribe(
            item -> System.out.println("  " + item),
            err  -> System.out.println("  onError: " + err.getMessage()),
            ()   -> System.out.println("  onComplete")
        );
    }

    static void demo4_schedulers() throws InterruptedException {
        System.out.println("\n[Demo 4] subscribeOn + observeOn");
        CountDownLatch latch = new CountDownLatch(1);

        Observable.<Integer>create(emitter -> {
            System.out.println("  [source] thread: " + Thread.currentThread().getName());
            emitter.onNext(42);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
          .observeOn(Schedulers.single())
          .subscribe(
              item -> System.out.println("  [observer] thread: " + Thread.currentThread().getName() + ", item: " + item),
              err  -> latch.countDown(),
              latch::countDown
          );

        latch.await(3, TimeUnit.SECONDS);
    }

    static void demo5_disposable() {
        System.out.println("\n[Demo 5] Disposable - cancel mid-stream");
        final Disposable[] ref = new Disposable[1];

        ref[0] = Observable.<Integer>create(emitter -> {
            for (int i = 1; i <= 5; i++) {
                emitter.onNext(i);
            }
            emitter.onComplete();
        }).subscribe(
            item -> {
                System.out.println("  received: " + item);
                if (item == 3) {
                    ref[0].dispose();
                    System.out.println("  disposed after item 3");
                }
            },
            err -> System.out.println("  onError: " + err.getMessage()),
            ()  -> System.out.println("  onComplete")
        );
    }

    static void demo6_errorHandling() {
        System.out.println("\n[Demo 6] Error handling");

        Observable.<Integer>create(emitter -> {
            emitter.onNext(10);
            emitter.onNext(0);
            emitter.onComplete();
        }).map(i -> {
            if (i == 0) throw new ArithmeticException("division by zero");
            return 100 / i;
        }).subscribe(
            item -> System.out.println("  result: " + item),
            err  -> System.out.println("  caught error: " + err.getMessage()),
            ()   -> System.out.println("  onComplete")
        );
    }
}
