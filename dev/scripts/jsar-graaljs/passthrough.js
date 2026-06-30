// Passthrough script: returns the input record unchanged.
// Called by both job-processor2 (Nashorn) and job-processor-graaljs (GraalJS).
// Signature: process(data: String, supplement: Object) → String

// Logger name must start with "dk.dbc.js." so the GraalJS LogCollector captures it
// and the processor persists it to the log-store (see LogStoreWriter).
const LOGGER = Java.type("org.slf4j.LoggerFactory").getLogger("dk.dbc.js.passthrough");

export function process(data, supplement) {
  LOGGER.info("passthrough: received {} bytes", data.length);
  LOGGER.debug("passthrough: supplement = {}", JSON.stringify(supplement));
  LOGGER.info("passthrough: returning record unchanged");
  return data;
}
