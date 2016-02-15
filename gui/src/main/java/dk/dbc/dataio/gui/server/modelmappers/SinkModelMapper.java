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


import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.model.SinkModel;

import java.util.ArrayList;
import java.util.List;

public class SinkModelMapper {
    /**
     * Private Constructor prevents instantiation of this static class
     */
    private SinkModelMapper (){}

    /**
     * Maps a Sink to a SinkModel
     * @param sink, the sink
     * @return model
     */
    public static SinkModel toModel(Sink sink) {
        SinkContent.SinkType sinkType;
        if (sink.getContent() == null || sink.getContent().getSinkType() == null) {
            sinkType = SinkContent.SinkType.ES;  // Old style Sinks - before Sink Config's
        } else {
            sinkType = sink.getContent().getSinkType();
        }
        if (sinkType == SinkContent.SinkType.OPENUPDATE) {
            OpenUpdateSinkConfig sinkConfig = (OpenUpdateSinkConfig) sink.getContent().getSinkConfig();
            List<String> queueProviders = sinkConfig.getAvailableQueueProviders();
            if (queueProviders == null) {
                queueProviders = new ArrayList<>();
            }
            return new SinkModel(
                    sink.getId(),
                    sink.getVersion(),
                    sinkType,
                    sink.getContent().getName(),
                    sink.getContent().getResource(),
                    sink.getContent().getDescription(),
                    sinkConfig.getUserId(),
                    sinkConfig.getPassword(),
                    sinkConfig.getEndpoint(),
                    queueProviders);
        } else {
            return new SinkModel(
                    sink.getId(),
                    sink.getVersion(),
                    sinkType,
                    sink.getContent().getName(),
                    sink.getContent().getResource(),
                    sink.getContent().getDescription());
        }
    }

    /**
     * Maps a model to submitter content
     * @param model, the model to map from
     * @return submitterContent
     * @throws IllegalArgumentException if any model values were illegal
     */
    public static SinkContent toSinkContent(SinkModel model) throws IllegalArgumentException {

        if (model.isInputFieldsEmpty()) {
            if (model.getSinkType() == SinkContent.SinkType.OPENUPDATE) {
                throw new IllegalArgumentException("model.name, model.resource, model.description, config.userId, config.password, config.endpoint cannot be empty");
            } else {
                throw new IllegalArgumentException("model.name, model.resource, model.description cannot be empty");
            }
        }

        List<String> matches = model.getDataioPatternMatches();
        if(!matches.isEmpty()) {
            throw new IllegalArgumentException(buildPatternMatchesErrorMsg(matches));
        }

        if (model.getSinkType() == SinkContent.SinkType.OPENUPDATE) {
            return new SinkContent(
                    model.getSinkName(),
                    model.getResourceName(),
                    model.getDescription(),
                    model.getSinkType(),
                    new OpenUpdateSinkConfig(
                            model.getOpenUpdateUserId(),
                            model.getOpenUpdatePassword(),
                            model.getOpenUpdateEndpoint(),
                            model.getOpenUpdateAvailableQueueProviders()));
        } else {
            return new SinkContent(
                    model.getSinkName(),
                    model.getResourceName(),
                    model.getDescription(),
                    model.getSinkType());
        }
    }

    /**
     * Maps a list of sinks to a list of sink models
     *
     * @param sinks the list of sinks
     * @return sinkModels the list of sinkModels
     */
    public static List<SinkModel> toListOfSinkModels(List<Sink> sinks) {
        List<SinkModel> sinkModels = new ArrayList<>();
        for (Sink sink : sinks) {
            sinkModels.add(toModel(sink));
        }
        return sinkModels;
    }

    /*
     * private helper methods
     */

    private static String buildPatternMatchesErrorMsg(List<String> matches) {
        StringBuilder stringBuilder = new StringBuilder("Illegal characters found in sink name:");
        for(String match : matches) {
            stringBuilder.append(" [").append(match).append("],");
        }
        return stringBuilder.deleteCharAt(stringBuilder.length() -1).toString();
    }

}
