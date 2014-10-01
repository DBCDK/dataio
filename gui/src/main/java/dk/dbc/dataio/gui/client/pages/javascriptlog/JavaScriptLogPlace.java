package dk.dbc.dataio.gui.client.pages.javascriptlog;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class JavaScriptLogPlace extends Place {

    private String combinedUrl;

    public JavaScriptLogPlace(String url) {
        this.combinedUrl = url;
    }
    public JavaScriptLogPlace(Long jobId, Long chunkId, Long failedItemId) {
        this(combineToUrl(jobId, chunkId, failedItemId));
    }

    public Long getJobId() {
        return Long.valueOf(combinedUrl.split(":")[0]);
    }

    public Long getChunkId() {
        return Long.valueOf(combinedUrl.split(":")[1]);
    }

    public Long getFailedItemId() {
        return Long.valueOf(combinedUrl.split(":")[2]);
    }

    private static String combineToUrl(Long jobId, Long chunkId, Long failedItemId) {
        return jobId + ":" + chunkId + ":" + failedItemId;
    }

    @Prefix("JavaScriptLog")
    public static class Tokenizer implements PlaceTokenizer<JavaScriptLogPlace> {
        @Override
        public String getToken(JavaScriptLogPlace place) {
            return String.valueOf(place.getFailedItemId());
        }
        @Override
        public JavaScriptLogPlace getPlace(String token) {
            return new JavaScriptLogPlace(token);
        }
    }
}
