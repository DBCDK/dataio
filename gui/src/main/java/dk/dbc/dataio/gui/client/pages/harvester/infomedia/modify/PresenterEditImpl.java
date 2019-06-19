/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;


public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {
    private long id;
    private PlaceController placeController;
    
    public PresenterEditImpl(PlaceController placeController, Place place, String header) {
        super(header);
        id = place.getHarvesterId();
        this.placeController = placeController;
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        super.start(containerWidget, eventBus);
    }

    @Override
    public void initializeModel() {}

    @Override
    void saveModel() {}

    @Override
    public void deleteButtonPressed() {}
}
