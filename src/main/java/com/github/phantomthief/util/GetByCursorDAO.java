package com.github.phantomthief.util;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 通过ID作为游标的列表数据访问对象接口
 *
 * @author w.vela
 */
public interface GetByCursorDAO<Id, Entity> {

    /**
     * 从指定的游标开始（包括），读取limit条记录作为列表返回
     *
     * @param cursor 起始游标，包括
     * @param limit 返回记录数
     */
    List<Entity> getByCursor(@Nullable Id cursor, int limit);
}
