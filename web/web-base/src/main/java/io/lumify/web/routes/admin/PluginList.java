package io.lumify.web.routes.admin;

import io.lumify.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.bootstrap.lib.LibLoader;
import io.lumify.core.config.Configuration;
import io.lumify.core.ingest.FileImportSupportingFileHandler;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.ingest.graphProperty.PostMimeTypeWorker;
import io.lumify.core.ingest.graphProperty.TermMentionFilter;
import io.lumify.core.model.user.UserListener;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.version.BuildInfo;
import io.lumify.core.version.ProjectInfo;
import io.lumify.core.version.ProjectInfoScanner;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.WebAppPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ServiceLoader;

public class PluginList extends BaseRequestHandler {
    @Inject
    public PluginList(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        JSONObject json = new JSONObject();

        json.put("loadedLibFiles", getLoadedLibFilesJson());
        json.put("graphPropertyWorkers", getGraphPropertyWorkersJson());
        json.put("postMimeTypeWorkers", getPostMimeTypeWorkersJson());
        json.put("userListeners", getUserListenersJson());
        json.put("libLoaders", getLibLoadersJson());
        json.put("fileImportSupportingFileHandlers", getFileImportSupportingFileHandlersJson());
        json.put("termMentionFilters", getTermMentionFiltersJson());
        json.put("webAppPlugins", getWebAppPluginsJson());

        respondWithJson(response, json);
    }

    private JSONArray getUserListenersJson() {
        JSONArray json = new JSONArray();
        for (UserListener userListener : ServiceLoader.load(UserListener.class)) {
            json.put(getUserListenerJson(userListener));
        }
        return json;
    }

    private JSONObject getUserListenerJson(UserListener userListener) {
        JSONObject json = new JSONObject();
        json.put("className", userListener.getClass().getName());
        return json;
    }

    private JSONArray getGraphPropertyWorkersJson() {
        JSONArray json = new JSONArray();
        for (GraphPropertyWorker graphPropertyWorker : ServiceLoader.load(GraphPropertyWorker.class)) {
            json.put(getGraphPropertyWorkerJson(graphPropertyWorker));
        }
        return json;
    }

    private JSONObject getGraphPropertyWorkerJson(GraphPropertyWorker graphPropertyWorker) {
        JSONObject json = new JSONObject();
        json.put("className", graphPropertyWorker.getClass().getName());
        return json;
    }

    private JSONArray getPostMimeTypeWorkersJson() {
        JSONArray json = new JSONArray();
        for (PostMimeTypeWorker postMimeTypeWorker : ServiceLoader.load(PostMimeTypeWorker.class)) {
            json.put(getPostMimeTypeWorkerJson(postMimeTypeWorker));
        }
        return json;
    }

    private JSONObject getPostMimeTypeWorkerJson(PostMimeTypeWorker postMimeTypeWorker) {
        JSONObject json = new JSONObject();
        json.put("className", postMimeTypeWorker.getClass().getName());
        return json;
    }

    private JSONArray getLoadedLibFilesJson() {
        JSONArray json = new JSONArray();
        for (File loadedLibFile : LibLoader.getLoadedLibFiles()) {
            json.put(getLoadedLibFileJson(loadedLibFile));
        }
        return json;
    }

    private JSONObject getLoadedLibFileJson(File loadedLibFile) {
        ProjectInfoScanner scanner = new ProjectInfoScanner(loadedLibFile);

        JSONObject json = new JSONObject();
        json.put("fileName", loadedLibFile);

        JSONArray projectInfosJson = new JSONArray();
        for (ProjectInfo projectInfo : scanner) {
            projectInfosJson.put(getProjectInfoJson(projectInfo));
        }
        json.put("projectInfos", projectInfosJson);

        return json;
    }

    private JSONObject getProjectInfoJson(ProjectInfo projectInfo) {
        JSONObject json = new JSONObject();
        json.put("name", projectInfo.getName());
        json.put("artifactId", projectInfo.getArtifactId());
        json.put("buildInfo", getBuildInfoJson(projectInfo.getBuildInfo()));
        json.put("coordinates", projectInfo.getCoordinates());
        json.put("groupId", projectInfo.getGroupId());
        json.put("scmRevision", projectInfo.getScmRevision());
        json.put("source", projectInfo.getSource());
        json.put("version", projectInfo.getVersion());
        return json;
    }

    private JSONObject getBuildInfoJson(BuildInfo buildInfo) {
        JSONObject json = new JSONObject();
        json.put("date", buildInfo.getDate());
        json.put("jvmVendor", buildInfo.getJvmVendor());
        json.put("jvmVersion", buildInfo.getJvmVersion());
        json.put("mavenVersion", buildInfo.getMavenVersion());
        json.put("osArch", buildInfo.getOsArch());
        json.put("osName", buildInfo.getOsName());
        json.put("osVersion", buildInfo.getOsVersion());
        json.put("timestamp", buildInfo.getTimestamp());
        json.put("user", buildInfo.getUser());
        return json;
    }

    private JSONArray getLibLoadersJson() {
        JSONArray json = new JSONArray();
        for (LibLoader libLoader : ServiceLoader.load(LibLoader.class)) {
            json.put(getLibLoaderJson(libLoader));
        }
        return json;
    }

    private JSONObject getLibLoaderJson(LibLoader libLoader) {
        JSONObject json = new JSONObject();
        json.put("className", libLoader.getClass().getName());
        return json;
    }

    private JSONArray getFileImportSupportingFileHandlersJson() {
        JSONArray json = new JSONArray();
        for (FileImportSupportingFileHandler fileImportSupportingFileHandler : ServiceLoader.load(FileImportSupportingFileHandler.class)) {
            json.put(getFileImportSupportingFileHandlerJson(fileImportSupportingFileHandler));
        }
        return json;
    }

    private JSONObject getFileImportSupportingFileHandlerJson(FileImportSupportingFileHandler fileImportSupportingFileHandler) {
        JSONObject json = new JSONObject();
        json.put("className", fileImportSupportingFileHandler.getClass().getName());
        return json;
    }

    private JSONArray getTermMentionFiltersJson() {
        JSONArray json = new JSONArray();
        for (TermMentionFilter termMentionFilter : ServiceLoader.load(TermMentionFilter.class)) {
            json.put(getTermMentionFilterJson(termMentionFilter));
        }
        return json;
    }

    private JSONObject getTermMentionFilterJson(TermMentionFilter termMentionFilter) {
        JSONObject json = new JSONObject();
        json.put("className", termMentionFilter.getClass().getName());
        return json;
    }

    private JSONArray getWebAppPluginsJson() {
        JSONArray json = new JSONArray();
        for (WebAppPlugin webAppPlugin : ServiceLoader.load(WebAppPlugin.class)) {
            json.put(getWebAppPluginJson(webAppPlugin));
        }
        return json;
    }

    private JSONObject getWebAppPluginJson(WebAppPlugin webAppPlugin) {
        JSONObject json = new JSONObject();
        json.put("className", webAppPlugin.getClass().getName());
        return json;
    }
}
