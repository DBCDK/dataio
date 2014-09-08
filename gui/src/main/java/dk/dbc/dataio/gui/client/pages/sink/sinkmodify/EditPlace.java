package dk.dbc.dataio.gui.client.pages.sink.sinkmodify;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.commons.types.Sink;

public class EditPlace extends Place {
    private Long sinkId;

    public EditPlace(String url) {
        this.sinkId = Long.valueOf(url);
    }

    public EditPlace(Sink sink) {
        this.sinkId = sink.getId();
    }

    public Long getSinkId() {
        return sinkId;
    }

    @Prefix("EditSink")
    public static class Tokenizer implements PlaceTokenizer<EditPlace> {
        @Override
        public String getToken(EditPlace place) {
            return String.valueOf(place.getSinkId());
        }
        @Override
        public EditPlace getPlace(String token) {
            return new EditPlace(token);
        }
    }
}
