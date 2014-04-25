package io.lumify.web.roleFilters;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.Roles;

import java.util.EnumSet;

public class PublishRoleFilter extends RoleFilter {
    @Inject
    protected PublishRoleFilter(UserRepository userRepository, Configuration configuration) {
        super(EnumSet.of(Roles.PUBLISH), userRepository, configuration);
    }
}
