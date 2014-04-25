package io.lumify.web.roleFilters;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.Roles;

import java.util.EnumSet;

public class ReadRoleFilter extends RoleFilter {
    @Inject
    protected ReadRoleFilter(UserRepository userRepository, Configuration configuration) {
        super(EnumSet.of(Roles.READ), userRepository, configuration);
    }
}
