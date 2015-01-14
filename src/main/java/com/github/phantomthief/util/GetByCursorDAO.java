/**
 * 
 */
package com.github.phantomthief.util;

import java.util.List;

/**
 * Get data by cursor
 * 
 * @author w.vela
 */
public interface GetByCursorDAO<Id, Entity> {

    /**
     * retrieve limited data starting by cursor(inclusive)
     * 
     * @param cursor
     * @param limit
     * @return
     */
    public List<Entity> getByCursor(Id cursor, int limit);

}
