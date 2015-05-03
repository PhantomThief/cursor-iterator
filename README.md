# Cursor Iterator [![Build Status](https://travis-ci.org/PhantomThief/cursor-iterator.svg)](https://travis-ci.org/PhantomThief/cursor-iterator) [![Coverage Status](https://coveralls.io/repos/PhantomThief/cursor-iterator/badge.svg?branch=master)](https://coveralls.io/r/PhantomThief/cursor-iterator?branch=master)
=======================

一个简单的适合移动端无限下拉构建数据的后端支持组件 

## Usage

```xml
<dependency>
    <groupId>com.github.phantomthief</groupId>
	<artifactId>cursor-iterator</artifactId>
    <version>1.0.0</version>
</dependency>
```

```Java
public class UserDAO {

    private static final int MAX_USER_ID = 938;

    // A fake DAO for test
    public List<User> getUsersAscById(Integer startId, int limit) {
        if (startId == null) {
            startId = 0;
        }
        List<User> result = IntStream.range(startId, Math.min(startId + limit, MAX_USER_ID))
                .mapToObj(User::new).collect(Collectors.toList());
        System.out.println("get users asc by id, startId:" + startId + ", limit:" + limit
                + ", result:" + result);
        return result;
    }
}

public void test() {
    UserDAO userDAO = new UserDAO();
    Integer startId = 100;
    int countPerFetch = 10;
    CursorIterator<Integer, User> users = new CursorIterator<>(userDAO::getUsersAscById,
            startId, countPerFetch, User::getId);

    List<User> finalResult = new ArrayList<>();
    for (User user : users) {
        if (user.getId() % 11 == 0) { // filter
            System.out.println("add to final result:" + user);
            finalResult.add(user);
        } else {
            System.out.println("ignore add user:" + user);
        }
        if (finalResult.size() == 50) {
            break;
        }
    }
}
```