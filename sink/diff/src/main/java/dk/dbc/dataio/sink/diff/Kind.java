package dk.dbc.dataio.sink.diff;

import com.deblock.jsondiff.DiffGenerator;
import com.deblock.jsondiff.diff.JsonDiff;
import com.deblock.jsondiff.matcher.CompositeJsonMatcher;
import com.deblock.jsondiff.matcher.StrictJsonArrayPartialMatcher;
import com.deblock.jsondiff.matcher.StrictJsonObjectPartialMatcher;
import com.deblock.jsondiff.matcher.StrictPrimitivePartialMatcher;
import com.deblock.jsondiff.viewer.PatchDiffViewer;
import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public enum Kind {
    JSON("jsondiff") {
        @Override
        public String diff(byte[] data1, byte[] data2) {
            CompositeJsonMatcher matcher = new CompositeJsonMatcher(new StrictJsonArrayPartialMatcher(), new StrictJsonObjectPartialMatcher(), new StrictPrimitivePartialMatcher());
            JsonDiff diff = DiffGenerator.diff(new String(data1, UTF_8), new String(data2, UTF_8), matcher);
            if(diff.similarityRate() == 100D) return "";
            return PatchDiffViewer.from(diff).toString();
        }
    },
    XML("xmldiff") {
        @Override
        public String diff(byte[] data1, byte[] data2) {
            Diff diff = DiffBuilder.compare(Input.from(data1)).withTest(Input.from(data2)).ignoreWhitespace().ignoreComments().checkForSimilar().build();
            if(!diff.hasDifferences()) return "";
            return diff.toString();
        }
    },
    PLAINTEXT("plaintextdiff") {
        @Override
        public String diff(byte[] data1, byte[] data2) {
            DiffRowGenerator generator = DiffRowGenerator.create()
                    .oldTag(f -> "-")
                    .newTag(f -> "+")
                    .build();
            List<DiffRow> rows = generator.generateDiffRows(List.of(new String(data1, UTF_8).split("\n")), List.of(new String(data2, UTF_8).split("\n")));
            List<DiffRow> diff = rows.stream().filter(r -> r.getTag() != DiffRow.Tag.EQUAL).collect(Collectors.toList());
            if(diff.isEmpty()) return "";
            return rows.stream().flatMap(Kind::lineDiff).collect(Collectors.joining("\n"));
        }
    };

    private final String tool;
    static String toolPath = SinkConfig.TOOL_PATH.asString();

    Kind(String tool) {
        this.tool = tool;
    }

    public String getTool() {
        return toolPath + '/' + tool;
    }

    public static Kind detect(byte[] data) {
        if (data != null && data.length > 0) {
            if (data[0] == '<') {
                return Kind.XML;
            }
            if (data[0] == '{' || data[0] == '[') {
                return Kind.JSON;
            }
        }
        return Kind.PLAINTEXT;
    }

    public abstract String diff(byte[] data1, byte[] data2);

    private static Stream<String> lineDiff(DiffRow diffRow) {
        if(diffRow.getTag() == DiffRow.Tag.EQUAL) return Stream.of("  " + diffRow.getOldLine());
        return Stream.of(
                Optional.of(diffRow.getOldLine()).filter(s -> !s.isEmpty()).map(s -> "- " + s).stream(),
                Optional.of(diffRow.getNewLine()).filter(s -> !s.isEmpty()).map(s -> "+ " + s).stream()
        ).flatMap(s -> s);
    }
}
