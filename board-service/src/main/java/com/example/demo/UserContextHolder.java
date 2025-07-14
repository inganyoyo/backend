package com.example.demo;

import org.springframework.stereotype.Component;

@Component
public class UserContextHolder {
    private static final ThreadLocal<UserContext> contextHolder = new ThreadLocal<>();

    public static void setContext(UserContext context) {
        contextHolder.set(context);
    }

    public static UserContext getContext() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }

    public static String getCurrentUserId() {
        UserContext context = getContext();
        return context != null ? context.getUserId() : null;
    }

    public static String getCurrentUserRole() {
        UserContext context = getContext();
        return context != null ? context.getRole() : null;
    }
}