package io.lumify.sql.web;

import io.lumify.sql.model.HibernateSessionManager;
import org.hibernate.SessionFactory;

import javax.servlet.*;
import java.io.IOException;

public class HibernateSessionManagementFilter implements Filter {

    private static final String ALREADY_FILTERED = "HibernateSessionManagementFilter.filtered";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // noop
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest.getAttribute(ALREADY_FILTERED) != null) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            servletRequest.setAttribute(ALREADY_FILTERED, "true");
            try {
                filterChain.doFilter(servletRequest, servletResponse);
            } finally {
                HibernateSessionManager.clearSession();
            }
        }
    }

    @Override
    public void destroy() {
        HibernateSessionManager.clearSession();
    }
}
