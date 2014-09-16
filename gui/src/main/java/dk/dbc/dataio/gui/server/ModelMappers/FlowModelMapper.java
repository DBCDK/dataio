package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;

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
    public static FlowContent toFlowContent(FlowModel model) throws IllegalArgumentException {
        if(model.isInputFieldsEmpty()) {
            throw new IllegalArgumentException("model.name, model.description, model.flowcomponents cannot be empty");
        }

        return new FlowContent(
                model.getFlowName(),
                model.getDescription(),
                FlowComponentModelMapper.toListOfFlowComponents(model.getFlowComponents())
        );
    }
}
