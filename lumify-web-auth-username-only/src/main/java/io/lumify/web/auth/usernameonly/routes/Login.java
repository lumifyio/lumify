package io.lumify.web.auth.usernameonly.routes;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.User;
import io.lumify.web.CurrentUser;
import io.lumify.web.MustacheTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class Login implements Handler {
    private static final String PASSWORD = "8XXuk2tQ523b";
    private final MustacheTemplate template;
    private final UserRepository userRepository;

    public Login(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.template = new MustacheTemplate("username-only/templates/login.mustache");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        if (request.getMethod().toUpperCase().equals("GET")) {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("context", request.getServletContext());
            this.template.render(data, response);
            return;
        }

        final String username = UrlUtils.urlDecode(request.getParameter("username"));

        User user = this.userRepository.findByUsername(username);
        if (user == null) {
            // For form based authentication, username and displayName will be the same
            user = this.userRepository.addUser(username, username, PASSWORD, new String[0]);
        }

        CurrentUser.set(request, user);

        response.sendRedirect(request.getServletContext().getContextPath() + "/");
    }
}
