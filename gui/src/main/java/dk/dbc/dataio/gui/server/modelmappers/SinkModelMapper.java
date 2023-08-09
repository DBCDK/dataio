package dk.dbc.dataio.gui.server.modelmappers;


import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.model.SinkModel;

import java.util.List;
import java.util.stream.Collectors;

public class SinkModelMapper {
    /**
     * Private Constructor prevents instantiation of this static class
     */
    private SinkModelMapper() {
    }

    /**
     * Maps a Sink to a SinkModel
     *
     * @param sink, the sink
     * @return model
     */
    public static SinkModel toModel(Sink sink) {
        return new SinkModel(
                sink.getId(),
                sink.getVersion(),
                sink.getContent().getSinkType(),
                sink.getContent().getName(),
                sink.getContent().getQueue(),
                sink.getContent().getDescription(),
                sink.getContent().getSequenceAnalysisOption(),
                sink.getContent().getSinkConfig(),
                sink.getContent().getTimeout());
    }

    /**
     * Maps a model to submitter content
     *
     * @param model, the model to map from
     * @return submitterContent
     * @throws IllegalArgumentException if any model values were illegal
     */
    public static SinkContent toSinkContent(SinkModel model) throws IllegalArgumentException {

        if (model.isInputFieldsEmpty()) {
            throw new IllegalArgumentException("model.name, model.resource, model.description cannot be empty");
        }

        List<String> matches = model.getDataioPatternMatches();
        if (!matches.isEmpty()) {
            throw new IllegalArgumentException(buildPatternMatchesErrorMsg(matches));
        }

        return new SinkContent(
                model.getSinkName(),
                model.getQueue(),
                model.getDescription(),
                model.getSinkType(),
                model.getSinkConfig(),
                model.getSequenceAnalysisOption(),
                model.getTimeout());
    }

    /**
     * Maps a list of sinks to a list of sink models
     *
     * @param sinks the list of sinks
     * @return sinkModels the list of sinkModels
     */
    public static List<SinkModel> toListOfSinkModels(List<Sink> sinks) {
        return sinks.stream().map(SinkModelMapper::toModel).collect(Collectors.toList());
    }

    /*
     * private helper methods
     */

    private static String buildPatternMatchesErrorMsg(List<String> matches) {
        StringBuilder stringBuilder = new StringBuilder("Illegal characters found in sink name:");
        for (String match : matches) {
            stringBuilder.append(" [").append(match).append("],");
        }
        return stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
    }
}
