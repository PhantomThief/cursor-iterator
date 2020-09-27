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
 * 游标迭代器
 * <p>提供一种迭代器机制，可以连续地遍历瀑布流式的数据接口</p>
 * <p>例如某个接口允许从指定的ID起始读n条记录返回：</p>
 * <pre>{@code
 * interface UserRepository {
 *     List<User> findAllUsers(Integer startId, int limit);
 * }
 * }</pre>
 * 如果要遍历列表的全部，通常可以设置一个游标ID，并多次调用findAllUsers来完成，例如：
 * <pre>{@code
 * void foreachUser() {
 *     Integer batchSize = 100;
 *     Integer cursorId = 1;
 *     while (null != cursorId) {
 *         List<User> users = findAllUsers(cursorId, batchSize + 1);
 *         (users.size() > batchSize ? users.subList(0, batchSize) : users).forEach(user -> {
 *             // 访问单个用户
 *         });
 *         if (users.size() >= batchSize + 1) {
 *            cursorId = users.get(batchSize).getId();
 *         } else {
 *            cursorId = null;
 *         }
 *     }
 * }
 * }</pre>
 * CursorIterator将这一过程抽象成一个组件，可直接返回一个承载全部列表的迭代器，并在访问时延迟加载后面的分段，使用CursorIterator实现例如：
 * <pre>{@code
 * CursorIterator<Integer, User> users = CursorIterator.newGenericBuilder()
 *         .start(1)
 *         .bufferSize(100)
 *         .cursorExtractor(User::getId)
 *         .build(UserRepository::findAllUsers);
 * // 直接作为Stream访问
 * users.stream().forEach(user -> {
 *     // 访问单个用户
 * });
 * }</pre>
 *
 * @param <Id> ID类型泛型
 * @param <Entity> 实体对象泛型
 * @author w.vela
 */
public class CursorIterator<Id, Entity> implements Iterable<Entity> {

    private static final int DEFAULT_BUFFER_SIZE = 30;
    private final PageScroller<Id, Entity> pageScroller;

    private CursorIterator(PageScroller<Id, Entity> pageScroller) {
        this.pageScroller = pageScroller;
    }

    /**
     * 创建通用游标迭代器的构造器
     *
     * @param <I> ID泛型类型
     * @param <E> 返回实体对象的泛型类型
     * @return 构造器对象
     */
    @CheckReturnValue
    @Nonnull
    public static <I, E> GenericBuilder<I, E> newGenericBuilder() {
        return new GenericBuilder<>(newBuilder());
    }

    /**
     * 已废弃：请使用泛型版本{@link #newGenericBuilder()}
     */
    @Deprecated
    @CheckReturnValue
    @Nonnull
    public static Builder<Object, Object> newBuilder() {
        return new Builder<>();
    }

    /**
     * 获取迭代器
     *
     * @return 返回迭代器对象
     */
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

    /**
     * 获取Stream
     *
     * @return 返回一个Stream对象
     */
    public Stream<Entity> stream() {
        return StreamSupport
                .stream(spliteratorUnknownSize(iterator(), (NONNULL | IMMUTABLE | ORDERED)), false);
    }

    /**
     * 泛型游标迭代器构造器
     *
     * @param <Id> ID泛型类型
     * @param <Entity> 实体对象泛型类型
     */
    public static class GenericBuilder<Id, Entity> {

        private final Builder<Object, Object> builder;

        private GenericBuilder(Builder<Object, Object> builder) {
            this.builder = builder;
        }

        /**
         * 已废弃：请使用 {@link #buildEx} 代替
         * <p>警告：此方法在遍历列表删除时有缺陷，会引起跳过删除部分ID，{@link #buildEx}在迭代中删除记录是友好的</p>
         *
         * @see #buildEx
         */
        @Deprecated
        @Nonnull
        public CursorIterator<Id, Entity> build(GetByCursorDAO<? super Id, ? extends Entity> dao) {
            return builder.build(dao);
        }

        /**
         * 构造游标迭代器
         *
         * @param dao 游标数据访问对象，提供根据初始ID、读取条数读取一列列表的数据访问方法
         * @return 构造的游标迭代器对象
         */
        @Nonnull
        public CursorIterator<Id, Entity> buildEx(GetByCursorDAO<? super Id, ? extends Entity> dao) {
            return builder.buildEx(dao);
        }

        /**
         * 设置游标提取函数
         *
         * @param function 提供一个函数，从传入的实体上提取游标ID对象
         * @return 当前构造器对象
         */
        @CheckReturnValue
        @Nonnull
        public GenericBuilder<Id, Entity>
        cursorExtractor(Function<? super Entity, ? extends Id> function) {
            builder.cursorExtractor(function);
            return this;
        }

        /**
         * 设置起始ID，此ID对应的记录将作为迭代器返回的第一条数据对象
         *
         * @param init 起始ID
         * @return 当前构造器对象
         */
        @CheckReturnValue
        @Nonnull
        public GenericBuilder<Id, Entity> start(Id init) {
            builder.start(init);
            return this;
        }

        /**
         * 设置一次取列表数据返回的记录数
         *
         * @param bufferSize 一次取列表数据返回的记录数
         * @return 当前构造器对象
         */
        @CheckReturnValue
        @Nonnull
        public GenericBuilder<Id, Entity> bufferSize(int bufferSize) {
            builder.bufferSize(bufferSize);
            return this;
        }

        /**
         * 设置一次取列表数据返回的记录数的提供器
         *
         * @param bufferSize 一次取列表数据返回的记录数提供器函数
         * @return 当前构造器对象
         */
        @CheckReturnValue
        @Nonnull
        public GenericBuilder<Id, Entity> bufferSize(IntSupplier bufferSize) {
            builder.bufferSize(bufferSize);
            return this;
        }

        /**
         * 设置最多取的页数，大于等于1
         *
         * @param maxNumberOfPages 最多取的页数
         * @return 当前构造器对象
         */
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
