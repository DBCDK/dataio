package dk.dbc.dataio.gui.server.modelmappers;


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

        List<String> matches = model.getDataioPatternMatches();
        if(!matches.isEmpty()) {
            throw new IllegalArgumentException(buildPatternMatchesErrorMsg(matches));
        }

        return new SinkContent(
            model.getSinkName(),
            model.getResourceName());
    }

    /**
     * Maps a list of sinks to a list of sink models
     *
     * @param sinks the list of sinks
     * @return sinkModels the list of sinkModels
     */
    public static List<SinkModel> toListOfSinkModels(List<Sink> sinks) {
        List<SinkModel> sinkModels = new ArrayList<SinkModel>();
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
