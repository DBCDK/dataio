package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.model.SinkModel;

public class EditPlace extends Place {
    private Long sinkId;

    public EditPlace(String url) {
        this.sinkId = Long.valueOf(url);
    }

    public EditPlace(SinkModel sink) {
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
