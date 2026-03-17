package com.ratelimiter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    @Value("${rate-limiter.capacity}")
    private int capacity;

    @Value("${rate-limiter.refill-tokens}")
    private int refillTokens;

    @Value("${rate-limiter.refill-duration}")
    private int refillDuration;

    public RateLimiterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setScriptText(TOKEN_BUCKET_SCRIPT);
        this.rateLimitScript.setResultType(Long.class);
    }

    private static final String TOKEN_BUCKET_SCRIPT = """
                local key = KEYS[1]
                local capacity = tonumber(ARGV[1])
                local refill_tokens = tonumber(ARGV[2])
                local refill_duration = tonumber(ARGV[3])
                local now = tonumber(ARGV[4])
                
                local tokens = tonumber(redis.call('hget', key, 'tokens'))
                local last_refill = tonumber(redis.call('hget', key, 'last_refill'))
                
                if tokens == nil then
                    tokens = capacity
                end
                if last_refill == nil then
                    last_refill = now
                end

                local windows_passed = math.floor((now - last_refill) / refill_duration)
                
                if windows_passed > 0 then
                    tokens = math.min(capacity, tokens + (windows_passed * refill_tokens))
                    last_refill = last_refill + (windows_passed * refill_duration)
                end
                
                if tokens >= 1 then
                    tokens = tokens - 1
                    redis.call('hset', key, 'tokens', tokens, 'last_refill', last_refill)
                    redis.call('expire', key, refill_duration * 2)
                    return 1
                else
                    redis.call('hset', key, 'tokens', tokens, 'last_refill', last_refill)
                    redis.call('expire', key, refill_duration * 2)
                    return 0
                end 
            """;

    public boolean isAllowed(String clientId) {
        String key = "rate_limit:" + clientId;
        long now = Instant.now().getEpochSecond();

        Long result = redisTemplate.execute(
                this.rateLimitScript,
                List.of(key),
                String.valueOf(capacity),
                String.valueOf(refillTokens),
                String.valueOf(refillDuration),
                String.valueOf(now)
        );

        boolean allowed = result != null && result == 1L;
        log.info("Client: {} | Allowed: {}", clientId, allowed);
        return allowed;
    }
}