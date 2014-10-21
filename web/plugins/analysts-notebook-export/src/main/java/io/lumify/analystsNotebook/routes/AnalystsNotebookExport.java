package io.lumify.analystsNotebook.routes;

import com.google.inject.Inject;
import io.lumify.analystsNotebook.AnalystsNotebookExporter;
import io.lumify.analystsNotebook.model.Chart;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import org.securegraph.Authorizations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AnalystsNotebookExport extends BaseRequestHandler {
    private WorkspaceRepository workspaceRepository;
    private AnalystsNotebookExporter analystsNotebookExporter;

    @Inject
    public AnalystsNotebookExport(UserRepository userRepository,
                                  WorkspaceRepository workspaceRepository,
                                  Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.workspaceRepository = workspaceRepository;
        analystsNotebookExporter = InjectHelper.getInstance(AnalystsNotebookExporter.class);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Authorizations authorizations = getAuthorizations(request, user);
        String workspaceId = getActiveWorkspaceId(request);
        Workspace workspace = workspaceRepository.findById(workspaceId, user);

        Chart chart = analystsNotebookExporter.toChart(workspace, user, authorizations);

        List<String> comments = new ArrayList<String>();
        comments.add(workspace.getDisplayTitle());
        comments.add("https://lumify/w=" + workspaceId);
        comments.add(String.format("Exported from Lumify %1$tF %1$tT %1$tz", new Date()));

        String xml = AnalystsNotebookExporter.toXml(chart, comments);

        respondWithPlaintext(response, xml);
    }
}
