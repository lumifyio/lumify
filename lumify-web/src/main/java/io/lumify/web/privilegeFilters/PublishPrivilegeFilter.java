package io.lumify.web.privilegeFilters;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.Privilege;

import java.util.EnumSet;

public class PublishPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected PublishPrivilegeFilter(UserRepository userRepository, Configuration configuration) {
        super(EnumSet.of(Privilege.PUBLISH), userRepository, configuration);
    }
}
