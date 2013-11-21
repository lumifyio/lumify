package com.altamiracorp.lumify.core.ingest.term.extraction;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.termMention.TermMention;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRowKey;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TermRegexFinder {

    public static List<TermMention> find(String artifactId, GraphVertex concept, String text, String regex) {
        return find(artifactId, concept, text, Pattern.compile(regex));
    }

    public static List<TermMention> find(String artifactId, GraphVertex concept, String text, Pattern regex) {
        Matcher m = regex.matcher(text);
        List<TermMention> termMentions = new ArrayList<TermMention>();
        while (m.find()) {
            TermMentionRowKey termMentionRowKey = new TermMentionRowKey(artifactId, m.start(), m.end());
            TermMention termMention = new TermMention(termMentionRowKey);
            termMention.getMetadata()
                    .setSign(m.group(1).toString())
                    .setOntologyClassUri((String) concept.getProperty(PropertyName.DISPLAY_NAME))
                    .setConceptGraphVertexId(concept.getId());
            termMentions.add(termMention);
        }
        return termMentions;
    }
}
