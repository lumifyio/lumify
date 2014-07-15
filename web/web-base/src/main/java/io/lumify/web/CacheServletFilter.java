package io.lumify.web;

import com.google.inject.Inject;
import io.lumify.core.bootstrap.InjectHelper;
import io.lumify.core.config.Configuration;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CacheServletFilter implements Filter {
    private Configuration configuration;
    private Integer maxAge;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        InjectHelper.inject(this);
        String maxAgeString = this.configuration.get("web.cacheServletFilter.maxAge", null);
        if (maxAgeString != null) {
            maxAge = Integer.parseInt(maxAgeString);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse && maxAge != null) {
            BaseRequestHandler.setMaxAge((HttpServletResponse) response, maxAge);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
