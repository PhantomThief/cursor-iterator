/**
 * 
 */
package com.github.phantomthieft.test;

/**
 * @author w.vela
 */
public class User {

    private final int id;

    public User(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "User [id=" + id + "]";
    }

}
