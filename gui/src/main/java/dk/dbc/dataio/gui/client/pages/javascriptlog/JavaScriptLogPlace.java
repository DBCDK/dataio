package dk.dbc.dataio.gui.client.pages.javascriptlog;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class JavaScriptLogPlace extends Place {

    private static String combinedUrl;

    public JavaScriptLogPlace(String url) {
        this.combinedUrl = url;
    }
    public JavaScriptLogPlace(long jobId, long chunkId, long failedItemId) {
        this(combineToUrl(jobId, chunkId, failedItemId));
    }

   /*
    Getter methods:
    A general exception is thrown if the input is not as expected.
    The reason for this is, that a user can type directly in the url. As a result, we do not have complete
    control of the input values given to the place.
    If input is flawed and an exception is thrown, the default value returned is: 0
    A zero value will result in the gui correctly displaying the error message: "Javascript log kunne ikke hentes."
    */

    public Long getJobId() {
        try {
            return Long.valueOf(combinedUrl.split(":")[0]);
        } catch (Exception e) {
            return 0L;
        }
    }

    public Long getChunkId() {
        try {
            return Long.valueOf(combinedUrl.split(":")[1]);
        } catch (Exception e) {
            return 0L;
        }
    }

    public Long getFailedItemId() {
        try {
            return Long.valueOf(combinedUrl.split(":")[2]);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String combineToUrl(Long jobId, Long chunkId, Long failedItemId) {
        return jobId + ":" + chunkId + ":" + failedItemId;
    }

    @Prefix("JavaScriptLog")
    public static class Tokenizer implements PlaceTokenizer<JavaScriptLogPlace> {
        @Override
        public String getToken(JavaScriptLogPlace place) {
            return String.valueOf(combinedUrl);
        }
        @Override
        public JavaScriptLogPlace getPlace(String token) {
            return new JavaScriptLogPlace(token);
        }
    }
}
