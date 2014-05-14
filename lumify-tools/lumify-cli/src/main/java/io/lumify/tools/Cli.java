package io.lumify.tools;

import java.lang.reflect.Method;
import java.util.Arrays;

public class Cli {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Require tools classname");
            System.exit(1);
            return;
        }
        String className = args[0];
        String[] remainingOfArgs = Arrays.copyOfRange(args, 1, args.length);

        Class clazz = findToolClass(className);
        Method mainMethod = clazz.getMethod("main", String[].class);
        mainMethod.invoke(null, new Object[]{remainingOfArgs});
    }

    private static Class findToolClass(String classname) throws ClassNotFoundException {
        try {
            return Class.forName(classname);
        } catch (ClassNotFoundException e) {
            return Class.forName("io.lumify.tools." + classname);
        }
    }
}
