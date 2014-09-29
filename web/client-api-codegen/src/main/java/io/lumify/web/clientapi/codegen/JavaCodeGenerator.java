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

    public static final String BASE_PACKAGE = "io.lumify.web.clientapi.codegen";

    // Run with VMWarguments: "-DfileMap=src/main/resources/lumify.json"
    //          Arguments:    "lumify.json"
    //          Working Dir:  "$MODULE_DIR$"
    public static void main(String[] args) {
        try {
            ClientOpts opts = new ClientOpts();
            opts.setUri("https://localhost:8889");
            Map<String, String> properties = new HashMap<String, String>();
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
        File destDir = new File(destinationDir());

        removeModelObjectImports(destDir);
    }

    private void removeModelObjectImports(File destDir) throws IOException {
        for (File f : destDir.listFiles()) {
            if (f.isDirectory()) {
                removeModelObjectImports(f);
            } else {
                String fileContents = FileUtils.readFileToString(f);
                fileContents = fileContents.replaceAll("import io.lumify.web.clientapi.codegen.model.Object;", "");
                fileContents = fileContents.replaceAll("import io.lumify.web.clientapi.codegen.model.LinkedHashMap;", "import java.util.Map;");
                fileContents = fileContents.replaceAll("LinkedHashMap", "Map<String,Object>");
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

    @Override
    public String destinationDir() {
        return "../client-api/src/main/java";
    }

    @Override
    public Some<String> apiPackage() {
        return new Some(BASE_PACKAGE);
    }

    @Override
    public Some<String> invokerPackage() {
        return new Some(BASE_PACKAGE);
    }

    @Override
    public Some<String> modelPackage() {
        return new Some(BASE_PACKAGE + ".model");
    }

    @Override
    public List<Tuple3<String, String, String>> supportingFiles() {
        return (List<Tuple3<String, String, String>>) super.supportingFiles().filterNot(new AbstractFunction1<Tuple3<String, String, String>, Object>() {
            @Override
            public Object apply(Tuple3<String, String, String> item) {
                return item._1().contains("apiInvoker") || item._1().contains("pom") || item._1().contains("JsonUtil");
            }
        });
    }
}
