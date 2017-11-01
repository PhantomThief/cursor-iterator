package com.github.phantomthieft.test;

import static com.github.phantomthief.util.CursorIterator.newBuilder;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import com.github.phantomthief.util.CursorIterator;

/**
 * @author w.vela
 */
class CursorIteratorTest {

    private final Logger logger = getLogger(getClass());

    @Test
    void test() {
        UserDAO userDAO = new UserDAO();
        Integer startId = 100;
        int countPerFetch = 10;
        CursorIterator<Integer, User> users = newBuilder() //
                .start(startId) //
                .bufferSize(countPerFetch) //
                .cursorExtractor(User::getId) //
                .build(userDAO::getUsersAscById);

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
    void testIterateTwice() {
        UserDAO userDAO = new UserDAO();
        Integer startId = 100;
        int countPerFetch = 10;
        CursorIterator<Integer, User> users = newBuilder() //
                .start(startId) //
                .bufferSize(countPerFetch) //
                .cursorExtractor(User::getId) //
                .build(userDAO::getUsersAscById);
        iterateOnce(users);
        iterateOnce(users);
    }

    @Test
    void testPageThreshold() {
        UserDAO userDAO = new UserDAO();
        Integer startId = 100;
        int countPerFetch = 10;
        CursorIterator<Integer, User> users = newBuilder() //
                .start(startId) //
                .bufferSize(countPerFetch) //
                .cursorExtractor(User::getId) //
                .maxNumberOfPages(3).build(userDAO::getUsersAscById);
        int i = 100;
        for (User user : users) {
            assertEquals(i++, user.getId());
        }
        assertEquals(130, i);
    }

    private void iterateOnce(CursorIterator<Integer, User> users) {
        int i = 100;
        for (User user : users) {
            assertEquals(i++, user.getId());
        }
        assertEquals(938, i);
    }

    @Test
    void testBuilder() {
        UserDAO userDAO = new UserDAO();
        Integer startId = 100;
        int countPerFetch = 10;
        CursorIterator<Integer, User> users = newBuilder() //
                .start(startId) //
                .cursorExtractor(User::getId) //
                .bufferSize(countPerFetch) //
                .build(userDAO::getUsersAscById);

        List<User> collect = users.stream() //
                .filter(user -> user.getId() % 11 == 0) //
                .limit(5) //
                .collect(toList());
        collect.forEach(u -> logger.info("user:{}", u));
    }

    @Test
    void testGenericBuilder() {
        UserDAO userDAO = new UserDAO();
        Integer startId = 100;
        int countPerFetch = 10;
        CursorIterator<Integer, User> users = CursorIterator.<Integer, User> newGenericBuilder() //
                .start(startId) //
                .cursorExtractor(User::getId) //
                .bufferSize(countPerFetch) //
                .build(userDAO::getUsersAscById);

        List<User> collect = users.stream() //
                .filter(user -> user.getId() % 11 == 0) //
                .limit(5) //
                .collect(toList());
        collect.forEach(u -> logger.info("user:{}", u));
    }
}
