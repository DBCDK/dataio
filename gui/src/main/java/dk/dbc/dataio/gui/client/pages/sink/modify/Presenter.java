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

package dk.dbc.dataio.gui.client.pages.sink.modify;

import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

import java.util.List;

public interface Presenter extends GenericPresenter {
    void sinkTypeChanged(SinkContent.SinkType sinkType);
    void nameChanged(String name);
    void resourceChanged(String resource);
    void descriptionChanged(String description);
    void openUpdateUserIdChanged(String userId);
    void passwordChanged(String password);
    void queueProvidersChanged(List<String> value);
    void endpointChanged(String endpoint);
    void esUserIdChanged(String userId);
    void esDatabaseChanged(String esDatabase);
    void imsEndpointChanged(String imsEndpoint);
    void worldCatUserIdChanged(String userId);
    void worldCatPasswordChanged(String password);
    void worldCatProjectIdChanged(String projectId);
    void worldCatEndpointChanged(String endpoint);
    void worldCatRetryDiagnosticsChanged(List<String> values);
    void vipEndpointChanged(String imsEndpoint);
    void keyPressed();
    void saveButtonPressed();
    void deleteButtonPressed();
    void queueProvidersAddButtonPressed();
    void worldCatRetryDiagnosticsAddButtonPressed();
    void worldCatRetryDiagnosticRemoveButtonPressed(String retryDiagnostic);
    void sequenceAnalysisSelectionChanged(String value);
}
