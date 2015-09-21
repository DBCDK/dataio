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

package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;

import java.util.List;

public interface FlowStoreProxyAsync {
    // Flows
    void createFlow(FlowModel model, AsyncCallback<FlowModel> async);
    void updateFlow(FlowModel model, AsyncCallback<FlowModel> async);
    void deleteFlow(long flowId, long version, AsyncCallback<Void> async);
    void findAllFlows(AsyncCallback<List<FlowModel>> async);
    void getFlow(Long id, AsyncCallback<FlowModel> async);

    // Flow Components
    void createFlowComponent(FlowComponentModel model, AsyncCallback<FlowComponentModel> async);
    void updateFlowComponent(FlowComponentModel model, AsyncCallback<FlowComponentModel> async);
    void refreshFlowComponents(Long id, Long version, AsyncCallback<FlowModel> async);
    void findAllFlowComponents(AsyncCallback<List<FlowComponentModel>> async);
    void getFlowComponent(Long id, AsyncCallback<FlowComponentModel> async);

    // Flow Binders
    void createFlowBinder(FlowBinderModel model, AsyncCallback<FlowBinderModel> async);
    void updateFlowBinder(FlowBinderModel model, AsyncCallback<FlowBinderModel> async);
    void deleteFlowBinder(long flowBinderId, long version, AsyncCallback<Void> async);
    void findAllFlowBinders(AsyncCallback<List<FlowBinderModel>> async);
    void getFlowBinder(long id, AsyncCallback<FlowBinderModel> async);

    // Submitters
    void createSubmitter(SubmitterModel model, AsyncCallback<SubmitterModel> async);
    void updateSubmitter(SubmitterModel model, AsyncCallback<SubmitterModel> async);
    void deleteSubmitter(long submitterId, long version, AsyncCallback<Void> async);
    void findAllSubmitters(AsyncCallback<List<SubmitterModel>> async);
    void getSubmitter(Long id, AsyncCallback<SubmitterModel> async);

    // Sinks
    void createSink(SinkModel model, AsyncCallback<SinkModel> async);
    void updateSink(SinkModel model, AsyncCallback<SinkModel> async);
    void deleteSink(long sinkId, long version, AsyncCallback<Void> async);
    void findAllSinks(AsyncCallback<List<SinkModel>> async);
    void getSink(Long id, AsyncCallback<SinkModel> async);

    // Other
    void close(AsyncCallback<Void> async);
}
