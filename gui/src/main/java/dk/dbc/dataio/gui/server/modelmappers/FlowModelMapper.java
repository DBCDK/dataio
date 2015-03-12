package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.gui.client.model.FlowModel;

import java.util.ArrayList;
import java.util.List;

public final class FlowModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private FlowModelMapper(){}

    /**
     * Maps a Flow to a Model
     * @param flow
     * @return model
     */
    public static FlowModel toModel(Flow flow){
        return new FlowModel(
                flow.getId(),
                flow.getVersion(),
                flow.getContent().getName(),
                flow.getContent().getDescription(),
                FlowComponentModelMapper.toListOfFlowComponentModels(flow.getContent().getComponents())
        );
    }

    /**
     * Maps a model to flow content
     * @param model The model
     * @return FlowContent The content of the Flow
     * @throws IllegalArgumentException
     */
    public static FlowContent toFlowContent(FlowModel model, List<FlowComponent> flowComponents) throws IllegalArgumentException {
        if(model.isInputFieldsEmpty()) {
            throw new IllegalArgumentException("model.name, model.description, model.flowcomponents cannot be empty");
        }

        return new FlowContent(
                model.getFlowName(),
                model.getDescription(),
                flowComponents
        );
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

}
