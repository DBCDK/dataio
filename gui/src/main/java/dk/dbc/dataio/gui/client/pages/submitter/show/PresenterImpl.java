/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.submitter.modify.EditPlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * This class represents the show submitters presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    private final PlaceController placeController;


    /**
     * Default constructor
     *
     * @param placeController The client factory to be used
     */
    public PresenterImpl(PlaceController placeController) {
        this.placeController = placeController;
    }


    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        getView().setPresenter(this);
        getView().setHeader(commonInjector.getMenuTexts().menu_Submitters());
        containerWidget.setWidget(getView().asWidget());
        fetchSubmitters();
    }


    /**
     * This method opens a new view, for editing the submitter in question
     * @param model The model for the submitter to edit
     */
    @Override
    public void editSubmitter(SubmitterModel model) {
        placeController.goTo(new EditPlace(model));
    }

    /**
     * This method opens a new view, for creating a new submitter
     */
    @Override
    public void createSubmitter() {
        getView().selectionModel.clear();
        placeController.goTo(new CreatePlace());
    }

    /*
     * Private methods
     */

    /**
     * This method fetches all submitters, and sends them to the view
     */
    private void fetchSubmitters() {
        commonInjector.getFlowStoreProxyAsync().findAllSubmitters(new FetchSubmittersCallback());
    }


    /**
     * This method deciphers if a submitter has been added, updated or deleted.
     * The view and selection model are updated accordingly
     *
     * @param models the list of submitters returned from flow store proxy
     */
    private void setSubmittersAndDecipherSelection(Set<SubmitterModel> dataProviderSet, List<SubmitterModel> models) {
        if (dataProviderSet.size() > models.size() || dataProviderSet.size() == 0) {
            getView().selectionModel.clear();
            getView().setSubmitters(models);
        } else {
            for (SubmitterModel current : models) {
                if (!dataProviderSet.contains(current)) {
                    getView().setSubmitters(models);
                    getView().selectionModel.setSelected(current, true);
                    break;
                }
            }
        }
    }


    /*
     * Private classes
     */

    /**
     * This class is the callback class for the findAllSubmitters method in the Flow Store
     */
    protected class FetchSubmittersCallback extends FilteredAsyncCallback<List<SubmitterModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<SubmitterModel> models) {
            setSubmittersAndDecipherSelection(new HashSet<SubmitterModel>(getView().dataProvider.getList()), models);
        }
    }

    private View getView() {
        return viewInjector.getView();
    }
}
