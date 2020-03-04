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

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowComponentView;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher.fetchRequiredJavaScriptResult;

import java.util.ArrayList;
import java.util.Collections;
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
                flowComponent.getNext() == null ? "" : String.valueOf(flowComponent.getNext().getSvnRevision()),
                flowComponent.getContent().getInvocationJavascriptName(),
                flowComponent.getContent().getInvocationMethod(),
                getJavaScriptNames(flowComponent.getContent().getJavascripts()),
                flowComponent.getNext() == null ? new ArrayList<>() : getJavaScriptNames(flowComponent.getNext().getJavascripts()),
                flowComponent.getContent().getDescription()
        );
    }

    public static FlowComponentModel toModel(FlowComponentView flowComponentView) {
        return new FlowComponentModel(
                flowComponentView.getId(),
                flowComponentView.getVersion(),
                flowComponentView.getName(),
                "",
                flowComponentView.getRevision(),
                flowComponentView.getNextRevision(),
                "",
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                ""
        );
    }

    private static List<String> getJavaScriptNames(List<JavaScript> javascripts) {
        List<String> javascriptNames = new ArrayList<>();
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
        List<FlowComponentModel> flowComponentModels = new ArrayList<>();
        for (FlowComponent flowComponent : flowComponents) {
            flowComponentModels.add(toModel(flowComponent));
        }
        return flowComponentModels;
    }

    public static List<FlowComponentModel> fromListOfFlowComponentViews(List<FlowComponentView> flowComponentViews) {
        List<FlowComponentModel> flowComponentModels = new ArrayList<>();
        if (flowComponentViews != null) {
            for (FlowComponentView flowComponentView : flowComponentViews) {
                flowComponentModels.add(toModel(flowComponentView));
            }
        }
        return flowComponentModels;
    }

    /**
     * Maps a model to a flow component content, containing the java scripts given as input
     * @param model FlowComponentModel
     * @param requiredJavaScripts List javaScript and requireCache
     * @return flowComponentContent
     */
    public static FlowComponentContent toFlowComponentContent(FlowComponentModel model, fetchRequiredJavaScriptResult requiredJavaScripts) throws IllegalArgumentException{
        if (model.isInputFieldsEmptyModulesExcluded()) {
            throw new IllegalArgumentException("The fields in the Flow Component Model cannot be empty");
        }
        if (requiredJavaScripts == null ) {
            throw new IllegalArgumentException("The list of java scripts cannot be null or empty");
        }
        if (requiredJavaScripts.javaScripts == null || requiredJavaScripts.javaScripts.isEmpty()) {
            throw new IllegalArgumentException("The list of java scripts cannot be null or empty");
        }

        List<String> matches = model.getDataioPatternMatches();
        if(!matches.isEmpty()) {
            throw new IllegalArgumentException(buildPatternMatchesErrorMsg(matches));
        }

        return new FlowComponentContent(
                model.getName(),
                model.getSvnProject(),
                Long.valueOf(model.getSvnRevision()),
                model.getInvocationJavascript(),
                requiredJavaScripts.javaScripts,
                model.getInvocationMethod(),
                model.getDescription(),
                requiredJavaScripts.requireCache);
        //TODO handle require
    }

    public static FlowComponentContent toNext(FlowComponentModel model, fetchRequiredJavaScriptResult requiredJavaScripts) throws IllegalArgumentException{
        return new FlowComponentContent(
                model.getName(),
                model.getSvnProject(),
                Long.valueOf(model.getSvnNext()),
                model.getInvocationJavascript(),
                requiredJavaScripts.javaScripts,
                model.getInvocationMethod(),
                model.getDescription(),
                requiredJavaScripts.requireCache);
    }


    /*
     * private helper methods
     */

    private static String buildPatternMatchesErrorMsg(List<String> matches) {
        StringBuilder stringBuilder = new StringBuilder("Illegal characters found in flowComponent name:");
        for(String match : matches) {
            stringBuilder.append(" [").append(match).append("],");
        }
        return stringBuilder.deleteCharAt(stringBuilder.length() -1).toString();
    }

}
