package com.github.phantomthief.util;

import static java.util.Collections.emptyList;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntSupplier;

import javax.annotation.Nonnull;

import com.google.common.collect.AbstractIterator;

/**
 * 按页从数据库里面取.
 * PageScroller 是无状态的, 所有状态全放在 iterator 中.
 *
 * @author lixian
 */
class PageScroller<Id, Entity> implements Iterable<List<Entity>> {

    static final boolean MODE_TRIM_FIRST = true;
    static final boolean MODE_TRIM_LAST = false;

    private final GetByCursorDAO<Id, Entity> dao;
    private final Id initCursor;
    private final IntSupplier bufferSize;
    private final Function<Entity, Id> entityIdFunction;
    private int maxNumberOfPages = Integer.MAX_VALUE;
    private final boolean mode;

    PageScroller(GetByCursorDAO<Id, Entity> dao, Id initCursor, IntSupplier bufferSize,
            Function<Entity, Id> entityIdFunction, boolean mode) {
        this.dao = dao;
        this.initCursor = initCursor;
        this.bufferSize = bufferSize;
        this.entityIdFunction = entityIdFunction;
        this.mode = mode;
    }

    public void setMaxNumberOfPages(int maxNumberOfPages) {
        this.maxNumberOfPages = maxNumberOfPages;
    }

    @Nonnull
    @Override
    public Iterator<List<Entity>> iterator() {
        if (mode == MODE_TRIM_FIRST) {
            return new TrimFirstIterator();
        } else {
            return new TrimLastIterator();
        }
    }

    private class TrimFirstIterator extends AbstractIterator<List<Entity>> {

        private List<Entity> previousPage;
        private boolean firstTime = true;
        private int pageIndex = 0;

        @Override
        protected List<Entity> computeNext() {
            int thisBufferSize = bufferSize.getAsInt();
            List<Entity> page;
            if (firstTime) {
                firstTime = false;
                // 第一次, 正常取
                page = dao.getByCursor(initCursor, thisBufferSize);
            } else {
                if (pageIndex >= maxNumberOfPages) {
                    // 已经取到限制的页数了
                    page = emptyList();
                } else if (previousPage.size() < thisBufferSize) {
                    // 上页还不满, fail fast
                    page = emptyList();
                } else {
                    Id start = entityIdFunction.apply(previousPage.get(previousPage.size() - 1));
                    page = fetchOnePageExcludeStart(dao, start, thisBufferSize);
                }
            }

            previousPage = page;
            pageIndex++;
            return page.isEmpty() ? endOfData() : page;
        }

        /**
         * 由于 dao 实现中, start 是被包含的, 使用上一次 cursor 取的时候希望去除 start, 所以还需要多取一个
         */
        private List<Entity> fetchOnePageExcludeStart(GetByCursorDAO<Id, Entity> dao, Id start,
                int limit) {
            List<Entity> entities = dao.getByCursor(start, limit + 1);
            return entities.isEmpty() ? entities : entities.subList(1, entities.size());
        }
    }

    private class TrimLastIterator extends AbstractIterator<List<Entity>> {

        private int pageIndex = 0;
        private Id cursor = initCursor;
        private boolean noNext = false;

        @Override
        protected List<Entity> computeNext() {
            if (noNext) {
                return endOfData();
            }
            pageIndex++;
            if (pageIndex > maxNumberOfPages) {
                return endOfData();
            }
            int thisBufferSize = bufferSize.getAsInt();
            List<Entity> list = dao.getByCursor(cursor, thisBufferSize + 1);
            if (list.isEmpty()) {
                return endOfData();
            }
            if (list.size() >= thisBufferSize + 1) {
                cursor = entityIdFunction.apply(list.get(thisBufferSize));
                return list.subList(0, thisBufferSize);
            } else {
                noNext = true;
                return list;
            }
        }
    }
}
