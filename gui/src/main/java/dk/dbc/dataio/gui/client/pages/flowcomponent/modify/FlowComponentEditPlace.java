package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.commons.types.FlowComponent;

/**
* FlowComponentEditPlace
*
* @author sma
*/
public class FlowComponentEditPlace extends Place {
    private Long flowComponentId;

    public FlowComponentEditPlace(String url) {
        this.flowComponentId = Long.valueOf(url);
    }

    public FlowComponentEditPlace(FlowComponent flowComponent) {
        this.flowComponentId = flowComponent.getId();
    }

    public Long getFlowComponentId() {
        return flowComponentId;
    }

    @Prefix("EditFlowComponent")
    public static class Tokenizer implements PlaceTokenizer<FlowComponentEditPlace> {
        @Override
        public String getToken(FlowComponentEditPlace place) {
            return String.valueOf(place.getFlowComponentId());
        }
        @Override
        public FlowComponentEditPlace getPlace(String token) {
            return new FlowComponentEditPlace(token);
        }
    }

}

