/**
 * 
 */
package com.github.phantomthieft.test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.github.phantomthief.util.CursorIterator;

/**
 * @author w.vela
 */
public class CursorIteratorTest {

    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    @Test
    public void test() {
        UserDAO userDAO = new UserDAO();
        Integer startId = 100;
        int countPerFetch = 10;
        CursorIterator<Integer, User> users = new CursorIterator<>(userDAO::getUsersAscById,
                startId, countPerFetch, User::getId);

        List<User> finalResult = new ArrayList<>();
        for (User user : users) {
            if (user.getId() % 11 == 0) { // filter
                logger.info("add to final result:" + user);
                finalResult.add(user);
            } else {
                logger.info("ignore add user:" + user);
            }
            if (finalResult.size() == 50) {
                break;
            }
        }
    }

    @Test
    public void testBuilder() {
        UserDAO userDAO = new UserDAO();
        Integer startId = 100;
        int countPerFetch = 10;
        CursorIterator<Integer, User> users = CursorIterator.<Integer, User> newBuilder() //
                .withDAO(userDAO::getUsersAscById) //
                .start(startId) //
                .cursorExtractor(User::getId) //
                .limit(countPerFetch) //
                .build();

        List<User> collect = users.stream() //
                .filter(user -> user.getId() % 11 == 0) //
                .limit(5) //
                .collect(Collectors.toList());
        collect.forEach(u -> logger.info("user:{}", u));
    }
}
