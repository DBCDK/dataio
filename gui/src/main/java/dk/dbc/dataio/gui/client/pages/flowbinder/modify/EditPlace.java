package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.util.ClientFactory;

public class EditPlace extends AbstractBasePlace {
    private Long flowBinderId;

    public EditPlace(String url) {
        this.flowBinderId = Long.valueOf(url);
    }

    public EditPlace(FlowBinderModel model) {
        this.flowBinderId = model.getId();
    }

    public Long getFlowBinderId() {
        return flowBinderId;
    }

    @Override
    public Activity createPresenter(ClientFactory clientFactory) {
        return new PresenterEditImpl(this, commonInjector.getMenuTexts().menu_FlowBinderEdit());
    }

    @Prefix("EditFlowBinder")
    public static class Tokenizer implements PlaceTokenizer<EditPlace> {
        @Override
        public String getToken(EditPlace place) {
            return String.valueOf(place.getFlowBinderId());
        }

        @Override
        public EditPlace getPlace(String token) {
            return new EditPlace(token);
        }
    }
}
