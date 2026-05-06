package com.javeme.duobao.common;

import org.springframework.stereotype.Component;

@Component
public class BaseContext {

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
