package dk.dbc.dataio.jobstore.types;

/**
 * Wire envelope for the {@code GET /sinks/{sinkId}/watermarks} response, distinguishing
 * "no watermark exists for this record" ({@code watermark == null}) from the response
 * itself being absent. Shared between job-store-service (marshalling) and
 * job-store-service-connector (unmarshalling) so the two sides cannot drift apart.
 */
public record WatermarkResponse(Watermark watermark) {
}
