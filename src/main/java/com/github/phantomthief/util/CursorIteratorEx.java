/**
 * 
 */
package com.github.phantomthief.util;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author w.vela
 */
public class CursorIteratorEx<T, C, R> implements Iterable<T> {

    private final C initCursor;
    private final Function<C, R> dataRetriever;
    private final Function<R, C> cursorExtractor;
    private final Function<R, Iterator<T>> dataExtractor;
    private final Predicate<C> endChecker;

    /**
     * @param initCursor
     * @param dataRetriever
     * @param cursorExtractor
     * @param dataExtractor
     * @param endChecker
     */
    private CursorIteratorEx(C initCursor, Function<C, R> dataRetriever,
            Function<R, C> cursorExtractor, Function<R, Iterator<T>> dataExtractor,
            Predicate<C> endChecker) {
        this.initCursor = initCursor;
        this.dataRetriever = dataRetriever;
        this.cursorExtractor = cursorExtractor;
        this.dataExtractor = dataExtractor;
        this.endChecker = endChecker;
    }

    private final class RollingIterator implements Iterator<T> {

        private C currentCursor;
        private R currentData;
        private Iterator<T> currentIterator;

        public RollingIterator() {
            currentCursor = initCursor;
            if (endChecker.test(currentCursor)) {
                return;
            }
            currentData = dataRetriever.apply(currentCursor);
            if (currentData != null) {
                currentIterator = dataExtractor.apply(currentData);
                currentCursor = cursorExtractor.apply(currentData);
            }
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            if (currentIterator == null) {
                return false;
            }
            if (currentIterator.hasNext()) {
                return true;
            }
            roll();
            return currentIterator != null && currentIterator.hasNext();
        }

        private void roll() {
            if (endChecker.test(currentCursor)) {
                currentData = null;
                currentIterator = null;
                return;
            }
            currentData = dataRetriever.apply(currentCursor);
            if (currentData == null) {
                currentIterator = null;
            } else {
                currentCursor = cursorExtractor.apply(currentData);
                currentIterator = dataExtractor.apply(currentData);
            }
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Override
        public T next() {
            return currentIterator.next();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<T> iterator() {
        return new RollingIterator();
    }

    public Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(),
                (Spliterator.NONNULL | Spliterator.IMMUTABLE)), false);
    }

    public static final class Builder<T, C, R> {

        private C initCursor;
        private Function<C, R> dataRetriever;
        private Function<R, C> cursorExtractor;
        private Function<R, Iterator<T>> dataExtractor;
        private Predicate<C> endChecker;

        public Builder<T, C, R> withInitCursor(C initCursor) {
            this.initCursor = initCursor;
            return this;
        }

        public Builder<T, C, R> withDataRetriever(Function<C, R> dataRetriever) {
            this.dataRetriever = dataRetriever;
            return this;
        }

        public Builder<T, C, R> withCursorExtractor(Function<R, C> cursorExtractor) {
            this.cursorExtractor = cursorExtractor;
            return this;
        }

        public Builder<T, C, R> withDataExtractor(Function<R, Iterator<T>> dataExtractor) {
            this.dataExtractor = dataExtractor;
            return this;
        }

        public Builder<T, C, R> withEndChecker(Predicate<C> endChecker) {
            this.endChecker = endChecker;
            return this;
        }

        public CursorIteratorEx<T, C, R> build() {
            ensure();
            return new CursorIteratorEx<>(initCursor, dataRetriever, cursorExtractor, dataExtractor,
                    endChecker);
        }

        private void ensure() {
            if (dataExtractor == null) {
                throw new NullPointerException("data extractor is null.");
            }
            if (dataRetriever == null) {
                throw new NullPointerException("data retriver is null.");
            }
            if (cursorExtractor == null) {
                throw new NullPointerException("data retriver is null.");
            }
            if (endChecker == null) {
                endChecker = Objects::isNull;
            }
        }

    }

    public static final <T, C, R> Builder<T, C, R> newBuilder() {
        return new Builder<>();
    }

}
