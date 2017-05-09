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

import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.model.SubmitterModel;

import java.util.ArrayList;
import java.util.List;

public final class SubmitterModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private SubmitterModelMapper(){}

    /**
     * Maps a Submitter to a Model
     * @param submitter, the submitter to map to a model
     * @return model
     */
    public static SubmitterModel toModel(Submitter submitter){
        return new SubmitterModel(
                submitter.getId(),
                submitter.getVersion(),
                Long.toString(submitter.getContent().getNumber()),
                submitter.getContent().getName(),
                submitter.getContent().getDescription(),
                submitter.getContent().getPriority() == null ? null : submitter.getContent().getPriority().getValue(),
                submitter.getContent().isEnabled());
    }

    /**
     * Maps a model to submitter content
     * @param model, the model to map to submitter content
     * @return submitterContent
     * @throws IllegalArgumentException if any model values were illegal
     */
    public static SubmitterContent toSubmitterContent(SubmitterModel model) throws IllegalArgumentException {

        if(model.isInputFieldsEmpty()) {
            throw new IllegalArgumentException("model.number, model.name, model.description cannot be empty");
        }

        List<String> matches = model.getDataioPatternMatches();
        if(!matches.isEmpty()) {
            throw new IllegalArgumentException(buildPatternMatchesErrorMsg(matches));
        }

        return new SubmitterContent(
                Long.parseLong(model.getNumber()),
                model.getName(),
                model.getDescription(),
                model.getPriority() == null ? null : Priority.of(model.getPriority()),
                model.isEnabled());
    }

    /**
     * Maps a list of submitters to a list of submitter models
     *
     * @param submitters the list of submitters
     * @return submitterModels the list of submitterModels
     */
    public static List<SubmitterModel> toListOfSubmitterModels(List<Submitter> submitters) {
        List<SubmitterModel> submitterModels = new ArrayList<>();
        for (Submitter submitter : submitters) {
            submitterModels.add(toModel(submitter));
        }
        return submitterModels;
    }

     /*
     * private helper methods
     */

    private static String buildPatternMatchesErrorMsg(List<String> matches) {
        StringBuilder stringBuilder = new StringBuilder("Illegal characters found in submitter name:");
        for(String match : matches) {
            stringBuilder.append(" [").append(match).append("],");
        }
        return stringBuilder.deleteCharAt(stringBuilder.length() -1).toString();
    }

}
