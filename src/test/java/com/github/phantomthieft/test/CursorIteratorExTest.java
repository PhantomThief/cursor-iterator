package com.github.phantomthieft.test;

import static com.github.phantomthief.util.CursorIteratorEx.newBuilder;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import com.github.phantomthief.util.CursorIteratorEx;
import com.github.phantomthieft.test.UserDAO.ScanResult;

/**
 * @author w.vela
 */
class CursorIteratorExTest {

    private final Logger logger = getLogger(getClass());

    @Test
    void test() {
        UserDAO userDAO = new UserDAO();
        Integer startId = 100;
        int countPerFetch = 10;
        CursorIteratorEx<User, Integer, ScanResult> users = newBuilder() //
                .withDataRetriever((Integer cursor) -> userDAO.scan(cursor, countPerFetch)) //
                .withCursorExtractor(ScanResult::getNextCursor) //
                .withDataExtractor((ScanResult s) -> s.getUsers().iterator()) //
                .withInitCursor(startId) //
                .build();

        List<User> collect = users.stream() //
                .filter(user -> user.getId() % 11 == 0) //
                .limit(5) //
                .collect(toList());
        collect.forEach(u -> logger.info("user:{}", u));
    }
}
