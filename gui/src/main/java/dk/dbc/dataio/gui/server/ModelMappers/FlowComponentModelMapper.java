package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;

import java.util.ArrayList;
import java.util.List;

public final class FlowComponentModelMapper {

    private final static String NOT_APPLICABLE = "n/a";

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
     * Maps a model to flow content
     *
     * @param model The model
     * @return FlowContent The content of the Flow
     * @throws IllegalArgumentException
     */
    public static FlowComponentContent toFlowComponentContent(FlowComponentModel model) throws IllegalArgumentException {
        if (model.isInputFieldsEmpty()) {
            throw new IllegalArgumentException("model.name, model.svnProject, model.svnRevision, model.invocationJavascript, model.invocationMethod, model.javascriptModules cannot be empty");
        }
        String a = String.valueOf(model.getSvnRevision());
        return new FlowComponentContent(
                model.getName(),
                model.getSvnProject(),
                Long.parseLong(model.getSvnRevision()),
                model.getInvocationJavascript(),
                getJavaScripts(model.getJavascriptModules()),
                model.getInvocationMethod()
        );
    }

    private static List<JavaScript> getJavaScripts(List<String> javascriptNames) {
        List<JavaScript> javascripts = new ArrayList<JavaScript>();
        for (String module : javascriptNames) {
            javascripts.add(new JavaScript(NOT_APPLICABLE, module));  // Please note, that the content of the javascript is not known at this time, and is there set as being empty!
        }
        return javascripts;
    }

}
