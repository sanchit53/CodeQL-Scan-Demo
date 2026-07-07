package com.example.monolith.domain;

import java.time.Clock;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Small domain service used as dummy Java code for CodeQL scanning examples.
 */
public final class GreetingService {
    private final Clock clock;

    public GreetingService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public String greetingFor(String userName) {
        String safeName = sanitizeName(userName);
        LocalTime now = LocalTime.now(clock);

        if (now.isBefore(LocalTime.NOON)) {
            return "Good morning, " + safeName + "!";
        }

        if (now.isBefore(LocalTime.of(18, 0))) {
            return "Good afternoon, " + safeName + "!";
        }

        return "Good evening, " + safeName + "!";
    }

    private static String sanitizeName(String userName) {
        if (userName == null || userName.isBlank()) {
            return "friend";
        }

        return userName.trim().replaceAll("[^A-Za-z0-9 .'-]", "");
    }
}
