# API Documentation

## Rate Limit Check

### Endpoint
```
GET /api/request
```

### Headers
- `X-Forwarded-For`: Client IP address (optional)

### Responses
- **200 OK**: Request accepted
  ```
  Request accepted. Welcome!
  ```

- **429 Too Many Requests**: Rate limit exceeded
  ```
  Rate limit exceeded. Try again in 60 seconds.
  ```
  Headers:
  - `Retry-After: 60`