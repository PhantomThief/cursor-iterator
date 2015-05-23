/**
 * 
 */
package com.github.phantomthieft.test;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.github.phantomthief.util.CursorIteratorEx;
import com.github.phantomthieft.test.UserDAO.ScanResult;

/**
 * @author w.vela
 */
public class CursorIteratorExTest {

    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    @Test
    public void test() {
        UserDAO userDAO = new UserDAO();
        Integer startId = 100;
        int countPerFetch = 10;
        CursorIteratorEx<User, Integer, ScanResult> users = CursorIteratorEx
                .<User, Integer, ScanResult> newBuilder() //
                .withDataRetriever(cursor -> userDAO.scan(cursor, countPerFetch)) //
                .withCursorExtractor(ScanResult::getNextCursor) //
                .withDataExtractor(s -> s.getUsers().iterator()) //
                .withInitCursor(startId) //
                .build();

        List<User> collect = users.stream() //
                .filter(user -> user.getId() % 11 == 0) //
                .limit(5) //
                .collect(Collectors.toList());
        collect.forEach(u -> logger.info("user:{}", u));
    }
}
