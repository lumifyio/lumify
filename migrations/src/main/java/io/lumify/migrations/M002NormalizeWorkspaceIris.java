package io.lumify.migrations;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import org.securegraph.Graph;

public class M002NormalizeWorkspaceIris extends OntologyMigrationBase {
    public static void main(String[] args) throws Exception {
        System.exit(run(M002NormalizeWorkspaceIris.class, args));
    }

    @Inject
    public M002NormalizeWorkspaceIris(
            Graph graph,
            Configuration configuration
    ) {
        super(graph, configuration);
    }

    @Override
    protected Class<? extends OntologyMigrationMapper> getMigrationMapperClass() {
        return M002NormalizeWorkspaceIrisMapper.class;
    }

    @Override
    protected int getFromVersion() {
        return 2;
    }

    public static class M002NormalizeWorkspaceIrisMapper extends OntologyMigrationMapper {
        protected M002NormalizeWorkspaceIrisMapper() {
            super(getOntologyMigrations());
        }

        private static OntologyMigration[] getOntologyMigrations() {
            return new OntologyMigration[]{
                    new RelationshipIriRenameOntologyMigration("http://lumify.io/workspace/toEntity", "http://lumify.io/workspace#toEntity"),
                    new RelationshipIriRenameOntologyMigration("http://lumify.io/workspace/toUser", "http://lumify.io/workspace#toUser"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/workspace/toUser/creator", "http://lumify.io/workspace#toUser/creator"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/workspace/toUser/access", "http://lumify.io/workspace#toUser/access"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/workspace/toEntity/graphPositionX", "http://lumify.io/workspace#toEntity/graphPositionX"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/workspace/toEntity/graphPositionY", "http://lumify.io/workspace#toEntity/graphPositionY"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/workspace/toEntity/graphLayoutJson", "http://lumify.io/workspace#toEntity/graphLayoutJson"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/workspace/toEntity/visible", "http://lumify.io/workspace#toEntity/visible"),
            };
        }
    }
}
