## 1. Clarify Requirement

### 1.1 Functional Requirement

- Who are the users? What actions can they do?
- Core features (explicitly state what is out of scope)
- Read heavy or write heavy?
- Real-time or batch/async?

### 1.2 Non-Functional Requirement

- Scale (Daily Active Users, requests/second)
- Availability (99.9%)
- Latency (< 100ms or < 1s)
- Consistency (Strong vs Eventual)
- Durability

## 2. Capacity Estimation

- Users (DAU, concurrent users)
- Throughput (reads/second, writes/second, peak (2x - 3x))
- Storage (data per event * events/day * retention)
- Bandwidth (inbound (write), outbound (read))

```
writes/sec    = DAU × writes/user/day ÷ 86,400
reads/sec     = DAU × reads/user/day  ÷ 86,400

storage/day   = writes/sec × avg_size × 86,400
storage/year  = storage/day × 365

bandwidth in  = writes/sec × avg_payload_size
bandwidth out = reads/sec  × avg_payload_size
```

| Result              | Implication                                          |
|---------------------|------------------------------------------------------|
| > 10K writes/sec    | Single DB won't work — sharding or NoSQL needed      |
| > 1M reads/sec      | Caching is mandatory                                 |
| > 1 TB/day storage  | Cold storage, tiering, archival policy needed        |
| Large media         | Object store (S3) + CDN, never store in DB           |
| High fan-out        | Queue + fan-out workers needed                       |

## 3. Core APIs

- Define 3-5 core APIs for the design
- Mention the protocol REST vs WebSocket vs gRPC vs SSE vs GraphQL (with reason)
- Mention endpoint with HTTP method, key request/response fields, status codes
- Mention idempotency keys and cursor-based pagination (if required)

| Protocol   | Use When                                                    |
|------------|-------------------------------------------------------------|
| REST       | Standard CRUD, stateless, client-initiated                  |
| WebSocket  | Bidirectional real-time (chat, live updates)                |
| gRPC       | Internal service-to-service (typed, efficient)              |
| SSE        | Server pushes to client one-way (feeds, notifications)      |
| GraphQL    | Client needs flexible queries over complex relational data  |

## 4. High Level Design

### 4.1 Components

- Client
- Load Balancer
- API Gateway
- App Servers
- Cache (Redis)
- Database
- Message Queue
- Object Store (S3)
- CDN
- Search Engine
- Notification Service (APNs / FCM)

### 4.2 Key Decisions

**Stateless vs Stateful**
- Stateless — any server handles any request (REST). Scale freely, round-robin LB.
- Stateful — client is tied to one server via open connection (WebSocket). Needs sticky routing (consistent hash) + a routing layer to track which server each client is on.

**Sync vs Async**
- Sync  — user waits for response (reads, simple creates). Use REST.
- Async — processing happens in background (transcode, email, fan-out). Use queue + worker.

## 5. Database Design

### 5.1 Database Schema

- Justify DB choice (read/write heavy, time-series, relational, trade-offs)
- Schema (Primary key, partition key, clustering key, indexes)
- Query patterns (design schema around access patterns, not entities)

| Database      | Use When                                                                    |
|---------------|-----------------------------------------------------------------------------|
| PostgreSQL    | Read heavy, relational data, complex queries, ACID                          |
| Cassandra     | Write heavy, time-series, simple lookup, massive scale                      |
| MongoDB       | Flexible schema, nested/hierarchical data, document store, horizontal scale |
| Redis         | Cache, sessions, leaderboards, TTL-based data, pub-sub                      |
| Elasticsearch | Full-text search, log analytics                                             |
| S3            | Large files, blobs, backups                                                 |


**PostgreSQL scale thresholds**

| QPS                | Verdict                                                               |
|--------------------|-----------------------------------------------------------------------|
| Writes < 5K/sec    | Comfortable single node                                               |
| Writes 5K–10K/sec  | Manageable with connection pooling (PgBouncer) + vertical scale       |
| Writes > 10K/sec   | Single node ceiling; shard (CitusDB) or switch to Cassandra / MongoDB |
| Reads < 50K/sec    | Fine with read replicas + index tuning                                |
| Reads 50K–100K/sec | Read replicas mandatory; Redis cache layer in front                   |
| Reads > 100K/sec   | PostgreSQL is not the right choice; NoSQL or cache-first              |

**PgBouncer** sits between app servers and PostgreSQL, multiplexing thousands of app connections down to a small fixed pool of real DB connections — eliminating connection overhead without changing query throughput.

## 6. Deep Dive

### 6.1 Scalability

- **Caching** — what to cache, TTL, eviction policy (LRU/LFU), cache-aside vs write-through
- **DB Sharding** — shard key must distribute load evenly; bad key causes hot partition
- **Read Replicas** — offload reads, but lag introduces eventual consistency (not for read-your-own-writes)
- **Hotspot / Hot Partition** — one shard gets disproportionate traffic (e.g. a very popular item/user). Fix: hash-based sharding, add random suffix to key.
- **Cache Stampede** — cache expires, many requests hit DB at once. Fix: mutex lock, staggered TTL, probabilistic early refresh.

| Cache              | Use When                                                                                                            |
|--------------------|---------------------------------------------------------------------------------------------------------------------|
| Redis              | Default choice — sessions, leaderboards, pub-sub, distributed locks, TTL-based data, rate limiting                  |
| Memcache           | Only when you need pure simple key-value caching with no other features                                             |
| Apache Ignite      | Dataset too large for a single Redis node — distributed cache across a cluster, supports SQL queries on cached data |
| CDN                | Static assets, images, videos — cache at edge near the user, offload bandwidth from app servers                     |

### 6.2 Async & Queue Design

Use a queue when:
- Processing is too slow for a synchronous response
- Need to decouple producer from consumer
- Need fan-out (1 event → multiple consumers)
- Need to absorb traffic bursts without dropping requests

| Queue       | Use When                                                      |
|-------------|---------------------------------------------------------------|
| Kafka       | High throughput, durable, ordered per partition, replayable   |
| SQS         | Simple task queue, managed, no strict ordering needed         |
| Redis       | Lightweight, already in stack, lower durability               |

- **Delivery guarantee** — at-least-once (retries on failure) vs at-most-once (fire and forget)
- **Ordering guarantee** — Kafka guarantees order per partition; SQS does not
- **Retention** — Kafka keeps messages for days (replayable); SQS deletes on consume
- **Exactly-once** is hard — use at-least-once delivery + idempotency key at consumer for effective exactly-once

### 6.3 Reliability & Failure Handling

| Failure              | Pattern                                                              |
|----------------------|----------------------------------------------------------------------|
| Server crash         | Retry with exponential backoff + jitter; route to another instance  |
| Message loss         | Persist before deliver; reconciliation job requeues stuck records   |
| Duplicate on retry   | Idempotency key at the consumer deduplicates                        |
| Cascading failure    | Circuit breaker — stop calling a failing downstream service         |
| Data loss            | Replication factor 3, quorum writes (W=2, R=2)                     |
| Network timeout      | Timeouts + fallback (cached result, degraded response)              |

## 7. Wrap Up

- **Bottlenecks** — what is the weakest point and how would you fix it?
- **Trade-offs** — "I chose X over Y, accepting the cost of Z"
  - e.g. Eventual consistency over strong consistency for higher write throughput
- **Rate limiting** — per user, per IP, token bucket or leaky bucket algorithm
- **Monitoring & Alerts** — key metrics (latency p99, error rate, queue depth, cache hit rate)
