package dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class FlowComponentCreatePlace  extends Place {
    private String flowComponentCreateName;

    public FlowComponentCreatePlace() {
        this.flowComponentCreateName = "";
    }

    public FlowComponentCreatePlace(String flowComponentCreateName) {
        this.flowComponentCreateName = flowComponentCreateName;
    }

    public String getFlowComponentCreateName() {
        return flowComponentCreateName;
    }

    @Prefix("CreateFlowComponent")
    public static class Tokenizer implements PlaceTokenizer<FlowComponentCreatePlace> {
        @Override
        public String getToken(FlowComponentCreatePlace place) {
            return place.getFlowComponentCreateName();
        }

        @Override
        public FlowComponentCreatePlace getPlace(String token) {
            return new FlowComponentCreatePlace(token);
        }
    }
    
}

