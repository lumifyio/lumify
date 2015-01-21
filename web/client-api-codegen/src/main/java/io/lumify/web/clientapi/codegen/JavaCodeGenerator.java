package io.lumify.web.clientapi.codegen;

import com.wordnik.swagger.codegen.BasicJavaGenerator;
import com.wordnik.swagger.codegen.model.ClientOpts;
import org.apache.commons.io.FileUtils;
import scala.Some;
import scala.Tuple3;
import scala.collection.immutable.List;
import scala.runtime.AbstractFunction1;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JavaCodeGenerator extends BasicJavaGenerator {

    public static final String BASE_CODEGEN_PACKAGE = "io.lumify.web.clientapi.codegen";
    public static final String BASE_PACKAGE = "io.lumify.web.clientapi";

    // Run with VMWarguments: "-DfileMap=src/main/resources/lumify.json"
    //          Arguments:    "lumify.json"
    //          Working Dir:  "$MODULE_DIR$"
    public static void main(String[] args) {
        try {
            ClientOpts opts = new ClientOpts();
            opts.setUri("https://lumify-dev:8889");
            Map<String, String> properties = new HashMap<>();
            properties.put("fileMap", System.getProperty("fileMap"));
            opts.setProperties(properties);
            JavaCodeGenerator generator = new JavaCodeGenerator();
            generator.generate(opts);
            generator.fixFiles();
        } catch (Exception ex) {
            throw new RuntimeException("generate fail", ex);
        }
    }

    private void fixFiles() throws IOException {
        File destDir = new File(destinationDir(), "io/lumify/web/clientapi/codegen");
        fixFiles(destDir);
    }

    private void fixFiles(File destDir) throws IOException {
        File[] files = destDir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                fixFiles(f);
            } else {
                String fileContents = FileUtils.readFileToString(f);
                fileContents = fileContents.replaceAll("import io.lumify.web.clientapi.model.Object;", "");
                fileContents = fileContents.replaceAll("import io.lumify.web.clientapi.model.TResult;", "");
                fileContents = fileContents.replaceAll("import io.lumify.web.clientapi.model.RawString;", "");
                fileContents = fileContents.replaceAll("import io.lumify.web.clientapi.codegen.model.Object;", "");
                fileContents = fileContents.replaceAll("import io.lumify.web.clientapi.codegen.model.LinkedHashMap;", "import java.util.Map;");
                fileContents = fileContents.replaceAll("import io.lumify.web.clientapi.codegen.ApiInvoker;", "import io.lumify.web.clientapi.ApiInvoker;");
                fileContents = fileContents.replaceAll("String basePath =", "protected String basePath =");
                fileContents = fileContents.replaceAll("ApiInvoker apiInvoker =", "protected ApiInvoker apiInvoker =");
                fileContents = fileContents.replaceAll("LinkedHashMap", "Map<String,Object>");

                fileContents = fileContents.replaceAll("return \\(RawString\\) ApiInvoker\\.deserialize\\(response, \"\", RawString\\.class\\);", "return response;");
                fileContents = fileContents.replaceAll("RawString", "String");

                fileContents = fileContents.replaceAll("TResult\\.class", "resultType");

                for (String param : new String[] { "id", "userId", "vertexId", "edgeId", "graphVertexId", "relatedToVertexId" }) {
                    fileContents = fixMultipartFormListFields(fileContents, param);
                }

                fileContents = fileContents.replaceAll("mp\\.field\\(\"file\", file, MediaType\\.MULTIPART_FORM_DATA_TYPE\\);",
                        "com.sun.jersey.core.header.FormDataContentDisposition dispo = com.sun.jersey.core.header.FormDataContentDisposition\n"
                                + "        .name(\"file\")\n"
                                + "        .fileName(file.getName())\n"
                                + "        .size(file.length())\n"
                                + "        .build();\n"
                                + "      com.sun.jersey.multipart.FormDataBodyPart bodyPart = new com.sun.jersey.multipart.FormDataBodyPart(dispo, file, MediaType.MULTIPART_FORM_DATA_TYPE);\n"
                                + "      mp.bodyPart(bodyPart);"
                );
                fileContents = fileContents.replaceAll("public TermMentionMetadata getMetadata\\(\\)", "@com.fasterxml.jackson.annotation.JsonProperty(\"Metadata\")\npublic TermMentionMetadata getMetadata()");
                fileContents = fileContents.replaceAll("public void setMetadata\\(TermMentionMetadata", "@com.fasterxml.jackson.annotation.JsonProperty(\"Metadata\")\npublic void setMetadata(TermMentionMetadata");

                FileUtils.write(f, fileContents);
            }
        }
    }

    private static final String HAS_FIELDS_REPLACE_MARKER = "//ifHasFieldsReplace";

    private String fixMultipartFormListFields(String fileContents, String singularParamName) {
        String pluralParamName = singularParamName + "s";

        fileContents = fileContents.replaceAll(
                String.format("mp.field\\(\"%s\\[\\]\", %s, MediaType.MULTIPART_FORM_DATA_TYPE\\);",
                        pluralParamName, pluralParamName),
                String.format("if(%s != null) { for(String %s:%s) { mp.field(\"%s[]\", %s, MediaType.MULTIPART_FORM_DATA_TYPE); } }\n%s",
                        pluralParamName, singularParamName, pluralParamName, pluralParamName, singularParamName,
                        HAS_FIELDS_REPLACE_MARKER));

        fileContents = fileContents.replaceAll(
                String.format("(?m)%s\\n\\s+if\\(hasFields\\)", HAS_FIELDS_REPLACE_MARKER),
                "      if\\(hasFields && !mp.getFields\\(\\).isEmpty\\(\\)\\)");

        fileContents = fileContents.replaceAll(
                String.format("formParams\\.put\\(\"%s\\[\\]\", %s\\);", pluralParamName, pluralParamName),
                "throw new java.lang.RuntimeException(\"invalid content type\");");

        return fileContents;
    }

    @Override
    public String destinationDir() {
        return "../client-api/src/main/java";
    }

    @Override
    public Some<String> apiPackage() {
        return new Some<>(BASE_CODEGEN_PACKAGE);
    }

    @Override
    public Some<String> invokerPackage() {
        return new Some<>(BASE_CODEGEN_PACKAGE);
    }

    @Override
    public Some<String> modelPackage() {
        return new Some<>(BASE_PACKAGE + ".model");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Tuple3<String, String, String>> supportingFiles() {
        return (List<Tuple3<String, String, String>>) super.supportingFiles().filterNot(new AbstractFunction1<Tuple3<String, String, String>, Object>() {
            @Override
            public Object apply(Tuple3<String, String, String> item) {
                return item._1().contains("apiInvoker") || item._1().contains("pom") || item._1().contains("JsonUtil");
            }
        });
    }
}
