package io.lumify.web.clientapi.codegen;

import com.wordnik.swagger.codegen.BasicJavaGenerator;
import com.wordnik.swagger.codegen.model.ClientOpts;
import scala.Some;
import scala.Tuple3;
import scala.collection.immutable.List;
import scala.runtime.AbstractFunction1;

import java.util.HashMap;
import java.util.Map;

public class JavaCodeGenerator extends BasicJavaGenerator {

    public static final String BASE_PACKAGE = "io.lumify.web.clientapi.codegen";

    public static void main(String[] args) {
        ClientOpts opts = new ClientOpts();
        opts.setUri("https://localhost:8889");
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("fileMap", System.getProperty("fileMap"));
        opts.setProperties(properties);
        new JavaCodeGenerator().generate(opts);
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
