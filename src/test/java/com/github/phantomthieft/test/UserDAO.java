/**
 * 
 */
package com.github.phantomthieft.test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author w.vela
 */
public class UserDAO {

    private static final int MAX_USER_ID = 938;

    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    // A fake DAO for test
    public List<User> getUsersAscById(Integer startId, int limit) {
        if (startId == null) {
            startId = 0;
        }
        List<User> result = IntStream.range(startId, Math.min(startId + limit, MAX_USER_ID))
                .mapToObj(User::new).collect(Collectors.toList());
        logger.trace("get users asc by id, startId:" + startId + ", limit:" + limit + ", result:"
                + result);
        return result;
    }
}
