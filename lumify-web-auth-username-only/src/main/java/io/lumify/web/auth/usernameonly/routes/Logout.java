package io.lumify.web.auth.usernameonly.routes;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.HandlerChain;
import io.lumify.web.CurrentUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Logout implements Handler{
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        CurrentUser.set(request, null);
        request.getSession().invalidate();
        response.sendRedirect(request.getServletContext().getContextPath() + "/");
    }
}
