package rx.disposable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A basic {@link Disposable} backed by an {@link AtomicBoolean}.
 */
public class SimpleDisposable implements Disposable {

    private final AtomicBoolean disposed = new AtomicBoolean(false);

    @Override
    public void dispose() {
        disposed.set(true);
    }

    @Override
    public boolean isDisposed() {
        return disposed.get();
    }
}
