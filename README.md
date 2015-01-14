# Cursor Iterator
=======================

A cursor iterator implements for batch build cursor data 

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