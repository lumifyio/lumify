package io.lumify.web.clientapi.codegen;

import com.wordnik.swagger.codegen.BasicJavaGenerator;
import scala.Some;
import scala.Tuple3;
import scala.collection.immutable.List;
import scala.runtime.AbstractFunction1;

public class JavaCodeGenerator extends BasicJavaGenerator {
    public static void main(String[] args) {
        new JavaCodeGenerator().generateClient(args);
    }

    @Override
    public String destinationDir() {
        return "target/generated-sources";
    }

    @Override
    public Some<String> apiPackage() {
        return new Some("io.lumify.web.clientapi");
    }

    @Override
    public Some<String> invokerPackage() {
        return new Some("io.lumify.web.clientapi");
    }

    @Override
    public Some<String> modelPackage() {
        return new Some("io.lumify.web.clientapi.model");
    }

    @Override
    public List<Tuple3<String, String, String>> supportingFiles() {
        return (List<Tuple3<String, String, String>>) super.supportingFiles().filterNot(new AbstractFunction1<Tuple3<String, String, String>, Object>() {
            @Override
            public Object apply(Tuple3<String, String, String> item) {
                return item._1().contains("apiInvoker");
            }
        });
    }
}
