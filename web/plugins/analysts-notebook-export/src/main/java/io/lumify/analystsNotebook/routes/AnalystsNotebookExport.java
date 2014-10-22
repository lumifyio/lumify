package io.lumify.analystsNotebook.routes;

import com.google.inject.Inject;
import io.lumify.analystsNotebook.AnalystsNotebookExporter;
import io.lumify.analystsNotebook.AnalystsNotebookVersion;
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
    private static final String VERSION_PARAMETER_NAME = "version";
    private static final AnalystsNotebookVersion DEFAULT_VERSION = AnalystsNotebookVersion.VERSION_7_OR_8;
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

        AnalystsNotebookVersion version = DEFAULT_VERSION;
        String versionParameter = getOptionalParameter(request, VERSION_PARAMETER_NAME);
        if (versionParameter != null) {
            version = AnalystsNotebookVersion.valueOf(versionParameter);
        }

        Chart chart = analystsNotebookExporter.toChart(version, workspace, user, authorizations);

        List<String> comments = new ArrayList<String>();
        comments.add(String.format("Lumify Workspace: %s", workspace.getDisplayTitle()));
        comments.add(String.format("%s/#w=%s", getBaseUrl(request), workspaceId));
        comments.add(String.format("Exported %1$tF %1$tT %1$tz for Analyst's Notebook version %2$s", new Date(), version.toString()));

        String xml = AnalystsNotebookExporter.toXml(chart, comments);

        respondWithPlaintext(response, xml);
    }

    private String getBaseUrl(HttpServletRequest request) {

        // TODO: check for proxy
        // TODO: move to BaseRequestHandler

        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int port = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(serverName);
        if (!(scheme.equals("http") && port == 80 || scheme.equals("https") && port == 443)) {
            sb.append(":").append(port);
        }
        sb.append(contextPath);
        return sb.toString();
    }
}
