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

package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;

import java.util.ArrayList;
import java.util.List;

public final class FlowBinderModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private FlowBinderModelMapper() {
    }

    /**
     * Maps a Flow Binder to a Model
     *
     * @param flowBinder The Flow Binder
     * @param flowModel The Flow Model (The Flow Binder only contains the flow id, therefore the corresponding Flow Model is given here)
     * @param submitterModels A list of Submitter Models (The Flow Binder only contains submitter id's, therefore the list of corresponding Submitter Models is given here)
     * @param sinkModel The Sink Model (The Flow Binder only contains the sink id, therefore the corresponding Sink Model is given here)
     * @return The resulting Flow Binder Model
     */
    public static FlowBinderModel toModel(FlowBinder flowBinder, FlowModel flowModel, List<SubmitterModel> submitterModels, SinkModel sinkModel) {
        FlowBinder checkedFlowBinder = InvariantUtil.checkNotNullOrThrow(flowBinder, "flowBinder");
        FlowModel checkedFlowModel = InvariantUtil.checkNotNullOrThrow(flowModel, "flowModel");
        List<SubmitterModel> checkedSubmitterModels = InvariantUtil.checkNotNullOrThrow(submitterModels, "submitterModels");
        SinkModel checkedSinkModel = InvariantUtil.checkNotNullOrThrow(sinkModel, "sinkModel");
        return new FlowBinderModel(
                checkedFlowBinder.getId(),
                checkedFlowBinder.getVersion(),
                checkedFlowBinder.getContent().getName(),
                checkedFlowBinder.getContent().getDescription(),
                checkedFlowBinder.getContent().getPackaging(),
                checkedFlowBinder.getContent().getFormat(),
                checkedFlowBinder.getContent().getCharset(),
                checkedFlowBinder.getContent().getDestination(),
                checkedFlowBinder.getContent().getRecordSplitter().name(),
                checkedFlowBinder.getContent().getSequenceAnalysis(),
                checkedFlowModel,
                checkedSubmitterModels,
                checkedSinkModel
                );
    }

    /**
     * Maps a model to Flow Binder content
     *
     * @param model The model
     * @return The content of the Flow Binder
     * @throws IllegalArgumentException if any matches were found
     */
    public static FlowBinderContent toFlowBinderContent(FlowBinderModel model) throws IllegalArgumentException {
        if (model.isInputFieldsEmpty()) {
            throw new IllegalArgumentException("The fields in the Flow Binder Model cannot be empty");
        }

        List<String> matches = model.getDataioPatternMatches();
        if(!matches.isEmpty()) {
            throw new IllegalArgumentException(buildPatternMatchesErrorMsg(matches));
        }

        return new FlowBinderContent(
                model.getName(),
                model.getDescription(),
                model.getPackaging(),
                model.getFormat(),
                model.getCharset(),
                model.getDestination(),
                RecordSplitterConstants.RecordSplitter.valueOf(model.getRecordSplitter()),
                model.getSequenceAnalysis(),
                model.getFlowModel().getId(),
                getSubmitterIds(model.getSubmitterModels()),
                model.getSinkModel().getId()
        );
    }

    private static List<Long> getSubmitterIds(List<SubmitterModel> submitterModels) {
        List<Long> submitterIds = new ArrayList<Long>();
        for (SubmitterModel model: submitterModels) {
            submitterIds.add(model.getId());
        }
        return submitterIds;
    }

    /*
     * private helper methods
     */

    private static String buildPatternMatchesErrorMsg(List<String> matches) {
        StringBuilder stringBuilder = new StringBuilder("Illegal characters found in flowbinder name:");
        for(String match : matches) {
            stringBuilder.append(" [").append(match).append("],");
        }
        return stringBuilder.deleteCharAt(stringBuilder.length() -1).toString();
    }
}
