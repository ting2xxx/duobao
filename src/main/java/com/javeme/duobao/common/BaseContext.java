package com.javeme.duobao.common;

import org.springframework.stereotype.Component;

/**
 * BaseContext allows you to securely remember "who is currently logged in" anywhere in
 * your code for the duration of a single HTTP request, without crossing over between
 * different users
 */
@Component
public class BaseContext {

    //like private locker for each, every user get its own locker
    private static final ThreadLocal<Long> currentUser = new ThreadLocal<>();

    public static void setUser(Long id) {
        currentUser.set(id);
    }

    public static Long getCurrentID() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
