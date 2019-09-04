package com.github.phantomthieft.test;

import static com.github.phantomthief.util.CursorIterator.newBuilder;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
        CursorIterator<Integer, User> users = newBuilder()
                .start(startId)
                .bufferSize(countPerFetch)
                .cursorExtractor(User::getId)
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
        CursorIterator<Integer, User> users = newBuilder()
                .start(startId)
                .bufferSize(countPerFetch)
                .cursorExtractor(User::getId)
                .build(userDAO::getUsersAscById);
        iterateOnce(users);
        iterateOnce(users);
    }

    @Test
    void testPageThreshold() {
        UserDAO userDAO = new UserDAO();
        Integer startId = 100;
        int countPerFetch = 10;
        CursorIterator<Integer, User> users = newBuilder()
                .start(startId)
                .bufferSize(countPerFetch)
                .cursorExtractor(User::getId)
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
        CursorIterator<Integer, User> users = newBuilder()
                .start(startId)
                .cursorExtractor(User::getId)
                .bufferSize(countPerFetch)
                .build(userDAO::getUsersAscById);

        List<User> collect = users.stream()
                .filter(user -> user.getId() % 11 == 0)
                .limit(5)
                .collect(toList());
        collect.forEach(u -> logger.info("user:{}", u));
    }

    @Test
    void testGenericBuilder() {
        UserDAO userDAO = new UserDAO();
        Integer startId = 100;
        int countPerFetch = 10;
        CursorIterator<Integer, User> users = CursorIterator.<Integer, User> newGenericBuilder()
                .start(startId)
                .cursorExtractor(User::getId)
                .bufferSize(countPerFetch)
                .build(userDAO::getUsersAscById);

        List<User> collect = users.stream()
                .filter(user -> user.getId() % 11 == 0)
                .limit(5)
                .collect(toList());
        collect.forEach(u -> logger.info("user:{}", u));
    }

    @Test
    void testDeleteWhileIterator() {
        testDeleting(1005);
        testDeleting(1000);
        testNoDeleting(1005);
        testNoDeleting(1000);
    }

    @Test
    void testDynamicBufferSize() {
        UserDAO userDAO = new UserDAO();
        int[] daoCount = {0};
        CursorIterator<Integer, User> users = newBuilder()
                .start(null)
                .cursorExtractor(User::getId)
                .bufferSize(() -> ThreadLocalRandom.current().nextInt(1, 10))
                .buildEx((startId, limit) -> {
                    daoCount[0]++;
                    return userDAO.getUsersAscById(startId, limit);
                });
        List<User> result = users.stream()
                .limit(200)
                .collect(toList());
        assertEquals(200, result.size());
        for (int i = 0; i < 200; i++) {
            assertEquals(i, result.get(i).getId());
        }
        assertTrue(daoCount[0] > 20);
    }

    private void testDeleting(int allSize) {
        MutableDAO dao = new MutableDAO(allSize);
        CursorIterator<Integer, User> cursor = CursorIterator.<Integer, User> newGenericBuilder()
                .start(0)
                .cursorExtractor(User::getId)
                .bufferSize(10)
                .buildEx(dao::getByCursor);
        List<User> users = new ArrayList<>();
        for (User user : cursor) {
            users.add(user);
            assertTrue(dao.deleteUser(user.getId()));
        }
        for (int i = 1; i <= allSize; i++) {
            assertEquals(new User(i), users.get(i - 1));
        }
        assertEquals(allSize, users.size());
        assertEquals(0, cursor.stream().count());
    }

    private void testNoDeleting(int allSize) {
        MutableDAO dao = new MutableDAO(allSize);
        CursorIterator<Integer, User> cursor = CursorIterator.<Integer, User> newGenericBuilder()
                .start(0)
                .cursorExtractor(User::getId)
                .bufferSize(10)
                .buildEx(dao::getByCursor);
        List<User> users = new ArrayList<>();
        for (User user : cursor) {
            users.add(user);
        }
        for (int i = 1; i <= allSize; i++) {
            assertEquals(new User(i), users.get(i - 1));
        }
        assertEquals(allSize, users.size());
    }

    @Test
    void testPageSize() {
        int allSize = 1005;
        MutableDAO dao = new MutableDAO(allSize);
        int bufferSize = 10;
        int maxNumberOfPages = 3;
        CursorIterator<Integer, User> cursor = CursorIterator.<Integer, User> newGenericBuilder()
                .start(0)
                .cursorExtractor(User::getId)
                .bufferSize(bufferSize)
                .maxNumberOfPages(maxNumberOfPages)
                .buildEx(dao::getByCursor);
        List<User> users = new ArrayList<>();
        for (User user : cursor) {
            users.add(user);
            assertTrue(dao.deleteUser(user.getId()));
        }
        int read = bufferSize * maxNumberOfPages;
        assertEquals(read, users.size());
        for (int i = 1; i <= read; i++) {
            assertEquals(new User(i), users.get(i - 1));
        }
    }
}
