package io.lumify.web;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class MustacheTemplate {
    public static final String DEFAULT_OUTPUT_CONTENT_TYPE = "text/html";
    private final Mustache template;
    private final String contentType;

    public MustacheTemplate(String templateResourcePath) {
        this(templateResourcePath, MustacheTemplate.DEFAULT_OUTPUT_CONTENT_TYPE);
    }

    public MustacheTemplate(String templateResourcePath, String contentType) {
        MustacheFactory mf = new DefaultMustacheFactory();
        this.template = mf.compile(templateResourcePath);
        this.contentType = contentType;
    }

    public void render(Map<String, Object> data, HttpServletResponse response) throws IOException {
        response.setContentType(this.contentType);
        template.execute(response.getWriter(), data);
    }
}
