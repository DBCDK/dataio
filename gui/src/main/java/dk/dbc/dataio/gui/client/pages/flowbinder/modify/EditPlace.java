package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.types.FlowBinderContentViewData;

public class EditPlace extends Place {
    private Long flowBinderId;

    public EditPlace(String url) {
        this.flowBinderId = Long.valueOf(url);
    }

    public EditPlace(FlowBinderContentViewData flowBinderContentViewData) {
        this.flowBinderId = flowBinderContentViewData.getId();
    }

    public Long getFlowId() {
        return flowBinderId;
    }

    @Prefix("EditFlowBinder")
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