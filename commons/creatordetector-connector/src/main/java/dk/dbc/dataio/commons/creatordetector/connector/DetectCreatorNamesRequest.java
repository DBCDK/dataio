package dk.dbc.dataio.commons.creatordetector.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request object for the creator detector connector.
 * @param query The text content to be analyzed for creator detection.
 * @param remoteId Remote ID used for logging and debugging purposes, not used in the actual detection process.
 */
public record DetectCreatorNamesRequest(String query, @JsonProperty("infomedia_id") String remoteId) {
    public DetectCreatorNamesRequest(String query, String remoteId) {
        this.query = query;
        this.remoteId = remoteId;
    }
}
