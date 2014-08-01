package io.lumify.opennlpDictionary.web;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.handlers.StaticResourceHandler;
import io.lumify.web.LumifyCsrfHandler;
import io.lumify.web.WebApp;
import io.lumify.web.WebAppPlugin;
import io.lumify.web.privilegeFilters.AdminPrivilegeFilter;

import javax.servlet.ServletContext;

public class AdminDictionaryWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticationHandlerClass = authenticationHandler.getClass();
        Class<? extends Handler> csrfHandlerClass = LumifyCsrfHandler.class;

        app.get("/admin/dictionaryAdmin.html", authenticationHandler, new StaticResourceHandler(getClass(), "/dictionaryAdmin.html", "text/html"));
        app.get("/admin/dictionary", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, AdminDictionary.class);
        app.get("/admin/dictionary/concept", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, AdminDictionaryByConcept.class);
        app.post("/admin/dictionary", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, AdminDictionaryEntryAdd.class);
        app.post("/admin/dictionary/delete", authenticationHandlerClass, csrfHandlerClass, AdminPrivilegeFilter.class, AdminDictionaryEntryDelete.class);
    }
}
