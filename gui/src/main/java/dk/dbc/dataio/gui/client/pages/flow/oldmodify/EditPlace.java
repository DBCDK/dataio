package dk.dbc.dataio.gui.client.pages.flow.oldmodify;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.model.FlowModel;

public class EditPlace extends Place {
    private Long flowId;

    public EditPlace(String url) {
        this.flowId = Long.valueOf(url);
    }

    public EditPlace(FlowModel model) {
        this.flowId = model.getId();
    }

    public Long getFlowId() {
        return flowId;
    }

    @Prefix("OldEditFlow")
    public static class Tokenizer implements PlaceTokenizer<EditPlace> {
        @Override
        public String getToken(EditPlace place) {
            return String.valueOf(place.getFlowId());
        }
        @Override
        public EditPlace getPlace(String token) {
            return new EditPlace(token);
        }
    }
}
