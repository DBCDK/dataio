package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;

import java.util.ArrayList;
import java.util.List;

public final class FlowComponentModelMapper {
    /**
     * Private Constructor prevents instantiation of this static class
     */
    private FlowComponentModelMapper() {
    }

    /**
     * Maps a Flow Component to a Model
     *
     * @param flowComponent The Flow component to map
     * @return model The requested FlowComponentModel
     */
    public static FlowComponentModel toModel(FlowComponent flowComponent) {
        return new FlowComponentModel(
                flowComponent.getId(),
                flowComponent.getVersion(),
                flowComponent.getContent().getName(),
                flowComponent.getContent().getSvnProjectForInvocationJavascript(),
                String.valueOf(flowComponent.getContent().getSvnRevision()),
                flowComponent.getContent().getInvocationJavascriptName(),
                flowComponent.getContent().getInvocationMethod(),
                getJavaScriptNames(flowComponent.getContent().getJavascripts())
        );
    }

    private static List<String> getJavaScriptNames(List<JavaScript> javascripts) {
        List<String> javascriptNames = new ArrayList<String>();
        for (JavaScript javaScript: javascripts) {
            javascriptNames.add(javaScript.getModuleName());
        }
        return javascriptNames;
    }

    /**
     * Maps a list of flow components to a list of flow component models
     *
     * @param flowComponents the list of flowComponents
     * @return flowComponentModels the list of flowComponentModels
     */
    public static List<FlowComponentModel> toListOfFlowComponentModels(List<FlowComponent> flowComponents) {
        List<FlowComponentModel> flowComponentModels = new ArrayList<FlowComponentModel>();
        for (FlowComponent flowComponent : flowComponents) {
            flowComponentModels.add(toModel(flowComponent));
        }
        return flowComponentModels;
    }

}
