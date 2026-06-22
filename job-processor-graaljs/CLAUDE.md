# job-processor-graaljs

GraalVM JavaScript-based chunk processor. See `README.md` for environment variables, message
format, and health/metrics endpoints.

Integration tests spin up a real Artemis container and the service container via Testcontainers.
The Docker image must exist before running ITs — if you only changed test code, run
`mvn -pl job-processor-graaljs package -DskipITs` first to produce `target/docker.out`.

## Key classes

| Class | Role |
|-------|------|
| `ChunkConsumerBean` | `@Singleton @Startup` — reads env config, starts consumer threads, schedules timeout checks |
| `ChunkMessageConsumer` | Handles one JMS `TextMessage`; delegates to `ChunkProcessor`, reports result to job-store via HTTP |
| `ChunkProcessor` | Per-chunk orchestration; throws on flow-fetch failure so the JMS transaction rolls back and retries |
| `FlowCache` | Guava cache of compiled `GraalJsScript` instances keyed by `flowId.flowVersion` |
| `GraalJsScript` | Wraps a GraalVM JS context with the flow's JSAR unpacked |
| `ProcessorHealth` | MicroProfile `@Liveness` — latches to `down` on fatal errors and never resets (requires pod restart) |

## Flow resolution

1. `flowId` and `flowVersion` are read from JMS headers on each incoming message.
2. `ChunkProcessor` looks up `flowId.flowVersion` in `FlowCache`.
3. On cache miss, the `Flow` is fetched from the job-store (`getCachedFlow`). If the flow has no
   embedded JSAR and `FLOWSTORE_URL` is configured, the JSAR is fetched separately from the
   flow-store.
4. A `GraalJsScript` is compiled from the JSAR and cached until TTL expiry or eviction.

## Error handling

- Flow fetch failure → `RuntimeException` propagates out of `onMessage` → JMS transaction
  rolls back → message is redelivered.
- Chunk processing timeout (> 3 min) → `ProcessorHealth.signalFatal` → liveness probe fails →
  pod restarts.
- `OutOfMemoryError` → `signalOutOfMemory` → same restart path.
- JS `IgnoreRecord` → item output `IGNORED`; `FailRecord` or other exception → item output
  `FAILURE` with diagnostics attached.
