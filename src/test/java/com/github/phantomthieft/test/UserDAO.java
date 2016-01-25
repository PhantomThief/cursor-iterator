/**
 * 
 */
package com.github.phantomthieft.test;

import static java.lang.Math.min;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;

/**
 * @author w.vela
 */
public class UserDAO {

    private static final int MAX_USER_ID = 938;

    private final Logger logger = getLogger(getClass());

    // A fake DAO for test
    public List<User> getUsersAscById(Integer startId, int limit) {
        if (startId == null) {
            startId = 0;
        }
        List<User> result = range(startId, min(startId + limit, MAX_USER_ID)).mapToObj(User::new)
                .collect(Collectors.toList());
        logger.trace("get users asc by id, startId:" + startId + ", limit:" + limit + ", result:"
                + result);
        return result;
    }

    // A fake DAO for test
    public ScanResult scan(Integer startId, int limit) {
        if (startId == null) {
            startId = 0;
        }
        List<User> result = range(startId, min(startId + limit, MAX_USER_ID)).mapToObj(User::new)
                .collect(Collectors.toList());
        logger.trace("get users asc by id, startId:" + startId + ", limit:" + limit + ", result:"
                + result);
        Integer nextCursor = startId + limit > MAX_USER_ID ? null : startId + limit;
        return new ScanResult(result, nextCursor);
    }

    public static final class ScanResult {

        private final List<User> users;
        private final Integer nextCursor;

        private ScanResult(List<User> users, Integer nextCursor) {
            this.users = users;
            this.nextCursor = nextCursor;
        }

        public List<User> getUsers() {
            return users;
        }

        public Integer getNextCursor() {
            return nextCursor;
        }

    }

}
