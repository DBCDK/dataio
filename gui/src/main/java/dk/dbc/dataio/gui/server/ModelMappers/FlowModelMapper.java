package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FlowModelMapper {

    private final static String NOT_APPLICABLE = "n/a";

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
                getFlowComponentsMap(flow.getContent())
        );
    }

    private static Map<String, String> getFlowComponentsMap(FlowContent content) {
        Map<String, String> map = new HashMap<String, String>();
        for (FlowComponent flowComponent: content.getComponents()) {
            map.put(Long.toString(flowComponent.getId()), flowComponent.getContent().getName());
        }
        return map;
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
                getFlowComponentsList(model.getFlowComponents())
        );
    }

    private static List<FlowComponent> getFlowComponentsList(Map<String, String> flowComponentMap) {
        List<FlowComponent> list = new ArrayList<FlowComponent>();
        for (Map.Entry<String, String> entry: flowComponentMap.entrySet()) {
            List<JavaScript> javaScripts = new ArrayList<JavaScript>();
            javaScripts.add(new JavaScript(NOT_APPLICABLE, NOT_APPLICABLE));  // Java script list must not be empty
            FlowComponentContent content = new FlowComponentContent(entry.getValue(), NOT_APPLICABLE, 1L, NOT_APPLICABLE, javaScripts, "");
            list.add(new FlowComponent(Long.parseLong(entry.getKey()), 1L, content));
        }
        return list;
    }
}
