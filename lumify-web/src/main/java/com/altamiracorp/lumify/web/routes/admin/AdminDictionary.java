package com.altamiracorp.lumify.web.routes.admin;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.dictionary.DictionaryEntry;
import com.altamiracorp.lumify.core.model.dictionary.DictionaryEntryRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AdminDictionary extends BaseRequestHandler {

    private DictionaryEntryRepository dictionaryEntryRepository;

    @Inject
    public AdminDictionary(
            final DictionaryEntryRepository dictionaryEntryRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);

        Iterable<DictionaryEntry> dictionary = dictionaryEntryRepository.findAll(user.getModelUserContext());
        JSONArray entries = new JSONArray();
        JSONObject results = new JSONObject();
        for (DictionaryEntry entry : dictionary) {
            entries.put(entry.toJson());
        }

        results.put("entries", entries);

        respondWithJson(response, results);
    }
}
