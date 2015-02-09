package io.lumify.migrations;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import org.securegraph.Graph;

public class M001NormalizeUserIris extends OntologyMigrationBase {
    public static void main(String[] args) throws Exception {
        System.exit(run(M001NormalizeUserIris.class, args));
    }

    @Inject
    public M001NormalizeUserIris(
            Graph graph,
            Configuration configuration
    ) {
        super(graph, configuration);
    }

    @Override
    protected Class<? extends OntologyMigrationMapper> getMigrationMapperClass() {
        return M001NormalizeUserIrisMapper.class;
    }

    @Override
    protected int getFromVersion() {
        return 1;
    }

    public static class M001NormalizeUserIrisMapper extends OntologyMigrationMapper {
        protected M001NormalizeUserIrisMapper() {
            super(getOntologyMigrations());
        }

        private static OntologyMigration[] getOntologyMigrations() {
            return new OntologyMigration[]{
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/username", "http://lumify.io/user#username"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/displayName", "http://lumify.io/user#displayName"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/emailAddress", "http://lumify.io/user#emailAddress"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/createDate", "http://lumify.io/user#createDate"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/currentLoginDate", "http://lumify.io/user#currentLoginDate"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/currentLoginRemoteAddr", "http://lumify.io/user#currentLoginRemoteAddr"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/previousLoginDate", "http://lumify.io/user#previousLoginDate"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/previousLoginRemoteAddr", "http://lumify.io/user#previousLoginRemoteAddr"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/loginCount", "http://lumify.io/user#loginCount"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/authorizations", "http://lumify.io/user#authorizations"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/privileges", "http://lumify.io/user#privileges"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/status", "http://lumify.io/user#status"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/currentWorkspace", "http://lumify.io/user#currentWorkspace"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/uiPreferences", "http://lumify.io/user#uiPreferences"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/passwordSalt", "http://lumify.io/user#passwordSalt"),
                    new PropertyIriRenameOntologyMigration("http://lumify.io/user/passwordHash", "http://lumify.io/user#passwordHash"),
            };
        }
    }
}
