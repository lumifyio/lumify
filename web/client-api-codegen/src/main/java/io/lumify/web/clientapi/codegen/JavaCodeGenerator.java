package io.lumify.web.clientapi.codegen;

import com.wordnik.swagger.codegen.BasicJavaGenerator;
import scala.Some;
import scala.Tuple3;
import scala.collection.immutable.List;
import scala.runtime.AbstractFunction1;

public class JavaCodeGenerator extends BasicJavaGenerator {

    public static final String BASE_PACKAGE = "io.lumify.web.clientapi.codegen";

    public static void main(String[] args) {
        new JavaCodeGenerator().generateClient(args);
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
                return item._1().contains("apiInvoker") || item._1().contains("pom");
            }
        });
    }
}
