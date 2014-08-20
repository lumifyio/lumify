package io.lumify.opennlpDictionary.web;

import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.opennlpDictionary.model.DictionaryEntryRepository;
import io.lumify.opennlpDictionary.model.DictionaryEntryRowKey;
import io.lumify.web.BaseRequestHandler;
import io.lumify.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AdminDictionaryEntryDelete extends BaseRequestHandler {

    private DictionaryEntryRepository dictionaryEntryRepository;

    @Inject
    public AdminDictionaryEntryDelete(
            final DictionaryEntryRepository dictionaryEntryRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String strRowKey = getAttributeString(request, "entryRowKey");
        User user = getUser(request);

        dictionaryEntryRepository.delete(new DictionaryEntryRowKey(strRowKey));

        JSONObject resultJson = new JSONObject();
        resultJson.put("success", true);

        respondWithJson(response, resultJson);
    }
}
