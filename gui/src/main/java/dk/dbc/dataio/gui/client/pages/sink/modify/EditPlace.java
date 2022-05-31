package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class EditPlace extends AbstractBasePlace {
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

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterEditImpl(this, commonInjector.getMenuTexts().menu_SinkEdit());
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
