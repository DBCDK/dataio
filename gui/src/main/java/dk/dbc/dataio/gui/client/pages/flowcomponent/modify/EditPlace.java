package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class EditPlace extends AbstractBasePlace {
    private Long flowComponentId;

    public EditPlace(String url) {
        this.flowComponentId = Long.valueOf(url);
    }

    public EditPlace(FlowComponentModel model) {
        this.flowComponentId = model.getId();
    }

    public Long getFlowComponentId() {
        return flowComponentId;
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterEditImpl(this, commonInjector.getMenuTexts().menu_FlowComponentEdit());
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
