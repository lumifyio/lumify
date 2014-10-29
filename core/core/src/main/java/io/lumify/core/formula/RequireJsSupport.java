package io.lumify.core.formula;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class RequireJsSupport extends ScriptableObject {

    private static final long serialVersionUID = 1L;
    private static LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RequireJsSupport.class);
    private static final boolean silent = false;

    @Override
    public String getClassName() {
        return "JsRuntime";
    }

    public static void print(Context cx, Scriptable thisObj, Object[] args,
                             Function funObj) {
        if (silent)
            return;
        for (int i = 0; i < args.length; i++)
            LOGGER.info(Context.toString(args[i]));
    }

    public static void load(Context cx, Scriptable thisObj, Object[] args,
                            Function funObj) throws FileNotFoundException, IOException {
        RequireJsSupport shell = (RequireJsSupport) getTopLevelScope(thisObj);
        for (int i = 0; i < args.length; i++) {
            LOGGER.info("Loading file " + Context.toString(args[i]));
            shell.processSource(cx, Context.toString(args[i]));
        }
    }

    private void processSource(Context cx, String filename)
            throws FileNotFoundException, IOException {
        String fileContents = getFileContents(filename);
        cx.evaluateString(this, fileContents, filename, 1, null);
    }

    private String getFileContents(String file) {
        InputStream is = RequireJsSupport.class.getResourceAsStream(file);
        if (is != null) {
            try {
                String s = IOUtils.toString(is);
                return s;
            } catch (IOException e) {
                LOGGER.error("File not found %s", file, e);
            }
        } else LOGGER.error("File not found %s", file);
        return "";
    }
}

