/**
 * 
 */
package com.github.phantomthief.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * 
 * @author w.vela
 */
public class CursorIterator<Id, Entity> implements Iterable<Entity> {

    private final GetByCursorDAO<Id, Entity> dao;

    private Id initCursor;

    private final int limit;

    private boolean firstTime;

    private final Function<Entity, Id> function;

    private boolean end;

    /**
     * @param dao 游标方式取的DAO
     * @param initCursor 第一次的游标位置（包含）
     * @param limit 每次游标迭代的条数
     * @param extractor 游标和实体数据的转换器
     */
    public CursorIterator(GetByCursorDAO<Id, Entity> dao, Id initCursor, int limit,
            Function<Entity, Id> extractor) {
        if ((limit <= 0) || (dao == null) || (extractor == null)) {
            throw new IllegalArgumentException();
        }
        this.dao = dao;
        this.initCursor = initCursor;
        this.limit = limit;
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
            innerData = dao.getByCursor(initCursor, firstTime ? limit : limit + 1);
            if ((innerData.size() > 0) && !firstTime) {
                innerData.remove(0);
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
        if (byCurosr.size() < limit) {
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

}
