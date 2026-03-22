# Design Document

## Overview
This distributed rate limiter implements the Token Bucket algorithm using Spring Boot and Redis with atomic Lua scripts for consistency across multiple nodes.

## Algorithm - Token Bucket
- **Capacity**: Maximum tokens per bucket (default: 10)
- **Refill**: Tokens added at fixed intervals (default: 10 tokens every 60 seconds)
- **Consumption**: Each request consumes 1 token
- **Rejection**: Requests blocked when tokens = 0 (HTTP 429)

## Key Design Decisions
- **Redis over Database**: Sub-millisecond in-memory operations for high throughput
- **Lua Scripts**: Atomic execution prevents race conditions between read/write operations
- **Token Bucket vs Fixed Window**: Prevents burst issues by continuous token tracking
- **IP-based Limiting**: Stateless, no authentication required (use API keys for production)

## Redis Storage
Each client has a Redis hash:
- Key: `rate_limit:<client_ip>`
- Fields: `tokens` (current count), `last_refill` (timestamp)
- Auto-expiration: `refill_duration * 2` seconds of inactivity

## Architecture
- **Controller**: HTTP layer, extracts client IP
- **Service**: Executes Lua script on Redis
- **Config**: Redis connection setup