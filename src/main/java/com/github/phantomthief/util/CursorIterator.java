/**
 * 
 */
package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 
 * @author w.vela
 */
public class CursorIterator<Id, Entity> implements Iterable<Entity> {

    private static final int DEFAULT_BUFFER_SIZE = 30;

    private final GetByCursorDAO<Id, Entity> dao;
    private final int bufferSize;
    private final Function<Entity, Id> function;

    private Id initCursor;
    private boolean firstTime;
    private boolean end;
    private List<Entity> innerData;

    /**
     * @param dao 游标方式取的DAO
     * @param initCursor 第一次的游标位置（包含）
     * @param bufferSize 每次游标迭代的条数
     * @param extractor 游标和实体数据的转换器
     */
    private CursorIterator(GetByCursorDAO<Id, Entity> dao, Id initCursor, int bufferSize,
            Function<Entity, Id> extractor) {
        this.dao = dao;
        this.initCursor = initCursor;
        this.bufferSize = bufferSize;
        this.firstTime = true;
        this.function = extractor;
        this.end = false;
    }

    public static <I, E> GenericBuilder<I, E> newGenericBuilder() {
        return new GenericBuilder<>(newBuilder());
    }

    public static Builder<Object, Object> newBuilder() {
        return new Builder<>();
    }

    private List<Entity> innerData() {
        if (end) {
            return Collections.emptyList();
        }
        if (innerData == null) {
            innerData = dao.getByCursor(initCursor, firstTime ? bufferSize : bufferSize + 1);
            if ((innerData.size() > 0) && !firstTime) {
                innerData = innerData.subList(1, innerData.size());
            }
        }
        return innerData;
    }

    /**
     * 取到内部迭代器
     */
    private Iterator<Entity> innerIterator() {
        return innerData().iterator();
    }

    /**
     * 下一个迭代时拿上一次游标的位置
     */
    private Id getLastCursor() {
        List<Entity> byCursor = innerData();
        if (byCursor == null || byCursor.isEmpty()) {
            return null;
        }
        return function.apply(byCursor.get(byCursor.size() - 1));
    }

    /**
     * 判断是否到结尾的标记
     */
    private boolean hasMore() {
        List<Entity> byCurosr = innerData();
        return byCurosr.size() >= bufferSize;
    }

    @Override
    public Iterator<Entity> iterator() {
        /**
         * 这个匿名类主要做lazy的用途，可以在迭代的过程中，不会把所有的数据和迭代器都生成，只生成最近两个的
         */
        return new Iterator<Entity>() {

            private Iterator<Entity> itr;

            private Iterator<Entity> itr() {
                if (itr == null) {
                    itr = innerIterator();
                }
                return itr;
            }

            @Override
            public boolean hasNext() {
                boolean hasNext = itr().hasNext();
                if (!hasNext) {
                    if (end) {
                        return false;
                    } else {
                        initCursor = getLastCursor();
                        firstTime = false;
                        end = !hasMore();
                        innerData = null;
                        itr = null;
                        return itr().hasNext();
                    }
                }
                return true;
            }

            @Override
            public Entity next() {
                return itr().next();
            }

            @Override
            public void remove() {
                itr().remove();
            }
        };
    }

    public Stream<Entity> stream() {
        return StreamSupport.stream(
                spliteratorUnknownSize(iterator(), (NONNULL | IMMUTABLE | ORDERED)), false);
    }

    public static class GenericBuilder<Id, Entity> {

        private final Builder<Object, Object> builder;

        private GenericBuilder(Builder<Object, Object> builder) {
            this.builder = builder;
        }

        public CursorIterator<Id, Entity> build(GetByCursorDAO<? super Id, ? extends Entity> dao) {
            return builder.build(dao);
        }

        public GenericBuilder<Id, Entity> cursorExtractor(
                Function<? super Entity, ? extends Id> function) {
            builder.cursorExtractor(function);
            return this;
        }

        public GenericBuilder<Id, Entity> start(Id init) {
            builder.start(init);
            return this;
        }

        public GenericBuilder<Id, Entity> bufferSize(int bufferSize) {
            builder.bufferSize(bufferSize);
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    public static class Builder<Id, Entity> {

        private GetByCursorDAO<Id, Entity> dao;
        private int bufferSize;
        private Function<Entity, Id> function;
        private Id init;

        public <I, E> CursorIterator<I, E> build(GetByCursorDAO<? super I, ? extends E> dao) {
            Builder<I, E> thisBuilder = (Builder<I, E>) this;
            thisBuilder.dao = (GetByCursorDAO<I, E>) dao;
            return thisBuilder.build();
        }

        public Builder<Id, Entity> bufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public <I, E> Builder<I, E> cursorExtractor(Function<? super E, ? extends I> function) {
            Builder<I, E> thisBuilder = (Builder<I, E>) this;
            thisBuilder.function = (Function<E, I>) function;
            return thisBuilder;
        }

        public <I, E> Builder<I, E> start(I init) {
            Builder<I, E> thisBuilder = (Builder<I, E>) this;
            thisBuilder.init = init;
            return thisBuilder;
        }

        private CursorIterator<Id, Entity> build() {
            ensure();
            return new CursorIterator<>(dao, init, bufferSize, function);
        }

        private void ensure() {
            checkNotNull(dao);
            checkNotNull(function);

            if (bufferSize <= 0) {
                bufferSize = DEFAULT_BUFFER_SIZE;
            }
        }
    }
}
