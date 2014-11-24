package io.lumify.analystsNotebook;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.analystsNotebook.aggregateClassification.AggregateClassificationClient;
import io.lumify.analystsNotebook.model.*;
import io.lumify.core.config.Configuration;
import io.lumify.core.formula.FormulaEvaluator;
import io.lumify.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceEntity;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.util.LookAheadIterable;

import java.util.*;

import static org.securegraph.util.IterableUtils.toList;

@Singleton
public class AnalystsNotebookExporter {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AnalystsNotebookExporter.class);

    private Graph graph;
    private WorkspaceRepository workspaceRepository;
    private OntologyRepository ontologyRepository;
    private ArtifactThumbnailRepository artifactThumbnailRepository;
    private Configuration configuration;
    private AnalystsNotebookExportConfiguration analystsNotebookExportConfiguration;
    private AggregateClassificationClient aggregateClassificationClient;

    @Inject
    public AnalystsNotebookExporter(Graph graph,
                                    WorkspaceRepository workspaceRepository,
                                    OntologyRepository ontologyRepository,
                                    ArtifactThumbnailRepository artifactThumbnailRepository,
                                    Configuration configuration) {
        this.graph = graph;
        this.workspaceRepository = workspaceRepository;
        this.ontologyRepository = ontologyRepository;
        this.artifactThumbnailRepository = artifactThumbnailRepository;
        this.configuration = configuration;
        analystsNotebookExportConfiguration = new AnalystsNotebookExportConfiguration();
        configuration.setConfigurables(analystsNotebookExportConfiguration, AnalystsNotebookExportConfiguration.CONFIGURATION_PREFIX);
        aggregateClassificationClient = new AggregateClassificationClient(configuration);
    }

    public Chart toChart(AnalystsNotebookVersion version, Workspace workspace, User user, Authorizations authorizations, Locale locale, String timeZone, String baseUrl) {
        LOGGER.debug("creating Chart from workspace %s for Analyst's Notebook version %s", workspace.getWorkspaceId(), version.toString());

        List<WorkspaceEntity> workspaceEntities = workspaceRepository.findEntities(workspace, user);
        Iterable<String> vertexIds = getVisibleWorkspaceEntityIds(workspaceEntities);
        Iterable<Vertex> vertices = graph.getVertices(vertexIds, authorizations);
        Map<Vertex, WorkspaceEntity> vertexWorkspaceEntityMap = createVertexWorkspaceEntityMap(vertices, workspaceEntities);
        List<Edge> edges = toList(graph.getEdges(graph.findRelatedEdges(vertexIds, authorizations), authorizations));

        Chart chart = new Chart();
        chart.setAttributeClassCollection(createAttributeClassCollection(vertices));
        chart.setLinkTypeCollection(getLinkTypes());
        chart.setEntityTypeCollection(EntityType.createForVertices(vertices, ontologyRepository, version));
        if (version.supports(AnalystsNotebookFeature.CUSTOM_IMAGE_COLLECTION) && analystsNotebookExportConfiguration.enableCustomImageCollection()) {
            chart.setCustomImageCollection(CustomImage.createForVertices(vertices, ontologyRepository));
        }

        String classificationBanner = "hello, world"; //aggregateClassificationClient.getClassificationBanner(vertices);

        if (version.supports(AnalystsNotebookFeature.SUMMARY) && classificationBanner != null) {
            chart.setSummary(getSummary(classificationBanner));
        }
        if (version.supports(AnalystsNotebookFeature.PRINT_SETTINGS) && classificationBanner != null) {
            chart.setPrintSettings(getPrintSettings());
        }

        List<ChartItem> chartItems = new ArrayList<ChartItem>();

        LOGGER.debug("adding %d vertices", vertexWorkspaceEntityMap.size());
        FormulaEvaluator formulaEvaluator = new FormulaEvaluator(configuration, ontologyRepository, locale, timeZone);
        for (Map.Entry<Vertex, WorkspaceEntity> entry : vertexWorkspaceEntityMap.entrySet()) {
            chartItems.add(ChartItem.createFromVertexAndWorkspaceEntity(version, entry.getKey(), entry.getValue(), ontologyRepository, artifactThumbnailRepository, formulaEvaluator, workspace.getWorkspaceId(), authorizations, user, baseUrl, analystsNotebookExportConfiguration));
        }
        formulaEvaluator.close();

        LOGGER.debug("adding %d edges", edges.size());
        for (Edge edge : edges) {
            chartItems.add(ChartItem.createFromEdge(version, edge, ontologyRepository));
        }

        if (classificationBanner != null) {
            int margin = 50;
            int[] minXYmaxXY = getMinXYmaxXY(workspaceEntities);
            int middleX = minXYmaxXY[0] + ((minXYmaxXY[2] - minXYmaxXY[0]) / 2); // center of items
            int headerY = minXYmaxXY[1] - margin; // possible negative value seems ok
            int footerY = minXYmaxXY[3] + margin;
            chartItems.add(ChartItem.createLabel(version, middleX, headerY, classificationBanner, classificationBanner, "class_header"));
            chartItems.add(ChartItem.createLabel(version, middleX, footerY, classificationBanner, classificationBanner, "class_footer"));
        }

        chart.setChartItemCollection(chartItems);
        return chart;
    }

    private List<AttributeClass> createAttributeClassCollection(Iterable<Vertex> vertices) {
        List<AttributeClass> attributeClasses = new ArrayList<AttributeClass>();

        if (analystsNotebookExportConfiguration.includeProperties()) {
            attributeClasses.addAll(AttributeClass.createForVertices(vertices, ontologyRepository));
        }
        if (analystsNotebookExportConfiguration.includeSubtitle()) {
            attributeClasses.add(new AttributeClass(AttributeClass.NAME_SUBTITLE, AttributeClass.TYPE_TEXT, true));
        }
        if (analystsNotebookExportConfiguration.includeTime()) {
            attributeClasses.add(new AttributeClass(AttributeClass.NAME_TIME, AttributeClass.TYPE_TEXT, true));
        }
        if (analystsNotebookExportConfiguration.includeImageUrl()) {
            attributeClasses.add(new AttributeClass(AttributeClass.NAME_IMAGE_URL, AttributeClass.TYPE_TEXT, false));
        }
        if (analystsNotebookExportConfiguration.includeVisibility()) {
            String label = analystsNotebookExportConfiguration.getVisibilityLabel();
            attributeClasses.add(new AttributeClass(label, AttributeClass.TYPE_TEXT, true));
        }

        return attributeClasses;
    }

    private static Map<Vertex, WorkspaceEntity> createVertexWorkspaceEntityMap(Iterable<Vertex> vertices, List<WorkspaceEntity> workspaceEntities) {
        Map<Vertex, WorkspaceEntity> map = new HashMap<Vertex, WorkspaceEntity>();
        for (Vertex vertex : vertices) {
            WorkspaceEntity correspondingWorkspaceEntity = null;
            for (WorkspaceEntity workspaceEntity : workspaceEntities) {
                if (workspaceEntity.getEntityVertexId().equals(vertex.getId())) {
                    correspondingWorkspaceEntity = workspaceEntity;
                    break;
                }
            }
            if (correspondingWorkspaceEntity != null) {
                map.put(vertex, correspondingWorkspaceEntity);
            }
        }
        return map;
    }

    private static int[] getMinXYmaxXY(Collection<WorkspaceEntity> workspaceEntities) {
        int[] minXYmaxXY = {Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0};
        for (WorkspaceEntity workspaceEntity : workspaceEntities) {
            int x = workspaceEntity.getGraphPositionX();
            int y = workspaceEntity.getGraphPositionY();
            /* min x */ minXYmaxXY[0] = x < minXYmaxXY[0] ? x : minXYmaxXY[0];
            /* min y */ minXYmaxXY[1] = y < minXYmaxXY[1] ? y : minXYmaxXY[1];
            /* max x */ minXYmaxXY[2] = x > minXYmaxXY[2] ? x : minXYmaxXY[2];
            /* max y */ minXYmaxXY[3] = y > minXYmaxXY[3] ? y : minXYmaxXY[3];
        }
        return minXYmaxXY;
    }

    private static List<LinkType> getLinkTypes() {
        List<LinkType> linkTypes = new ArrayList<LinkType>();
        LinkType linkType = new LinkType();
        linkType.setColour("65280");
        linkType.setName(LinkType.NAME_LINK);
        linkTypes.add(linkType);
        return linkTypes;
    }

    private static PrintSettings getPrintSettings() {
        PrintSettings printSettings = new PrintSettings();
        List<Header> headers = new ArrayList<Header>();
        Header header = new Header();
        header.setPosition(Header.POSITION_HEADER_FOOTER_POSITION_CENTER);
        header.setProperty("classification");
        header.setVisible(true);
        headers.add(header);
        printSettings.setHeaderCollection(headers);
        List<Footer> footers = new ArrayList<Footer>();
        Footer footer = new Footer();
        footer.setPosition(Footer.POSITION_HEADER_FOOTER_POSITION_CENTER);
        footer.setProperty("classification");
        footer.setVisible(true);
        footers.add(footer);
        printSettings.setFooterCollection(footers);
        return printSettings;
    }

    private static Summary getSummary(String classificationBanner) {
        Summary summary = new Summary();
        List<CustomProperty> customProperties = new ArrayList<CustomProperty>();
        CustomProperty customProperty = new CustomProperty();
        customProperty.setName("classification");
        customProperty.setType(CustomProperty.TYPE_STRING);
        customProperty.setValue(classificationBanner);
        customProperties.add(customProperty);
        summary.setCustomPropertyCollection(customProperties);
        return summary;
    }

    // TODO: this is copied from io.lumify.web.routes.workspace.WorkspaceVertices
    private LookAheadIterable<WorkspaceEntity, String> getVisibleWorkspaceEntityIds(final List<WorkspaceEntity> workspaceEntities) {
        return new LookAheadIterable<WorkspaceEntity, String>() {
            @Override
            protected boolean isIncluded(WorkspaceEntity workspaceEntity, String entityVertexId) {
                return workspaceEntity.isVisible();
            }

            @Override
            protected String convert(WorkspaceEntity workspaceEntity) {
                return workspaceEntity.getEntityVertexId();
            }

            @Override
            protected Iterator<WorkspaceEntity> createIterator() {
                return workspaceEntities.iterator();
            }
        };
    }
}
