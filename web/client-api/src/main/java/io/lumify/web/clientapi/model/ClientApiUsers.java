package io.lumify.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiUsers implements ClientApiObject {
    private List<ClientApiUser> users = new ArrayList<ClientApiUser>();

    public List<ClientApiUser> getUsers() {
        return users;
    }
}
