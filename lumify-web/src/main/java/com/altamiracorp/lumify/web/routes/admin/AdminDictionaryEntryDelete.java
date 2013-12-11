package com.altamiracorp.lumify.web.routes.admin;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.dictionary.DictionaryEntryRepository;
import com.altamiracorp.lumify.core.model.dictionary.DictionaryEntryRowKey;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AdminDictionaryEntryDelete extends BaseRequestHandler{

    private DictionaryEntryRepository dictionaryEntryRepository;

    @Inject
    public AdminDictionaryEntryDelete (DictionaryEntryRepository dictionaryEntryRepository) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String strRowKey = getAttributeString(request, "entryRowKey");
        User user = getUser(request);

        dictionaryEntryRepository.delete(new DictionaryEntryRowKey(strRowKey),user.getModelUserContext());

        JSONObject resultJson = new JSONObject();
        resultJson.put("success", true);

        respondWithJson(response,resultJson);
    }
}
