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
 * 游标迭代器增强版
 * <p>提供一种迭代器机制，可以连续地遍历瀑布流式的数据接口，相比{@link CursorIterator}，此类允许定制游标处理时的细节</p>
 * <p>例如某个接口允许从指定的ID起始读n条记录，并返回下一页的起始ID：</p>
 * <pre>{@code
 * interface UserRepository {
 *     ScanResult<User> scan(Integer startId, int limit);
 * }
 * class ScanResult {
 *     private final List<User> users;
 *     private final Integer nextCursor;
 *     // getters/setters...
 * }
 * }</pre>
 * 使用CursorIteratorEx可以很简洁地实现遍历过程：
 * <pre>{@code
 * Integer startId = 100;
 * int countPerFetch = 10;
 * CursorIteratorEx<User, Integer, ScanResult> users = newBuilder()
 *     .withDataRetriever((Integer cursor) -> repository.scan(cursor, countPerFetch))
 *     .withCursorExtractor(ScanResult::getNextCursor)
 *     .withDataExtractor((ScanResult s) -> s.getUsers().iterator())
 *     .withInitCursor(startId)
 *     .build();
 * }</pre>
 *
 * @param <T> 返回实体的类型泛型
 * @param <C> ID类型泛型
 * @param <R> 列表读取结果对象的泛型
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

    /**
     * 创建游标迭代器构造器对象
     *
     * @return 游标迭代器构造器对象
     */
    @CheckReturnValue
    @Nonnull
    public static Builder<Object, Object, Object> newBuilder() {
        return new Builder<>();
    }

    /**
     * 获取迭代器
     *
     * @return 返回迭代器对象
     */
    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return new RollingIterator();
    }

    /**
     * 获取Stream
     *
     * @return 返回一个Stream对象
     */
    public Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(),
                (Spliterator.NONNULL | Spliterator.IMMUTABLE)), false);
    }

    /**
     * 游标迭代器构造器
     *
     * @param <T> 返回实体的类型泛型
     * @param <C> ID类型泛型
     * @param <R> 列表读取结果对象的泛型
     */
    @SuppressWarnings("unchecked")
    public static final class Builder<T, C, R> {

        private C initCursor;
        private boolean checkFirstCursor;
        private Function<C, R> dataRetriever;
        private Function<R, C> cursorExtractor;
        private Function<R, Iterator<T>> dataExtractor;
        private Predicate<C> endChecker;

        /**
         * 设置起始ID，此ID对应的记录将作为迭代器返回的第一条数据对象
         *
         * @param initCursor 起始ID
         * @param <C1> ID类型泛型
         * @return 当前构造器对象
         */
        @CheckReturnValue
        @Nonnull
        public <C1> Builder<?, C1, ?> withInitCursor(C1 initCursor) {
            Builder<?, C1, ?> thisBuilder = (Builder<?, C1, ?>) this;
            thisBuilder.initCursor = initCursor;
            return thisBuilder;
        }

        /**
         * 设置是否对首个传入的游标进行终末检查
         *
         * @param check 是否对首个传入的游标进行终末检查，默认不检查
         * @return 当前构造器对象
         */
        @CheckReturnValue
        @Nonnull
        public Builder<T, C, R> firstCursorCheckEnd(boolean check) {
            this.checkFirstCursor = check;
            return this;
        }

        /**
         * 数据读取函数
         *
         * @param dataRetriever 数据读取函数，传入当前起始的ID，返回查询结果对象
         * @param <C1> ID类型泛型
         * @param <R1> 查询结果泛型
         * @return 当前构造器对象
         */
        @CheckReturnValue
        @Nonnull
        public <C1, R1> Builder<?, C1, R1> withDataRetriever(Function<C1, R1> dataRetriever) {
            Builder<?, C1, R1> thisBuilder = (Builder<?, C1, R1>) this;
            thisBuilder.dataRetriever = dataRetriever;
            return thisBuilder;
        }

        /**
         * 获取下一条游标函数
         *
         * @param cursorExtractor 获取下一条游标函数，传入当前获得的查询结果对象，返回下一条游标ID对象
         * @param <C1> ID类型泛型
         * @param <R1> 查询结果泛型
         * @return 当前构造器对象
         */
        @CheckReturnValue
        @Nonnull
        public <C1, R1> Builder<?, C1, R1> withCursorExtractor(Function<R1, C1> cursorExtractor) {
            Builder<?, C1, R1> thisBuilder = (Builder<?, C1, R1>) this;
            thisBuilder.cursorExtractor = cursorExtractor;
            return thisBuilder;
        }

        /**
         * 数据提取器函数
         *
         * @param dataExtractor 数据提取器函数，传入当前获得的查询结果对象，返回结果实体集合的迭代器对象
         * @param <T1> 实体类型泛型
         * @param <R1> 查询结果泛型
         * @return 当前构造器对象
         */
        @CheckReturnValue
        @Nonnull
        public <T1, R1> Builder<T1, ?, R1>
        withDataExtractor(Function<R1, Iterator<T1>> dataExtractor) {
            Builder<T1, ?, R1> thisBuilder = (Builder<T1, ?, R1>) this;
            thisBuilder.dataExtractor = dataExtractor;
            return thisBuilder;
        }

        /**
         * 设置游标终末检查器
         *
         * @param endChecker 游标终末检查器，传入当前的游标值，判断是否还有下一条记录存在，有返回true，否则返回false。
         * 默认根据游标是否为null来进行判断，不为空证明有下一条记录
         * @param <C1> ID类型泛型
         * @return 当前构造器对象
         */
        @CheckReturnValue
        @Nonnull
        public <C1> Builder<?, C1, ?> withEndChecker(Predicate<C1> endChecker) {
            Builder<?, C1, ?> thisBuilder = (Builder<?, C1, ?>) this;
            thisBuilder.endChecker = endChecker;
            return thisBuilder;
        }

        /**
         * 构造游标迭代器
         *
         * @param <T1> 返回实体的类型泛型
         * @param <C1> ID类型泛型
         * @param <R1> 列表读取结果对象的泛型
         * @return 游标迭代器对象
         */
        @SuppressWarnings("rawtypes")
        @Nonnull
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
                throw new NullPointerException("data retriever is null.");
            }
            if (cursorExtractor == null) {
                throw new NullPointerException("data retriever is null.");
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
