package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.commons.types.FlowComponent;

public class EditPlace extends Place {
    private Long flowComponentId;

    public EditPlace(String url) {
        this.flowComponentId = Long.valueOf(url);
    }

    public EditPlace(FlowComponent flowComponent) {
        this.flowComponentId = flowComponent.getId();
    }

    public Long getFlowComponentId() {
        return flowComponentId;
    }

    @Prefix("EditFlowComponent")
    public static class Tokenizer implements PlaceTokenizer<EditPlace> {
        @Override
        public String getToken(EditPlace place) {
            return String.valueOf(place.getFlowComponentId());
        }
        @Override
        public EditPlace getPlace(String token) {
            return new EditPlace(token);
        }
    }
}
