package com.github.phantomthief.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.google.common.collect.AbstractIterator;

/**
 * 按页从数据库里面取.
 * PageScroller 是无状态的, 所有状态全放在 iterator 中.
 *
 * @author lixian
 */
public class PageScroller<Id, Entity> implements Iterable<List<Entity>> {

    private final GetByCursorDAO<Id, Entity> dao;
    private final Id initCursor;
    private final int bufferSize;
    private final Function<Entity, Id> entityIdFunction;
    private int maxNumberOfPages = Integer.MAX_VALUE;

    public PageScroller(GetByCursorDAO<Id, Entity> dao, Id initCursor, int bufferSize,
            Function<Entity, Id> entityIdFunction) {
        this.dao = dao;
        this.initCursor = initCursor;
        this.bufferSize = bufferSize;
        this.entityIdFunction = entityIdFunction;
    }

    /**
     * 由于 dao 实现中, start 可能是被包含的, 要过滤 start, 最好多取一个
     */
    private List<Entity> fetchOnePageExcludeStart(GetByCursorDAO<Id, Entity> dao, Id start,
            int limit) {
        List<Entity> entities = dao.getByCursor(start, limit + 1);
        // 如果结果中不包含 start 的话，则不去除
        boolean isHead = true;
        List<Entity> result = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            // skip head (cursor inclusive)
            if (isHead && entityIdFunction.apply(entity).equals(start)) {
                continue;
            }
            result.add(entity);
            // break if reach limit (cursor exclusive)
            if (result.size() >= limit) {
                break;
            }
            isHead = false;
        }
        return result;
    }

    public void setMaxNumberOfPages(int maxNumberOfPages) {
        this.maxNumberOfPages = maxNumberOfPages;
    }

    @Override
    public Iterator<List<Entity>> iterator() {
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
    }
}
