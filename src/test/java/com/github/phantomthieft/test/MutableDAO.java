package com.github.phantomthieft.test;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

/**
 * @author w.vela
 * Created on 2018-12-04.
 */
class MutableDAO {

    private final List<User> userList;

    MutableDAO(int allUserCount) {
        this.userList = new ArrayList<>(allUserCount);
        for (int i = 1; i <= allUserCount; i++) {
            userList.add(new User(i));
        }
    }

    List<User> getByCursor(int startId, int limit) {
        return userList.stream() //
                .filter(user -> user.getId() >= startId) //
                .limit(limit) //
                .collect(toList());
    }

    boolean deleteUser(int userId) {
        return userList.removeIf(user -> user.getId() == userId);
    }
}
