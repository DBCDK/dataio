package dk.dbc.dataio.gui.server.ModelMappers;

import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.pages.submittermodify.Model;

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
    public static Model toModel(Submitter submitter){
        return new Model(
                submitter.getId(),
                submitter.getVersion(),
                Long.toString(submitter.getContent().getNumber()),
                submitter.getContent().getName(),
                submitter.getContent().getDescription());
    }

    /**
     * Maps a Model to a Submitter
     * @param model
     * @return submitter
     */
    public static Submitter toSubmitter(Model model) throws IllegalArgumentException {
        SubmitterContent content;
        if(model.isInputFieldsEmpty()) {
            throw new IllegalArgumentException("model.number, model.name, model.description cannot be empty");
        }
        content = new SubmitterContent(
                Long.parseLong(model.getNumber()),
                model.getName(),
                model.getDescription());

        return new Submitter(model.getId(), model.getVersion(), content);
    }
}
