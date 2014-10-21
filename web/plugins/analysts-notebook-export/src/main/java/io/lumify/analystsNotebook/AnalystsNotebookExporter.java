package io.lumify.analystsNotebook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.analystsNotebook.model.*;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.ontology.Concept;
import io.lumify.core.model.ontology.OntologyRepository;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceEntity;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import org.apache.commons.codec.binary.Base64;
import org.securegraph.Authorizations;
import org.securegraph.Edge;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.util.LookAheadIterable;

import java.util.*;

import static org.securegraph.util.IterableUtils.toList;

@Singleton
public class AnalystsNotebookExporter {
    private static final String XML_DECLARATION = "<?xml version='1.0' encoding='UTF-8'?>";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private Graph graph;
    private WorkspaceRepository workspaceRepository;
    private OntologyRepository ontologyRepository;

    @Inject
    public AnalystsNotebookExporter(Graph graph, WorkspaceRepository workspaceRepository, OntologyRepository ontologyRepository) {
        this.graph = graph;
        this.workspaceRepository = workspaceRepository;
        this.ontologyRepository = ontologyRepository;
    }

    public static String toXml(Chart chart, List<String> comments) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(XML_DECLARATION).append(LINE_SEPARATOR);
            sb = appendComments(sb, comments);
            sb.append(getXmlMapper().writeValueAsString(chart));
            return sb.toString();
        } catch (JsonProcessingException e) {
            throw new LumifyException("exception while generating XML", e);
        }
    }

    public static StringBuilder appendComments(StringBuilder sb, List<String> comments) {
        if (comments != null && comments.size() > 0) {
            if (comments.size() == 1) {
                return sb.append("<!-- ").append(comments.get(0)).append(" -->").append(LINE_SEPARATOR);
            } else {
                for (int i = 0; i < comments.size(); i++) {
                    sb.append(i == 0 ? "<!-- " : "     ").append(comments.get(i)).append(LINE_SEPARATOR);
                }
                sb.append(" -->").append(LINE_SEPARATOR);
            }
        }
        return sb;
    }

    public Chart toChart(AnalystsNotebookVersion version, Workspace workspace, User user, Authorizations authorizations) {
        List<WorkspaceEntity> workspaceEntities = workspaceRepository.findEntities(workspace, user);

        Iterable<String> vertexIds = getVisibleWorkspaceEntityIds(workspaceEntities);
        Iterable<Vertex> vertices = graph.getVertices(vertexIds, authorizations);
        Map<Vertex, WorkspaceEntity> vertexWorkspaceEntityMap = createVertexWorkspaceEntityMap(vertices, workspaceEntities);
        Map<String, String> conceptTypeIconFileMap = createConceptTypeIconFileMap(vertices);

        List<Edge> edges = toList(graph.getEdges(graph.findRelatedEdges(vertexIds, authorizations), authorizations));

        String classificationBanner = "CLASSIFICATION BANNER"; // TODO: get the aggregate visibility with a service?

        Chart chart = new Chart();

        chart.setLinkTypeCollection(getLinkTypes());

        List<EntityType> entityTypes = new ArrayList<EntityType>();
        for (Map.Entry<String, String> entry : conceptTypeIconFileMap.entrySet()) {
            entityTypes.add(new EntityType(entry.getKey(), entry.getValue()));
        }
        chart.setEntityTypeCollection(entityTypes);

        List<ChartItem> chartItems = new ArrayList<ChartItem>();
        for (Map.Entry<Vertex, WorkspaceEntity> entry : vertexWorkspaceEntityMap.entrySet()) {
            chartItems.add(ChartItem.createFromVertexAndWorkspaceEntity(version, entry.getKey(), entry.getValue()));
        }
        for (Edge edge : edges) {
            chartItems.add(ChartItem.createFromEdge(version, edge));
        }
        chartItems.add(getLabelChartItem(classificationBanner, 4889, 7, "class_header"));
        chartItems.add(getLabelChartItem(classificationBanner, 4889, 6667, "class_footer"));
        chart.setChartItemCollection(chartItems);

        if (version == AnalystsNotebookVersion.VERSION_7_OR_8) {
            chart.setSummary(getSummary(classificationBanner));
            chart.setPrintSettings(getPrintSettings());
        }

        return chart;
    }

    private ChartItem getLabelChartItem(String chartItemLabelAndDescription, int x, int y, String labelId) {
        ChartItem chartItem = new ChartItem();
        chartItem.setLabel(chartItemLabelAndDescription);
        chartItem.setDescription(chartItemLabelAndDescription);
        chartItem.setDateSet(false);
        chartItem.setxPosition(x);
        Label label = new Label();
        label.setLabelId(labelId);
        End end = new End();
        end.setY(y);
        end.setLabel(label);
        chartItem.setEnd(end);
        return chartItem;
    }

    private Map<String, String> createConceptTypeIconFileMap(Iterable<Vertex> vertices) {
        Map<String, String> map = new HashMap<String, String>();
        for (Vertex vertex : vertices) {
            String conceptType = (String) vertex.getPropertyValue(LumifyProperties.CONCEPT_TYPE.getPropertyName());
            if (!map.containsKey(conceptType)) {
                Concept concept = ontologyRepository.getConceptByIRI(conceptType);
                String iconFile = getGlyphIcon(concept);
                if (iconFile == null) {
                    iconFile = EntityType.ICON_FILE_DEFAULT;
                }
                map.put(conceptType, iconFile);
            }
        }
        return map;
    }

    private String getGlyphIcon(Concept concept) {
        if (concept.hasGlyphIconResource()) {
            byte[] glyphIcon = concept.getGlyphIcon();
            return Base64.encodeBase64String(glyphIcon);
        } else {
            concept = ontologyRepository.getParentConcept(concept);
            if (concept != null) {
                return getGlyphIcon(concept);
            } else {
                return null;
            }
        }
    }

    private Map<Vertex, WorkspaceEntity> createVertexWorkspaceEntityMap(Iterable<Vertex> vertices, List<WorkspaceEntity> workspaceEntities) {
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

    private List<LinkType> getLinkTypes() {
        List<LinkType> linkTypes = new ArrayList<LinkType>();
        LinkType linkType = new LinkType();
        linkType.setColour("65280");
        linkType.setName(LinkType.NAME_LINK);
        linkTypes.add(linkType);
        return linkTypes;
    }

    private PrintSettings getPrintSettings() {
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

    private Summary getSummary(String classificationBanner) {
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

    private static XmlMapper getXmlMapper() {
        XmlMapper mapper = new XmlMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
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
