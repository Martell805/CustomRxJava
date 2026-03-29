package rx.disposable;

/**
 * Represents a disposable resource or subscription that can be cancelled.
 */
public interface Disposable {

    /** Disposes the resource or cancels the subscription. */
    void dispose();

    /** Returns {@code true} if this disposable has been disposed. */
    boolean isDisposed();
}
