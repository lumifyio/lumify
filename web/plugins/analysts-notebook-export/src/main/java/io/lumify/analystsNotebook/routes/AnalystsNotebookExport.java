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
import org.apache.commons.io.IOUtils;
import org.securegraph.Authorizations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnalystsNotebookExport extends BaseRequestHandler {
    private static final String VERSION_PARAMETER_NAME = "version";
    private static final AnalystsNotebookVersion DEFAULT_VERSION = AnalystsNotebookVersion.VERSION_8_9;
    private static final String FILE_EXT = "anx";
    private static final String CONTENT_TYPE = "application/xml";
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

        Locale locale = getLocale(request);
        String timeZone = getTimeZone(request);
        String baseUrl = getBaseUrl(request);
        Chart chart = analystsNotebookExporter.toChart(version, workspace, user, authorizations, locale, timeZone, baseUrl);

        List<String> comments = new ArrayList<String>();
        comments.add(String.format("Lumify Workspace: %s", workspace.getDisplayTitle()));
        comments.add(String.format("%s/#w=%s", baseUrl, workspaceId));
        comments.add(String.format("Exported %1$tF %1$tT %1$tz for Analyst's Notebook version %2$s", new Date(), version.toString()));

        String xml = chart.toXml(comments);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        response.setContentType(CONTENT_TYPE);
        setMaxAge(response, EXPIRES_1_HOUR);
        String filename = workspace.getDisplayTitle().replaceAll("[^A-Za-z0-9]", "_") + "_" + simpleDateFormat.format(new Date()) + "." + FILE_EXT;
        response.addHeader("Content-Disposition", "attachment; filename=" + filename.replaceAll("_{2,}", "_"));

        InputStream in = new ByteArrayInputStream(xml.getBytes());
        try {
            IOUtils.copy(in, response.getOutputStream());
        } finally {
            in.close();
        }
        chain.next(request, response);
    }
}
