/**
 * 
 */
package com.github.phantomthief.util;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Get data by cursor
 * 
 * @author w.vela
 */
public interface GetByCursorDAO<Id, Entity> {

    /**
     * retrieve limited data starting by cursor(inclusive)
     */
    List<Entity> getByCursor(@Nullable Id cursor, int limit);
}
