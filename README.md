Cursor Iterator
=======================
[![Build Status](https://travis-ci.org/PhantomThief/cursor-iterator.svg)](https://travis-ci.org/PhantomThief/cursor-iterator)
[![Coverage Status](https://coveralls.io/repos/PhantomThief/cursor-iterator/badge.svg?branch=master)](https://coveralls.io/r/PhantomThief/cursor-iterator?branch=master)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/PhantomThief/cursor-iterator.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/PhantomThief/cursor-iterator/alerts/)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/PhantomThief/cursor-iterator.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/PhantomThief/cursor-iterator/context:java)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.phantomthief/cursor-iterator)](https://search.maven.org/artifact/com.github.phantomthief/cursor-iterator/)

一个简单的适合移动端无限下拉构建数据的后端支持组件 

## 使用方法

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
CursorIterator<Integer, User> users = CursorIterator.newGenericBuilder()
        .start(startId)
        .cursorExtractor(User::getId)
        .bufferSize(countPerFetch)
        .buildEx(UserDAO::getUsersAscById);

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