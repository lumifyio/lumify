package io.lumify.opennlpDictionary.web;

import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.opennlpDictionary.model.DictionaryEntry;
import io.lumify.opennlpDictionary.model.DictionaryEntryRepository;
import io.lumify.web.BaseRequestHandler;
import io.lumify.miniweb.HandlerChain;
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
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
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
