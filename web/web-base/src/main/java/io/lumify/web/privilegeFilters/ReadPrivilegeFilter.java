package io.lumify.web.privilegeFilters;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.web.clientapi.model.Privilege;

import java.util.EnumSet;

public class ReadPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected ReadPrivilegeFilter(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(EnumSet.of(Privilege.READ), userRepository, workspaceRepository, configuration);
    }
}
