package com.altamiracorp.lumify.core.ingest.term.extraction;

import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRowKey;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.DISPLAY_NAME;

public class TermRegexFinder {
    public static List<TermMentionModel> find(String artifactId, Vertex concept, String text, String regex, Visibility visibility) {
        return find(artifactId, concept, text, Pattern.compile(regex), visibility);
    }

    public static List<TermMentionModel> find(String artifactId, Vertex concept, String text, Pattern regex, Visibility visibility) {
        Matcher m = regex.matcher(text);
        List<TermMentionModel> termMentions = new ArrayList<TermMentionModel>();
        while (m.find()) {
            if (m.groupCount() != 2) {
                throw new RuntimeException("regex pattern must have 2 capture groups. the first will determine the start and end index, the second will determine the sign.");
            }

            String groupCapture = m.group(1);
            int groupCaptureOffset = text.indexOf(groupCapture, m.start());

            TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, groupCaptureOffset, groupCaptureOffset + groupCapture.length());
            TermMentionModel termMention = new TermMentionModel(termMentionRowKey);
            termMention.getMetadata()
                    .setSign(m.group(2), visibility)
                    .setOntologyClassUri(DISPLAY_NAME.getPropertyValue(concept), visibility)
                    .setConceptGraphVertexId(concept.getId(), visibility);
            termMentions.add(termMention);
        }
        return termMentions;
    }
}
