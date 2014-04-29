package io.lumify.web.privilegeFilters;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.Privilege;

import java.util.EnumSet;

public class ReadPrivilegeFilter extends PrivilegeFilter {
    @Inject
    protected ReadPrivilegeFilter(UserRepository userRepository, Configuration configuration) {
        super(EnumSet.of(Privilege.READ), userRepository, configuration);
    }
}
