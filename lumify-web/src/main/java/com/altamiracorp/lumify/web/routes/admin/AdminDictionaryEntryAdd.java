package com.altamiracorp.lumify.web.routes.admin;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.dictionary.DictionaryEntry;
import com.altamiracorp.lumify.core.model.dictionary.DictionaryEntryRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AdminDictionaryEntryAdd extends BaseRequestHandler {

    private DictionaryEntryRepository dictionaryEntryRepository;

    @Inject
    public AdminDictionaryEntryAdd(
            final DictionaryEntryRepository dictionaryEntryRepository,
            final UserRepository userRepository,
            final Configuration configuration) {
        super(userRepository, configuration);
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String tokens = getRequiredParameter(request, "tokens");
        final String concept = getRequiredParameter(request, "concept");
        final String resolvedName = getOptionalParameter(request, "resolvedName");
        User user = getUser(request);

        DictionaryEntry entry = dictionaryEntryRepository.saveNew(tokens, concept, resolvedName, user);

        JSONObject resultJson = new JSONObject();
        resultJson.put("success", true);
        resultJson.put("entry", entry.toJson());

        respondWithJson(response, resultJson);
    }

}
