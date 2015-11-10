/**
 * 
 */
package com.github.phantomthief.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
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

    /**
     * @param dao 游标方式取的DAO
     * @param initCursor 第一次的游标位置（包含）
     * @param bufferSize 每次游标迭代的条数
     * @param extractor 游标和实体数据的转换器
     */
    public CursorIterator(GetByCursorDAO<Id, Entity> dao, Id initCursor, int bufferSize,
            Function<Entity, Id> extractor) {
        if ((bufferSize <= 0) || (dao == null) || (extractor == null)) {
            throw new IllegalArgumentException();
        }
        this.dao = dao;
        this.initCursor = initCursor;
        this.bufferSize = bufferSize;
        this.firstTime = true;
        this.function = extractor;
        this.end = false;
    }

    private List<Entity> innerData;

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
     * 
     * @return
     */
    private Iterator<Entity> innerIterator() {
        return innerData().iterator();
    }

    /**
     * 下一个迭代时拿上一次游标的位置
     * 
     * @return
     */
    private Id getLastCursor() {
        List<Entity> byCursor = innerData();
        if (byCursor == null || byCursor.isEmpty()) {
            return null;
        }
        Id result = function.apply(byCursor.get(byCursor.size() - 1));
        return result;
    }

    /**
     * 判断是否到结尾的标记
     * 
     * @return
     */
    private boolean hasMore() {
        List<Entity> byCurosr = innerData();
        if (byCurosr.size() < bufferSize) {
            return false;
        } else {
            return true;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
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
                        return hasNext;
                    } else {
                        initCursor = getLastCursor();
                        firstTime = false;
                        end = !hasMore();
                        innerData = null;
                        itr = null;
                        return itr().hasNext();
                    }
                }
                return hasNext;
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
                Spliterators.spliteratorUnknownSize(iterator(),
                        (Spliterator.NONNULL | Spliterator.IMMUTABLE | Spliterator.ORDERED)),
                false);
    }

    @SuppressWarnings("unchecked")
    public static final class Builder<Id, Entity> {

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
            if (dao == null) {
                throw new NullPointerException("dao is null");
            }
            if (function == null) {
                throw new NullPointerException("cursor extractor is null");
            }
            if (bufferSize <= 0) {
                bufferSize = DEFAULT_BUFFER_SIZE;
            }
        }
    }

    public static Builder<Object, Object> newBuilder() {
        return new Builder<>();
    }
}
