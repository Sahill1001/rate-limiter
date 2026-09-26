package com.ratelimiter;

import com.ratelimiter.service.RateLimiterService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

/** Executes the application's actual Lua script against a real Redis instance. */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_TESTS", matches = "true")
class TokenBucketRedisTests {
    private LettuceConnectionFactory connection;
    private StringRedisTemplate redis;
    private DefaultRedisScript<Long> script;
    private String key;

    @BeforeEach void connect() {
        connection = new LettuceConnectionFactory("localhost", 6379);
        connection.afterPropertiesSet();
        redis = new StringRedisTemplate(connection);
        script = new DefaultRedisScript<>(
                (String) ReflectionTestUtils.getField(RateLimiterService.class, "TOKEN_BUCKET_SCRIPT"), Long.class);
        key = "test:rate_limit:" + UUID.randomUUID();
    }

    @AfterEach void cleanup() {
        try { redis.delete(List.of(key, key + ":other")); }
        finally { connection.destroy(); }
    }

    private long request(String clientKey, long now) {
        return redis.execute(script, List.of(clientKey), "10", "10", "60", Long.toString(now));
    }

    @Test void allowsCapacityThenRejects() {
        for (int i = 0; i < 10; i++) assertEquals(1, request(key, 1000));
        assertEquals(0, request(key, 1000));
    }

    @Test void refillsAtTheWindowBoundary() {
        for (int i = 0; i < 10; i++) request(key, 1000);
        assertEquals(0, request(key, 1059));
        assertEquals(1, request(key, 1060));
    }

    @Test void refillNeverExceedsCapacity() {
        request(key, 1000);
        for (int i = 0; i < 10; i++) assertEquals(1, request(key, 1600));
        assertEquals(0, request(key, 1600));
    }

    @Test void clientsHaveIndependentBuckets() {
        for (int i = 0; i < 10; i++) request(key, 1000);
        assertEquals(0, request(key, 1000));
        assertEquals(1, request(key + ":other", 1000));
    }

    @Test void concurrentRequestsCannotSpendMoreThanCapacity() throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        try {
            var tasks = IntStream.range(0, 40)
                    .mapToObj(i -> (Callable<Long>) () -> request(key, 1000)).toList();
            long accepted = 0;
            for (var result : executor.invokeAll(tasks)) accepted += result.get();
            assertEquals(10, accepted);
        } finally { executor.shutdownNow(); }
    }
}
