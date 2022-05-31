package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.FlowView;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.util.Format;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public final class FlowModelMapper {
    private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Format.LONG_DATE_TIME_FORMAT);

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private FlowModelMapper() {
    }

    /**
     * Maps a Flow to a Model
     *
     * @param flow, the flow
     * @return the flow model
     */
    public static FlowModel toModel(Flow flow) {
        return new FlowModel(
                flow.getId(),
                flow.getVersion(),
                flow.getContent().getName(),
                flow.getContent().getDescription(),
                flow.getContent().getTimeOfFlowComponentUpdate() == null ? "" : simpleDateFormat.format(flow.getContent().getTimeOfFlowComponentUpdate()),
                FlowComponentModelMapper.toListOfFlowComponentModels(flow.getContent().getComponents())
        );
    }

    public static FlowModel toModel(FlowView flowView) {
        return new FlowModel(
                flowView.getId(),
                flowView.getVersion(),
                flowView.getName(),
                flowView.getDescription(),
                flowView.getTimeOfComponentUpdate() == null ? "" : simpleDateFormat.format(flowView.getTimeOfComponentUpdate()),
                FlowComponentModelMapper.fromListOfFlowComponentViews(flowView.getComponents())
        );
    }

    /**
     * Maps a model to flow content
     *
     * @param model          The model
     * @param flowComponents the list of flow components
     * @return FlowContent The content of the Flow
     * @throws IllegalArgumentException if any matches were found
     */
    public static FlowContent toFlowContent(FlowModel model, List<FlowComponent> flowComponents) throws IllegalArgumentException {
        if (model.isInputFieldsEmpty()) {
            throw new IllegalArgumentException("model.name, model.description, model.flowcomponents cannot be empty");
        }

        List<String> matches = model.getDataioPatternMatches();
        if (!matches.isEmpty()) {
            throw new IllegalArgumentException(buildPatternMatchesErrorMsg(matches));
        }
        try {
            return new FlowContent(
                    model.getFlowName(),
                    model.getDescription(),
                    flowComponents,
                    model.getTimeOfFlowComponentUpdate().isEmpty() ? null : simpleDateFormat.parse(model.getTimeOfFlowComponentUpdate())
            );
        } catch (ParseException e) {
            throw new IllegalArgumentException("error parsing timeOfFlowComponentUpdate");
        }
    }

    /**
     * Maps a list of flows to a list of flow models
     *
     * @param flows the list of flows
     * @return flowModels the list of flowModels
     */
    public static List<FlowModel> toListOfFlowModels(List<Flow> flows) {
        List<FlowModel> flowModels = new ArrayList<FlowModel>();
        for (Flow flow : flows) {
            flowModels.add(toModel(flow));
        }
        return flowModels;
    }

    public static List<FlowModel> fromListOfFlowViews(List<FlowView> flowViews) {
        List<FlowModel> flowModels = new ArrayList<>();
        for (FlowView flowView : flowViews) {
            flowModels.add(toModel(flowView));
        }
        return flowModels;
    }

    /*
     * private helper methods
     */

    private static String buildPatternMatchesErrorMsg(List<String> matches) {
        StringBuilder stringBuilder = new StringBuilder("Illegal characters found in flow name:");
        for (String match : matches) {
            stringBuilder.append(" [").append(match).append("],");
        }
        return stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
    }


}
