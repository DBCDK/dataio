package dk.dbc.dataio.gui.client.pages.javascriptlog;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class JavaScriptLogPlace extends Place {
    private Long failedItemId;

    public JavaScriptLogPlace(String url) {
        this.failedItemId = Long.valueOf(url);
    }

    public Long getFailedItemId() {
        return failedItemId;
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
