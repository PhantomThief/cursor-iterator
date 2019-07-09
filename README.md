# Cursor Iterator [![Build Status](https://travis-ci.org/PhantomThief/cursor-iterator.svg)](https://travis-ci.org/PhantomThief/cursor-iterator) [![Coverage Status](https://coveralls.io/repos/PhantomThief/cursor-iterator/badge.svg?branch=master)](https://coveralls.io/r/PhantomThief/cursor-iterator?branch=master)
=======================

一个简单的适合移动端无限下拉构建数据的后端支持组件 

## 使用方法

* Stable版本
```xml
<dependency>
    <groupId>com.github.phantomthief</groupId>
	<artifactId>cursor-iterator</artifactId>
    <version>1.0.11</version>
</dependency>
```

* Development版本
```xml
<dependency>
    <groupId>com.github.phantomthief</groupId>
	<artifactId>cursor-iterator</artifactId>
    <version>1.0.12-SNAPSHOT</version>
</dependency>
```

```Java
public class UserDAO {

    private static final int MAX_USER_ID = 938;

    // A fake DAO for test
    public static List<User> getUsersAscById(Integer startId, int limit) {
        if (startId == null) {
            startId = 0;
        }
        List<User> result = IntStream.range(startId, Math.min(startId + limit, MAX_USER_ID))
                .mapToObj(User::new)
                .collect(Collectors.toList());
        System.out.println("get users asc by id, startId:" + startId + ", limit:" + limit
                + ", result:" + result);
        return result;
    }
}

// 声明
CursorIterator<Integer, User> users = CursorIterator.newBuilder()
        .start(startId)
        .cursorExtractor(User::getId)
        .bufferSize(countPerFetch)
        .build(UserDAO::getUsersAscById);

// jdk1.8 Stream方式
List<User> collect = users.stream()
		.filter(user -> user.getId() % 11 == 0)
		.limit(5)
        .collect(Collectors.toList());
        
// 传统迭代器模式
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
```

## 注意事项

* GetByCursorDAO返回的元素不能有null，因为如果结尾的元素是null，CursorIterator将无法根据null计算下一次迭代滑动窗口时的起始位置