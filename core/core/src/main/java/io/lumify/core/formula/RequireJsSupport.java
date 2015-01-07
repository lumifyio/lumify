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

    @Override
    public String getClassName() {
        return "RequireJsSupport";
    }

    public static void print(Context cx, Scriptable thisObj, Object[] args,
                             Function funObj) {
        for (int i = 0; i < args.length; i++)
            LOGGER.debug(Context.toString(args[i]));
    }

    public static void consoleWarn(Context cx, Scriptable thisObj, Object[] args,
                                  Function funObj) {
        for (int i = 0; i < args.length; i++)
            LOGGER.warn(Context.toString(args[i]));
    }

    public static void consoleError(Context cx, Scriptable thisObj, Object[] args,
                                  Function funObj) {
        for (int i = 0; i < args.length; i++)
            LOGGER.error(Context.toString(args[i]));
    }

    public static void load(Context cx, Scriptable thisObj, Object[] args,
                            Function funObj) throws FileNotFoundException, IOException {
        RequireJsSupport shell = (RequireJsSupport) getTopLevelScope(thisObj);
        for (int i = 0; i < args.length; i++) {
            LOGGER.debug("Loading file " + Context.toString(args[i]));
            shell.processSource(cx, Context.toString(args[i]));
        }
    }

    public static String readFile(Context cx, Scriptable thisObj, Object[] args,
                            Function funObj) throws FileNotFoundException, IOException {
        RequireJsSupport shell = (RequireJsSupport) getTopLevelScope(thisObj);
        if (args.length == 1) {
            return shell.getFileContents(Context.toString(args[0]));
        }
        return null;
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
                return IOUtils.toString(is);
            } catch (IOException e) {
                LOGGER.error("File not readable %s", file, e);
            }
        } else LOGGER.error("File not found %s", file);
        return "";
    }
}

