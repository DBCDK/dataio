package dk.dbc.dataio.gui.server.ModelMappers;


import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.pages.sink.modify.SinkModel;

public class SinkModelMapper {
    /**
     * Private Constructor prevents instantiation of this static class
     */
    private SinkModelMapper (){}

    /**
     * Maps a Sink to a SinkModel
     * @param sink
     * @return model
     */
    public static SinkModel toModel(Sink sink){
        return new SinkModel(
                sink.getId(),
                sink.getVersion(),
                sink.getContent().getName(),
                sink.getContent().getResource());
    }

    /**
     * Maps a model to submitter content
     * @param model
     * @return submitterContent
     * @throws IllegalArgumentException
     */
    public static SinkContent toSinkContent(SinkModel model) throws IllegalArgumentException {

        if(model.isInputFieldsEmpty()) {
            throw new IllegalArgumentException("model.name, model.resource cannot be empty");
        }

        return new SinkContent(
            model.getSinkName(),
            model.getResourceName());
    }
}
