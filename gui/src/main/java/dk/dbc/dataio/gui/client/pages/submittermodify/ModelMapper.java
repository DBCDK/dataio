package dk.dbc.dataio.gui.client.pages.submittermodify;

import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;

public final class ModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private ModelMapper(){}

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
    public static Submitter toSubmitter(Model model){
        SubmitterContent content = new SubmitterContent(
                Long.valueOf(model.getNumber()).longValue(),
                model.getName(),
                model.getDescription());

        return new Submitter(model.getId(), model.getVersion(), content);
    }

}
