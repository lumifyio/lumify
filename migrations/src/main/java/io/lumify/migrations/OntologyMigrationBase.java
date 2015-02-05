package io.lumify.migrations;

import io.lumify.core.config.Configuration;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.securegraph.*;

public abstract class OntologyMigrationBase extends MigrationBase {
    protected OntologyMigrationBase(
            Graph graph,
            Configuration configuration
    ) {
        super(graph, configuration);
    }

    @Override
    protected Class<? extends ElementMigrationMapperBase> getVertexMigrationMapperClass() {
        return getMigrationMapperClass();
    }

    protected abstract Class<? extends OntologyMigrationMapper> getMigrationMapperClass();

    public static abstract class OntologyMigrationMapper extends ElementMigrationMapperBase {
        private final OntologyMigration[] migrations;

        protected OntologyMigrationMapper(OntologyMigration[] migrations) {
            this.migrations = migrations;
        }

        @Override
        protected boolean migrate(Element element, Context context) {
            boolean result = false;
            for (OntologyMigration migration : migrations) {
                if (migration.migrateElement(this, element)) {
                    result = true;
                }
            }

            for (Property property : element.getProperties()) {
                for (OntologyMigration migration : migrations) {
                    if (migration.migrateProperty(this, element, property)) {
                        result = true;
                    }
                }
            }
            return result;
        }
    }

    public static abstract class OntologyMigration {
        public boolean migrateElement(OntologyMigrationMapper ontologyMigrationMapper, Element element) {
            if (element instanceof Vertex) {
                return migrateVertex(ontologyMigrationMapper, (Vertex) element);
            } else if (element instanceof Edge) {
                return migrateEdge(ontologyMigrationMapper, (Edge) element);
            }
            return false;
        }

        @SuppressWarnings("unused")
        public boolean migrateVertex(OntologyMigrationMapper ontologyMigrationMapper, Vertex vertex) {
            return false;
        }

        @SuppressWarnings("unused")
        public boolean migrateEdge(OntologyMigrationMapper ontologyMigrationMapper, Edge edge) {
            return false;
        }

        @SuppressWarnings("unused")
        public boolean migrateProperty(OntologyMigrationMapper ontologyMigrationMapper, Element element, Property property) {
            return false;
        }
    }

    public static class RelationshipIriRenameOntologyMigration extends OntologyMigration {
        private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RelationshipIriRenameOntologyMigration.class);
        private final String fromIri;
        private final String toIri;

        public RelationshipIriRenameOntologyMigration(String fromIri, String toIri) {
            this.fromIri = fromIri;
            this.toIri = toIri;
        }

        @Override
        public boolean migrateEdge(OntologyMigrationMapper ontologyMigrationMapper, Edge edge) {
            if (!edge.getLabel().equalsIgnoreCase(this.fromIri)) {
                return false;
            }

            if (ontologyMigrationMapper.isDryRun()) {
                LOGGER.debug("altering edge iri: %s -> %s", edge.toString(), toIri);
            } else {
                ontologyMigrationMapper.prepareEdge(edge)
                        .alterEdgeLabel(this.toIri)
                        .save(ontologyMigrationMapper.getAuthorizations());
            }
            return true;
        }
    }

    public static class PropertyIriRenameOntologyMigration extends OntologyMigration {
        private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(PropertyIriRenameOntologyMigration.class);
        private final String fromIri;
        private final String toIri;

        public PropertyIriRenameOntologyMigration(String fromIri, String toIri) {
            this.fromIri = fromIri;
            this.toIri = toIri;
        }

        @Override
        public boolean migrateProperty(OntologyMigrationMapper ontologyMigrationMapper, Element element, Property property) {
            if (!property.getName().equals(this.fromIri)) {
                return false;
            }

            if (ontologyMigrationMapper.isDryRun()) {
                LOGGER.debug("changing property iri: %s -> %s", property.toString(), toIri);
            } else {
                if (element instanceof Vertex) {
                    ontologyMigrationMapper.prepareVertex((Vertex) element)
                            .removeProperty(property.getKey(), property.getName(), property.getVisibility())
                            .addPropertyValue(property.getKey(), toIri, property.getValue(), property.getMetadata(), property.getVisibility())
                            .save(ontologyMigrationMapper.getAuthorizations());
                } else {
                    ontologyMigrationMapper.prepareEdge((Edge) element)
                            .removeProperty(property.getKey(), property.getName(), property.getVisibility())
                            .addPropertyValue(property.getKey(), toIri, property.getValue(), property.getMetadata(), property.getVisibility())
                            .save(ontologyMigrationMapper.getAuthorizations());
                }
            }
            return true;
        }
    }
}
