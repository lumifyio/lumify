package io.lumify.javaCodeIngest;

import org.securegraph.Vertex;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class JavaCodeIngestIdGenerator {
    public static String createClassId(String className) {
        return "CLASS_" + className;
    }

    public static String createMethodId(JavaClass javaClass, Method method) {
        return createMethodId(javaClass.getClassName(), method.getName(), method.getSignature());
    }

    public static String createMethodId(String methodClassName, String methodName, String methodSignature) {
        return "METHOD_" + methodClassName + "." + methodName + methodSignature;
    }

    public static String createFieldId(JavaClass javaClass, Field field) {
        return createMethodId(javaClass.getClassName(), field.getName());
    }

    public static String createMethodId(String className, String name) {
        return "FIELD_" + className + "." + name;
    }

    public static String createFileContainsClassEdgeId(Vertex fileVertex, Vertex classVertex) {
        return "FILE_CONTAINS_" + fileVertex.getId() + "-" + classVertex.getId();
    }

    public static String createClassContainsMethodEdgeId(Vertex classVertex, Vertex methodVertex) {
        return "CLASS_CONTAINS_METHOD_" + classVertex.getId() + "-" + methodVertex.getId();
    }

    public static String createClassContainsFieldEdgeId(Vertex classVertex, Vertex fieldVertex) {
        return "CLASS_CONTAINS_FIELD_" + classVertex.getId() + "-" + fieldVertex.getId();
    }

    public static String createClassReferencesEdgeId(Vertex classVertex, Vertex typeVertex) {
        return "CLASS_REFERENCES_" + classVertex.getId() + "-" + typeVertex.getId();
    }

    public static String createMethodInvokesMethodEdgeId(Vertex methodVertex, Vertex invokedMethodVertex) {
        return "METHOD_INVOKES_" + methodVertex.getId() + "-" + invokedMethodVertex.getId();
    }

    public static String createFieldTypeEdgeId(Vertex fieldVertex, Vertex fieldTypeVertex) {
        return "FIELD_TYPE_" + fieldVertex.getId() + "-" + fieldTypeVertex.getId();
    }

    public static String createReturnTypeEdgeId(Vertex methodVertex, Vertex returnTypeVertex) {
        return "RETURN_TYPE_" + methodVertex.getId() + "-" + returnTypeVertex.getId();
    }

    public static String createArgumentEdgeId(Vertex methodVertex, Vertex argumentTypeVertex, String argumentName) {
        return "ARGUMENT_" + methodVertex.getId() + "-" + argumentTypeVertex.getId() + "-" + argumentName;
    }
}
