package dk.dbc.dataio.jobstore.types;

public record Watermark(int jobId, int chunkId, short itemId) {
}
