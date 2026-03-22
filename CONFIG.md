# Configuration

## Application Properties

Configure rate limiting in `src/main/resources/application.yaml`:

```yaml
rate-limiter:
  capacity: 10          # Max tokens per bucket
  refill-tokens: 10     # Tokens added per refill
  refill-duration: 60   # Refill window in seconds
```

## Properties Table

| Property | Default | Description |
|----------|---------|-------------|
| `capacity` | 10 | Maximum tokens a bucket can hold |
| `refill-tokens` | 10 | Tokens added each refill cycle |
| `refill-duration` | 60 | Seconds between refills |

## Redis Configuration

Redis connection is configured in `RedisConfig.java`. Default connects to `localhost:6379`.