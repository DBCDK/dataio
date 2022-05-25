package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.invariant.InvariantUtil;

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
     * @param flowBinder      The Flow Binder
     * @param flowModel       The Flow Model (The Flow Binder only contains the flow id, therefore the corresponding Flow Model is given here)
     * @param submitterModels A list of Submitter Models (The Flow Binder only contains submitter id's, therefore the list of corresponding Submitter Models is given here)
     * @param sinkModel       The Sink Model (The Flow Binder only contains the sink id, therefore the corresponding Sink Model is given here)
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
                checkedFlowBinder.getContent().getPriority() == null ? null : checkedFlowBinder.getContent().getPriority().getValue(),
                checkedFlowBinder.getContent().getRecordSplitter().name(),
                checkedFlowModel,
                checkedSubmitterModels,
                checkedSinkModel,
                checkedFlowBinder.getContent().getQueueProvider()
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
        if (!matches.isEmpty()) {
            throw new IllegalArgumentException(buildPatternMatchesErrorMsg(matches));
        }

        return new FlowBinderContent(
                model.getName(),
                model.getDescription(),
                model.getPackaging(),
                model.getFormat(),
                model.getCharset(),
                model.getDestination(),
                model.getPriority() == null ? null : Priority.of(model.getPriority()),
                RecordSplitterConstants.RecordSplitter.valueOf(model.getRecordSplitter()),
                model.getFlowModel().getId(),
                getSubmitterIds(model.getSubmitterModels()),
                model.getSinkModel().getId(),
                model.getQueueProvider()
        );
    }

    private static List<Long> getSubmitterIds(List<SubmitterModel> submitterModels) {
        List<Long> submitterIds = new ArrayList<>();
        for (SubmitterModel model : submitterModels) {
            submitterIds.add(model.getId());
        }
        return submitterIds;
    }

    /*
     * private helper methods
     */

    private static String buildPatternMatchesErrorMsg(List<String> matches) {
        StringBuilder stringBuilder = new StringBuilder("Illegal characters found in flowbinder name:");
        for (String match : matches) {
            stringBuilder.append(" [").append(match).append("],");
        }
        return stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
    }
}
