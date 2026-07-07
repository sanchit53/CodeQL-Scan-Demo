package com.example.monolith.app;

import com.example.monolith.domain.GreetingService;
import java.time.Clock;

/**
 * Minimal runnable entry point for the dummy multi-module Java project.
 */
public final class DemoApplication {
    private DemoApplication() {
    }

    public static void main(String[] args) {
        String userName = args.length > 0 ? args[0] : "CodeQL reviewer";
        GreetingService greetingService = new GreetingService(Clock.systemUTC());
        System.out.println(greetingService.greetingFor(userName));
    }
}
