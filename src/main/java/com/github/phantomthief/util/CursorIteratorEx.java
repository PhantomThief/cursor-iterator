package com.github.phantomthief.util;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * @author w.vela
 */
public class CursorIteratorEx<T, C, R> implements Iterable<T> {

    private final C initCursor;
    private final boolean checkFirstCursor;
    private final Function<C, R> dataRetriever;
    private final Function<R, C> cursorExtractor;
    private final Function<R, Iterator<T>> dataExtractor;
    private final Predicate<C> endChecker;

    private CursorIteratorEx(C initCursor, boolean checkFirstCursor, Function<C, R> dataRetriever,
            Function<R, C> cursorExtractor, Function<R, Iterator<T>> dataExtractor,
            Predicate<C> endChecker) {
        this.initCursor = initCursor;
        this.checkFirstCursor = checkFirstCursor;
        this.dataRetriever = dataRetriever;
        this.cursorExtractor = cursorExtractor;
        this.dataExtractor = dataExtractor;
        this.endChecker = endChecker;
    }

    @CheckReturnValue
    public static Builder<Object, Object, Object> newBuilder() {
        return new Builder<>();
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return new RollingIterator();
    }

    public Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(),
                (Spliterator.NONNULL | Spliterator.IMMUTABLE)), false);
    }

    @SuppressWarnings("unchecked")
    public static final class Builder<T, C, R> {

        private C initCursor;
        private boolean checkFirstCursor;
        private Function<C, R> dataRetriever;
        private Function<R, C> cursorExtractor;
        private Function<R, Iterator<T>> dataExtractor;
        private Predicate<C> endChecker;

        @CheckReturnValue
        public <C1> Builder<?, C1, ?> withInitCursor(C1 initCursor) {
            Builder<?, C1, ?> thisBuilder = (Builder<?, C1, ?>) this;
            thisBuilder.initCursor = initCursor;
            return thisBuilder;
        }

        @CheckReturnValue
        public Builder<T, C, R> firstCursorCheckEnd(boolean check) {
            this.checkFirstCursor = check;
            return this;
        }

        @CheckReturnValue
        public <C1, R1> Builder<?, C1, R1> withDataRetriever(Function<C1, R1> dataRetriever) {
            Builder<?, C1, R1> thisBuilder = (Builder<?, C1, R1>) this;
            thisBuilder.dataRetriever = dataRetriever;
            return thisBuilder;
        }

        @CheckReturnValue
        public <C1, R1> Builder<?, C1, R1> withCursorExtractor(Function<R1, C1> cursorExtractor) {
            Builder<?, C1, R1> thisBuilder = (Builder<?, C1, R1>) this;
            thisBuilder.cursorExtractor = cursorExtractor;
            return thisBuilder;
        }

        @CheckReturnValue
        public <T1, R1> Builder<T1, ?, R1>
                withDataExtractor(Function<R1, Iterator<T1>> dataExtractor) {
            Builder<T1, ?, R1> thisBuilder = (Builder<T1, ?, R1>) this;
            thisBuilder.dataExtractor = dataExtractor;
            return thisBuilder;
        }

        @CheckReturnValue
        public <C1> Builder<?, C1, ?> withEndChecker(Predicate<C1> endChecker) {
            Builder<?, C1, ?> thisBuilder = (Builder<?, C1, ?>) this;
            thisBuilder.endChecker = endChecker;
            return thisBuilder;
        }

        @SuppressWarnings("rawtypes")
        public <T1, C1, R1> CursorIteratorEx<T1, C1, R1> build() {
            ensure();
            return new CursorIteratorEx(initCursor, checkFirstCursor, dataRetriever,
                    cursorExtractor, dataExtractor, endChecker);
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

    private final class RollingIterator implements Iterator<T> {

        private C currentCursor;
        private R currentData;
        private Iterator<T> currentIterator;

        RollingIterator() {
            currentCursor = initCursor;
            if (checkFirstCursor && endChecker.test(currentCursor)) {
                return;
            }
            currentData = dataRetriever.apply(currentCursor);
            if (currentData != null) {
                currentIterator = dataExtractor.apply(currentData);
                currentCursor = cursorExtractor.apply(currentData);
            }
        }

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

        @Override
        public T next() {
            return currentIterator.next();
        }
    }
}
