package io.lumify.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiTermMentionsResponse implements ClientApiObject {
    private List<ClientApiElement> termMentions = new ArrayList<ClientApiElement>();

    public List<ClientApiElement> getTermMentions() {
        return termMentions;
    }
}
