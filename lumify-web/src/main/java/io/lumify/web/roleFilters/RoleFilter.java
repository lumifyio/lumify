package io.lumify.web.roleFilters;

import com.altamiracorp.miniweb.HandlerChain;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.Roles;
import io.lumify.core.user.User;
import io.lumify.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

public class RoleFilter extends BaseRequestHandler {
    private final Set<Roles> requiredRoles;

    protected RoleFilter(Set<Roles> requiredRoles, UserRepository userRepository, Configuration configuration) {
        super(userRepository, configuration);
        this.requiredRoles = requiredRoles;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Set<Roles> userRoles = getRoles(user);
        if (!Roles.hasAll(userRoles, requiredRoles)) {
            respondWithAccessDenied(response, "You do not have the required roles: " + Roles.toString(requiredRoles));
            return;
        }
        chain.next(request, response);
    }
}
