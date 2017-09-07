package com.github.phantomthieft.test;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;

import com.github.phantomthief.util.CursorIterator;

/**
 * @author lixian
 */
public class CursorExcludeTest {

    @Test
    public void test() throws Exception {
        CursorIterator<Long, Long> iterator = CursorIterator.<Long, Long> newGenericBuilder() //
                .start(100L) //
                .cursorExtractor(Function.identity()) //
                .bufferSize(10) //
                .maxNumberOfPages(10) //
                .build(this::rangeExcludeStart);
        long i = 100;
        for (Long n : iterator) {
            assertEquals(++i, (long) n);
        }
        assertEquals(200, i);
    }

    private List<Long> rangeExcludeStart(Long start, int limit) {
        return LongStream.range(start, start + limit + 1).skip(1).boxed() //
                .collect(Collectors.toList());
    }
}
