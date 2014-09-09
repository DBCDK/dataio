package dk.dbc.dataio.gui.client.pages.flowcomponent.show;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class FlowComponentsShowPlace  extends Place {
    private String flowComponentsShowName;

    public FlowComponentsShowPlace() {
        this.flowComponentsShowName = "";
    }

    public FlowComponentsShowPlace(String flowComponentsShowName) {
        this.flowComponentsShowName = flowComponentsShowName;
    }

    public String getFlowComponentsShowName() {
        return flowComponentsShowName;
    }

    @Prefix("ShowFlowComponents")
    public static class Tokenizer implements PlaceTokenizer<FlowComponentsShowPlace> {
        @Override
        public String getToken(FlowComponentsShowPlace place) {
            return place.getFlowComponentsShowName();
        }

        @Override
        public FlowComponentsShowPlace getPlace(String token) {
            return new FlowComponentsShowPlace(token);
        }
    }
    
}

