package com.github.phantomthief.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.google.common.collect.AbstractIterator;

/**
 * 按页从数据库里面取.
 * PageScroller 是无状态的, 所有状态全放在 iterator 中.
 *
 * @author lixian
 */
class PageScroller<Id, Entity> implements Iterable<List<Entity>> {

    static final boolean MODE_START_EXCLUSIVE = true;
    static final boolean MODE_END_EXCLUSIVE = false;

    private final GetByCursorDAO<Id, Entity> dao;
    private final Id initCursor;
    private final int bufferSize;
    private final Function<Entity, Id> entityIdFunction;
    private int maxNumberOfPages = Integer.MAX_VALUE;
    private final boolean mode;

    PageScroller(GetByCursorDAO<Id, Entity> dao, Id initCursor, int bufferSize,
            Function<Entity, Id> entityIdFunction, boolean mode) {
        this.dao = dao;
        this.initCursor = initCursor;
        this.bufferSize = bufferSize;
        this.entityIdFunction = entityIdFunction;
        this.mode = mode;
    }

    /**
     * 由于 dao 实现中, start 是被包含的, 使用上一次 cursor 取的时候希望去除 start, 所以还需要多取一个
     */
    private static <Id, Entity> List<Entity>
            fetchOnePageExcludeStart(GetByCursorDAO<Id, Entity> dao, Id start, int limit) {
        List<Entity> entities = dao.getByCursor(start, limit + 1);
        return entities.isEmpty() ? entities : entities.subList(1, entities.size());
    }

    public void setMaxNumberOfPages(int maxNumberOfPages) {
        this.maxNumberOfPages = maxNumberOfPages;
    }

    @Nonnull
    @Override
    public Iterator<List<Entity>> iterator() {
        if (mode == MODE_START_EXCLUSIVE) {
            return new AbstractIterator<List<Entity>>() {

                private List<Entity> previousPage;
                private boolean firstTime = true;
                private int pageIndex = 0;

                @Override
                protected List<Entity> computeNext() {
                    List<Entity> page;
                    if (firstTime) {
                        firstTime = false;
                        // 第一次, 正常取
                        page = dao.getByCursor(initCursor, bufferSize);
                    } else {
                        if (pageIndex >= maxNumberOfPages) {
                            // 已经取到限制的页数了
                            page = Collections.emptyList();
                        } else if (previousPage.size() < bufferSize) {
                            // 上页还不满, fail fast
                            page = Collections.emptyList();
                        } else {
                            Id start = entityIdFunction
                                    .apply(previousPage.get(previousPage.size() - 1));
                            page = fetchOnePageExcludeStart(dao, start, bufferSize);
                        }
                    }

                    previousPage = page;
                    pageIndex++;
                    return page.isEmpty() ? endOfData() : page;
                }
            };
        } else {
            return new AbstractIterator<List<Entity>>() {
                
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
                    List<Entity> list = dao.getByCursor(cursor, bufferSize + 1);
                    if (list.isEmpty()) {
                        return endOfData();
                    }
                    if (list.size() >= bufferSize + 1) {
                        cursor = entityIdFunction.apply(list.get(bufferSize));
                        return list.subList(0, bufferSize);
                    } else {
                        noNext = true;
                        return list;
                    }
                }
            };
        }
    }
}
