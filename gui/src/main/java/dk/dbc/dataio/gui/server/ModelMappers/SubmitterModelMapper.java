package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.pages.submittermodify.SubmitterModel;

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
}
