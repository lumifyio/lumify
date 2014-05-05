package io.lumify.web.privilegeFilters;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.Privilege;

import java.util.EnumSet;

public class AdminPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected AdminPrivilegeFilter(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(EnumSet.of(Privilege.ADMIN), userRepository, workspaceRepository, configuration);
    }
}
