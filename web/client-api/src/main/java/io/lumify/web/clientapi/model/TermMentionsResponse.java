package io.lumify.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class TermMentionsResponse {
    private List<Element> termMentions = new ArrayList<Element>();

    public List<Element> getTermMentions() {
        return termMentions;
    }
}
