package dk.dbc.dataio.gui.client.pages.flow.show;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class FlowsShowPlace  extends Place {
    private String flowsShowName;

    public FlowsShowPlace() {
        this.flowsShowName = "";
    }

    public FlowsShowPlace(String flowsShowName) {
        this.flowsShowName = flowsShowName;
    }

    public String getFlowsShowName() {
        return flowsShowName;
    }

    @Prefix("ShowFlows")
    public static class Tokenizer implements PlaceTokenizer<FlowsShowPlace> {
        @Override
        public String getToken(FlowsShowPlace place) {
            return place.getFlowsShowName();
        }

        @Override
        public FlowsShowPlace getPlace(String token) {
            return new FlowsShowPlace(token);
        }
    }

}

