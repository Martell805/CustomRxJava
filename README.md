# Mini-RxJava - Отчёт

## Структура проекта

```
src/
├── main/java/
│   ├── rx/
│   │   ├── core/
│   │   │   ├── Observer.java               # Интерфейс наблюдателя
│   │   │   ├── Emitter.java                # API для источника внутри create()
│   │   │   ├── ObservableOnSubscribe.java  # Функциональный интерфейс источника
│   │   │   └── Observable.java             # Основной класс потока
│   │   ├── disposable/
│   │   │   ├── Disposable.java             # Интерфейс отмены подписки
│   │   │   └── SimpleDisposable.java       # Реализация на AtomicBoolean
│   │   └── schedulers/
│   │       ├── Scheduler.java              # Интерфейс планировщика
│   │       ├── IOThreadScheduler.java      # CachedThreadPool
│   │       ├── ComputationScheduler.java   # FixedThreadPool (N = CPU cores)
│   │       ├── SingleThreadScheduler.java  # SingleThreadExecutor
│   │       └── Schedulers.java             # Фабрика синглтонов
│   └── demo/
│       └── Main.java                       # 6 демонстрационных сценариев
└── test/java/rx/
    └── ObservableTest.java                 # 17 юнит-тестов
```

---

## 1. Архитектура системы

### Паттерн Observer

```
ObservableOnSubscribe            Observer
  (source / producer)              (consumer)
        │                               ▲
        │  subscribe(Emitter)           │
        ▼                               │
     Emitter  ──── onNext(T) ──────────►│
              ──── onError(t) ─────────►│
              ──── onComplete() ───────►│
```

`Observable` является холодным (cold): каждая подписка инициирует независимый
запуск источника. Оператор `subscribe()` запускает цепочку и возвращает `Disposable`.

### Неизменяемая цепочка операторов

Каждый оператор (`map`, `filter`, `flatMap`, `subscribeOn`, `observeOn`) создаёт
новый `Observable`, оборачивающий предыдущий. Данные не текут до вызова
`subscribe()`:

```
Observable.create(source)
    .filter(predicate)      ← новый Observable
    .map(function)          ← новый Observable
    .subscribeOn(io)        ← новый Observable
    .observeOn(single)      ← новый Observable
    .subscribe(observer)    ← запускает всю цепочку
```

---

## 2. Компоненты

### Observer

```java
public interface Observer<T> {
    void onNext(T item);
    void onError(Throwable t);
    void onComplete();
}
```

Контракт: после `onError` или `onComplete` никакие другие события не должны
поступать. Соблюдается через проверку `disposable.isDisposed()` в `Emitter`.

### Disposable

`SimpleDisposable` хранит флаг `AtomicBoolean`. При `dispose()` флаг
выставляется в `true`, и `Emitter` перестаёт проксировать события вниз.
Это защищает от гонки: если источник многопоточный, `compareAndSet` гарантирует
атомарность.

### Observable.create()

```java
Observable<Integer> source = Observable.create(emitter -> {
    emitter.onNext(1);
    emitter.onNext(2);
    emitter.onComplete();
});
```

Любое непойманное исключение внутри лямбды перехватывается в `subscribe()` и
направляется в `onError`.

---

## 3. Операторы

| Оператор            | Сложность    | Описание                                                                    |
|---------------------|--------------|-----------------------------------------------------------------------------|
| `map(Function)`     | O(1)/элемент | Синхронное преобразование каждого элемента                                  |
| `filter(Predicate)` | O(1)/элемент | Пропускает только подходящие элементы                                       |
| `flatMap(Function)` | O(inner)     | Раскрывает каждый элемент в inner Observable, конкатенирует последовательно |

flatMap в данной реализации выполняет конкатенацию (не слияние/merge):
inner Observable'ы обрабатываются один за другим в порядке поступления элементов
из источника. Для параллельного merge потребовалась бы очередь и координация потоков.

---

## 4. Schedulers - принципы работы и различия

### IOThreadScheduler (аналог `Schedulers.io()`)

- Основан на `Executors.newCachedThreadPool()`.
- Создаёт новый поток для каждой задачи, если нет свободных; простаивающие потоки
  удерживаются 60 секунд перед завершением.
- Применение: сетевые запросы, файловый ввод-вывод, операции с БД -
  всё, что блокирует поток на ожидание.
- Риск: неограниченный рост числа потоков при взрывном потоке задач.

### ComputationScheduler (аналог `Schedulers.computation()`)

- Основан на `Executors.newFixedThreadPool(N_CPU)`.
- Число потоков фиксировано равным `Runtime.getRuntime().availableProcessors()`.
- Применение: CPU-bound вычисления (парсинг, криптография, трансформации данных).
- Нельзя использовать для блокирующих операций - это заблокирует все вычислительные
  потоки и остановит обработку.

### SingleThreadScheduler (аналог `Schedulers.single()`)

- Основан на `Executors.newSingleThreadExecutor()`.
- Один поток, задачи выполняются строго последовательно.
- Применение: обновление UI (Android main thread pattern), сериализация доступа
  к разделяемому ресурсу без `synchronized`.

### subscribeOn vs observeOn

```
[source]──subscribeOn(io)──[operator chain]──observeOn(single)──[observer]
          ↑                                                       ↑
       io thread                                            single thread
```

- `subscribeOn` - переключает поток, в котором источник вызывает `emitter.onNext()`.
  Имеет смысл только один раз в цепочке (первый вызов побеждает).
- `observeOn` - переключает поток, в котором потребитель получает уведомления.
  Может применяться многократно в одной цепочке.

---

## 5. Обработка ошибок

Ошибки распространяются по тем же каналам, что и данные:

1. Исключение в источнике → перехватывается в `subscribe()` → `emitter.onError()`.
2. Исключение в `map`/`filter` → перехватывается внутри оператора → `emitter.onError()`.
3. Исключение в inner Observable `flatMap` → `emitter.onError()` внешнего потока.

После `onError` `Disposable` не отменяется автоматически, но `Emitter` проверяет
`isDisposed()` перед каждой передачей события.

---

## 6. Тестирование

### Покрытие (17 тестов)

| Группа            | Тесты                                                         |
|-------------------|---------------------------------------------------------------|
| Observer contract | `onNext`, `onComplete`, `onError` через throw, `onError` явно |
| Disposable        | `dispose()` предотвращает события; `isDisposed()`             |
| map               | преобразование; ошибка в mapper                               |
| filter            | фильтрация чётных; ошибка в predicate                         |
| flatMap           | раскрытие; ошибка во inner Observable                         |
| Chaining          | `filter` + `map` вместе                                       |
| subscribeOn       | IO thread; Computation thread                                 |
| observeOn         | Single thread; IO + Single в одной цепочке                    |
| Error propagation | сквозь `map`+`filter`; нет событий после `onError`            |

### Многопоточные тесты

Используют `CountDownLatch` для синхронизации: тест ждёт завершения асинхронной
цепочки не более 3 секунд, затем проверяет имя потока через `AtomicReference<String>`.

---

## 7. Компиляция и запуск

```bash
# Сборка и тесты (Gradle)
./gradlew test

# Запуск демо
./gradlew run
```
