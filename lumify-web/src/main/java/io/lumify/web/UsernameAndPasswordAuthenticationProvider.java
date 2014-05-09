package io.lumify.web;

import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import com.google.inject.Inject;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UsernameAndPasswordAuthenticationProvider extends AuthenticationProvider {
    private final UserRepository userRepository;

    @Inject
    public UsernameAndPasswordAuthenticationProvider(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String userId = getUserId(request);
        if (userId != null) {
            chain.next(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Override
    public boolean login(HttpServletRequest request) {
        final String username = UrlUtils.urlDecode(request.getParameter("username")).trim();
        final String password = UrlUtils.urlDecode(request.getParameter("password")).trim();

        User user = userRepository.findByUsername(username.toLowerCase());
        if (user != null && userRepository.isPasswordValid(user, password)) {
            setUserId(request, user.getUserId());
            return true;
        } else {
            return false;
        }
    }
}
