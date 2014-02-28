package dk.dbc.dataio.gui.client.pages.flowbindersshow;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class FlowBindersShowPlace  extends Place {
    private String flowBindersShowName;

    public FlowBindersShowPlace() {
        this.flowBindersShowName = "";
    }

    public FlowBindersShowPlace(String flowBindersShowName) {
        this.flowBindersShowName = flowBindersShowName;
    }

    public String getFlowBindersShowName() {
        return flowBindersShowName;
    }

    @Prefix("ShowFlowBinders")
    public static class Tokenizer implements PlaceTokenizer<FlowBindersShowPlace> {
        @Override
        public String getToken(FlowBindersShowPlace place) {
            return place.getFlowBindersShowName();
        }

        @Override
        public FlowBindersShowPlace getPlace(String token) {
            return new FlowBindersShowPlace(token);
        }
    }

}

