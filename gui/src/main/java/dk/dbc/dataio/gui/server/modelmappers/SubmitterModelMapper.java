package dk.dbc.dataio.gui.server.modelmappers;

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
     * @param submitter
     * @return model
     */
    public static SubmitterModel toModel(Submitter submitter){
        return new SubmitterModel(
                submitter.getId(),
                submitter.getVersion(),
                Long.toString(submitter.getContent().getNumber()),
                submitter.getContent().getName(),
                submitter.getContent().getDescription());
    }

    /**
     * Maps a model to submitter content
     * @param model
     * @return submitterContent
     * @throws IllegalArgumentException
     */
    public static SubmitterContent toSubmitterContent(SubmitterModel model) throws IllegalArgumentException {

        if(model.isInputFieldsEmpty()) {
            throw new IllegalArgumentException("model.number, model.name, model.description cannot be empty");
        }

        return new SubmitterContent(
                Long.parseLong(model.getNumber()),
                model.getName(),
                model.getDescription());
    }

    /**
     * Maps a list of submitters to a list of submitter models
     *
     * @param submitters the list of submitters
     * @return submitterModels the list of submitterModels
     */
    public static List<SubmitterModel> toListOfSubmitterModels(List<Submitter> submitters) {
        List<SubmitterModel> submitterModels = new ArrayList<SubmitterModel>();
        for (Submitter submitter : submitters) {
            submitterModels.add(toModel(submitter));
        }
        return submitterModels;
    }

}
