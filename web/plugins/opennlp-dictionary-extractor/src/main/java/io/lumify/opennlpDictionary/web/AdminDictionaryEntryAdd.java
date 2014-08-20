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
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AdminDictionaryEntryAdd extends BaseRequestHandler {

    private DictionaryEntryRepository dictionaryEntryRepository;

    @Inject
    public AdminDictionaryEntryAdd(
            final DictionaryEntryRepository dictionaryEntryRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
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
