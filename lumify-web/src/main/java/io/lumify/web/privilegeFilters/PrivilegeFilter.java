package io.lumify.web.privilegeFilters;

import com.altamiracorp.miniweb.HandlerChain;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.User;
import io.lumify.web.BaseRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

public class PrivilegeFilter extends BaseRequestHandler {
    private final Set<Privilege> requiredPrivileges;

    protected PrivilegeFilter(Set<Privilege> requiredPrivileges, UserRepository userRepository, Configuration configuration) {
        super(userRepository, configuration);
        this.requiredPrivileges = requiredPrivileges;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        Set<Privilege> userPrivileges = getPrivileges(user);
        if (!Privilege.hasAll(userPrivileges, requiredPrivileges)) {
            respondWithAccessDenied(response, "You do not have the required privileges: " + Privilege.toString(requiredPrivileges));
            return;
        }
        chain.next(request, response);
    }
}
