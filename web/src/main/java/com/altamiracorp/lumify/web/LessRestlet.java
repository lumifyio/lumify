package com.altamiracorp.lumify.web;

import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.miniweb.utils.UrlUtils;
import org.apache.commons.io.FilenameUtils;
import org.lesscss.LessCompiler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

public class LessRestlet extends BaseRequestHandler {
  private static File rootDir;

  public static void init(File rootDir) {
    LessRestlet.rootDir = rootDir;
  }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        LessCompiler lessCompiler = new LessCompiler();
        lessCompiler.setCompress(false);

        String path = request.getRequestURL().substring(UrlUtils.getRootRef(request).length());
        File file = new File(rootDir, path);
        String fileNameWithoutExtension = file.toString().substring(0, file.toString().length() - (FilenameUtils.getExtension(file.toString()).length() + 1));
        file = new File(fileNameWithoutExtension + ".less");

        String css = lessCompiler.compile(file);

        response.setContentType("text/css");
        response.getWriter().write(css);
    }
}
