package io.lumify.web.roleFilters;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.Roles;

import java.util.EnumSet;

public class AdminRoleFilter extends RoleFilter {
    @Inject
    protected AdminRoleFilter(UserRepository userRepository, Configuration configuration) {
        super(EnumSet.of(Roles.ADMIN), userRepository, configuration);
    }
}
