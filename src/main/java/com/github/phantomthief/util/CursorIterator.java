package com.github.phantomthief.util;

import static com.github.phantomthief.util.PageScroller.MODE_TRIM_FIRST;
import static com.github.phantomthief.util.PageScroller.MODE_TRIM_LAST;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import com.google.common.collect.AbstractIterator;

/**
 * @author w.vela
 */
public class CursorIterator<Id, Entity> implements Iterable<Entity> {

    private static final int DEFAULT_BUFFER_SIZE = 30;
    private final PageScroller<Id, Entity> pageScroller;

    private CursorIterator(PageScroller<Id, Entity> pageScroller) {
        this.pageScroller = pageScroller;
    }

    @CheckReturnValue
    @Nonnull
    public static <I, E> GenericBuilder<I, E> newGenericBuilder() {
        return new GenericBuilder<>(newBuilder());
    }

    /**
     * better use {@link #newGenericBuilder()} for type safe
     */
    @Deprecated
    @CheckReturnValue
    @Nonnull
    public static Builder<Object, Object> newBuilder() {
        return new Builder<>();
    }

    @Nonnull
    @Override
    public Iterator<Entity> iterator() {
        return new AbstractIterator<Entity>() {

            private final Iterator<List<Entity>> pageIterator = pageScroller.iterator();
            private Iterator<Entity> entityIteratorInPage;

            @Override
            protected Entity computeNext() {
                if (entityIteratorInPage == null || !entityIteratorInPage.hasNext()) {
                    if (pageIterator.hasNext()) {
                        entityIteratorInPage = pageIterator.next().iterator();
                    } else {
                        return endOfData();
                    }
                }
                return entityIteratorInPage.next();
            }
        };
    }

    public Stream<Entity> stream() {
        return StreamSupport
                .stream(spliteratorUnknownSize(iterator(), (NONNULL | IMMUTABLE | ORDERED)), false);
    }

    public static class GenericBuilder<Id, Entity> {

        private final Builder<Object, Object> builder;

        private GenericBuilder(Builder<Object, Object> builder) {
            this.builder = builder;
        }

        /**
         * 使用 {@link #buildEx} 代替，后者在迭代中删除是友好的
         */
        @Deprecated
        @Nonnull
        public CursorIterator<Id, Entity> build(GetByCursorDAO<? super Id, ? extends Entity> dao) {
            return builder.build(dao);
        }

        @Nonnull
        public CursorIterator<Id, Entity> buildEx(GetByCursorDAO<? super Id, ? extends Entity> dao) {
            return builder.buildEx(dao);
        }

        @CheckReturnValue
        @Nonnull
        public GenericBuilder<Id, Entity>
                cursorExtractor(Function<? super Entity, ? extends Id> function) {
            builder.cursorExtractor(function);
            return this;
        }

        @CheckReturnValue
        @Nonnull
        public GenericBuilder<Id, Entity> start(Id init) {
            builder.start(init);
            return this;
        }

        @CheckReturnValue
        @Nonnull
        public GenericBuilder<Id, Entity> bufferSize(int bufferSize) {
            builder.bufferSize(bufferSize);
            return this;
        }

        @CheckReturnValue
        @Nonnull
        public GenericBuilder<Id, Entity> bufferSize(IntSupplier bufferSize) {
            builder.bufferSize(bufferSize);
            return this;
        }

        @CheckReturnValue
        @Nonnull
        public GenericBuilder<Id, Entity> maxNumberOfPages(int maxNumberOfPages) {
            builder.maxNumberOfPages(maxNumberOfPages);
            return this;
        }
    }

    /**
     * 使用 {@link #newGenericBuilder()} 代替
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public static class Builder<Id, Entity> {

        private GetByCursorDAO<Id, Entity> dao;
        private IntSupplier bufferSize;
        private Function<Entity, Id> function;
        private Id init;
        private int maxNumberOfPages = 0;
        private boolean mode = MODE_TRIM_FIRST;

        /**
         * 使用 {@link #buildEx} 代替，后者在迭代中删除是友好的
         */
        @Deprecated
        @Nonnull
        public <I, E> CursorIterator<I, E> build(GetByCursorDAO<? super I, ? extends E> dao) {
            Builder<I, E> thisBuilder = (Builder<I, E>) this;
            thisBuilder.dao = (GetByCursorDAO<I, E>) dao;
            return thisBuilder.build();
        }

        @Nonnull
        public <I, E> CursorIterator<I, E> buildEx(GetByCursorDAO<? super I, ? extends E> dao) {
            this.mode = MODE_TRIM_LAST;
            return build(dao);
        }

        @CheckReturnValue
        @Nonnull
        public Builder<Id, Entity> bufferSize(int bufferSize) {
            checkArgument(bufferSize > 0);
            return bufferSize(() -> bufferSize);
        }

        @CheckReturnValue
        @Nonnull
        public Builder<Id, Entity> bufferSize(@Nonnull IntSupplier bufferSize) {
            this.bufferSize = checkNotNull(bufferSize);
            return this;
        }

        @CheckReturnValue
        @Nonnull
        public <I, E> Builder<I, E> cursorExtractor(Function<? super E, ? extends I> function) {
            Builder<I, E> thisBuilder = (Builder<I, E>) this;
            thisBuilder.function = (Function<E, I>) function;
            return thisBuilder;
        }

        @CheckReturnValue
        @Nonnull
        public <I, E> Builder<I, E> start(I init) {
            Builder<I, E> thisBuilder = (Builder<I, E>) this;
            thisBuilder.init = init;
            return thisBuilder;
        }

        @CheckReturnValue
        @Nonnull
        public <I, E> Builder<I, E> maxNumberOfPages(int maxNumberOfPages) {
            Builder<I, E> thisBuilder = (Builder<I, E>) this;
            thisBuilder.maxNumberOfPages = maxNumberOfPages;
            return thisBuilder;
        }

        private CursorIterator<Id, Entity> build() {
            ensure();
            PageScroller<Id, Entity> scroller = new PageScroller<>(dao, init, bufferSize, function,
                    mode);
            if (maxNumberOfPages > 0) {
                scroller.setMaxNumberOfPages(maxNumberOfPages);
            }
            return new CursorIterator<>(scroller);
        }

        private void ensure() {
            checkNotNull(dao);
            checkNotNull(function);

            if (bufferSize == null) {
                bufferSize = () -> DEFAULT_BUFFER_SIZE;
            }
        }
    }
}
