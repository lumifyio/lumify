package io.lumify.javaCodeIngest;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorker;
import io.lumify.core.model.properties.LumifyProperties;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;
import org.securegraph.*;

import java.io.InputStream;

import static org.securegraph.util.IterableUtils.singleOrDefault;

public class ClassFileGraphPropertyWorker extends GraphPropertyWorker {
    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        LumifyProperties.MIME_TYPE.setProperty(data.getElement(), "application/x-java-class", data.createPropertyMetadata(), data.getVisibility(), getAuthorizations());

        Vertex jarVertex = singleOrDefault(((Vertex) data.getElement()).getVertices(Direction.BOTH, Ontology.EDGE_LABEL_JAR_CONTAINS, getAuthorizations()), null);

        String fileName = LumifyProperties.FILE_NAME.getPropertyValue(data.getElement());

        JavaClass javaClass = new ClassParser(in, fileName).parse();
        ConstantPoolGen constants = new ConstantPoolGen(javaClass.getConstantPool());

        Vertex classVertex = createClassVertex(javaClass, data);
        if (jarVertex != null) {
            getGraph().addEdge(jarVertex, classVertex, Ontology.EDGE_LABEL_JAR_CONTAINS, data.getProperty().getVisibility(), getAuthorizations());
        }

        for (Method method : javaClass.getMethods()) {
            createMethodVertex(method, classVertex, javaClass, constants, data);
        }
        for (Field field : javaClass.getFields()) {
            createFieldVertex(field, classVertex, javaClass, data);
        }

        getGraph().flush();
    }

    private Vertex createClassVertex(JavaClass javaClass, GraphPropertyWorkData data) {
        String className = javaClass.getClassName();
        VertexBuilder classVertexBuilder = createClassVertexBuilder(className, data);
        if (javaClass.isInterface()) {
            LumifyProperties.CONCEPT_TYPE.setProperty(classVertexBuilder, Ontology.CONCEPT_TYPE_INTERFACE, data.getProperty().getVisibility());
        } else {
            LumifyProperties.CONCEPT_TYPE.setProperty(classVertexBuilder, Ontology.CONCEPT_TYPE_CLASS, data.getProperty().getVisibility());
        }
        Vertex classVertex = classVertexBuilder.save(getAuthorizations());

        String containsClassEdgeId = JavaCodeIngestIdGenerator.createFileContainsClassEdgeId((Vertex) data.getElement(), classVertex);
        getGraph().addEdge(containsClassEdgeId, (Vertex) data.getElement(), classVertex, Ontology.EDGE_LABEL_CLASS_FILE_CONTAINS_CLASS, data.getProperty().getVisibility(), getAuthorizations());

        return classVertex;
    }

    private Vertex createClassVertex(String className, GraphPropertyWorkData data) {
        VertexBuilder classVertexBuilder = createClassVertexBuilder(className, data);
        return classVertexBuilder.save(getAuthorizations());
    }

    private VertexBuilder createClassVertexBuilder(String className, GraphPropertyWorkData data) {
        int i;
        while ((i = className.lastIndexOf('[')) > 0) {
            className = className.substring(0, i);
        }
        String classId = JavaCodeIngestIdGenerator.createClassId(className);
        VertexBuilder vertexBuilder = getGraph().prepareVertex(classId, data.getVisibility());
        data.setVisibilityJsonOnElement(vertexBuilder);
        LumifyProperties.TITLE.setProperty(vertexBuilder, classNameToTitle(className), data.createPropertyMetadata(), data.getVisibility());
        Ontology.CLASS_NAME.setProperty(vertexBuilder, className, data.createPropertyMetadata(), data.getVisibility());
        return vertexBuilder;
    }

    private String classNameToTitle(String className) {
        int i = className.lastIndexOf('.');
        if (i < 0) {
            return className;
        }
        return className.substring(i + 1);
    }

    private void createMethodVertex(Method method, Vertex classVertex, JavaClass javaClass, ConstantPoolGen constants, GraphPropertyWorkData data) {
        String methodId = JavaCodeIngestIdGenerator.createMethodId(javaClass, method);
        VertexBuilder vertexBuilder = getGraph().prepareVertex(methodId, data.getVisibility());
        data.setVisibilityJsonOnElement(vertexBuilder);
        LumifyProperties.TITLE.setProperty(vertexBuilder, method.getName() + method.getSignature(), data.createPropertyMetadata(), data.getVisibility());
        LumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, Ontology.CONCEPT_TYPE_METHOD, data.createPropertyMetadata(), data.getVisibility());
        Vertex methodVertex = vertexBuilder.save(getAuthorizations());

        String classContainsMethodEdgeId = JavaCodeIngestIdGenerator.createClassContainsMethodEdgeId(classVertex, methodVertex);
        Edge edge = getGraph().addEdge(classContainsMethodEdgeId, classVertex, methodVertex, Ontology.EDGE_LABEL_CLASS_CONTAINS, data.getVisibility(), getAuthorizations());
        data.setVisibilityJsonOnElement(edge, getAuthorizations());

        // return type
        if (!method.getReturnType().toString().equals("void")) {
            Vertex returnTypeVertex = createClassVertex(method.getReturnType().toString(), data);
            String returnTypeEdgeId = JavaCodeIngestIdGenerator.createReturnTypeEdgeId(methodVertex, returnTypeVertex);
            edge = getGraph().addEdge(returnTypeEdgeId, methodVertex, returnTypeVertex, Ontology.EDGE_LABEL_METHOD_RETURN_TYPE, data.getVisibility(), getAuthorizations());
            data.setVisibilityJsonOnElement(edge, getAuthorizations());
            createClassReferencesEdge(classVertex, returnTypeVertex, data);
        }

        // arguments
        for (int i = 0; i < method.getArgumentTypes().length; i++) {
            Type argumentType = method.getArgumentTypes()[i];
            String argumentName = "arg" + i;
            Vertex argumentTypeVertex = createClassVertex(argumentType.toString(), data);
            String argumentEdgeId = JavaCodeIngestIdGenerator.createArgumentEdgeId(methodVertex, argumentTypeVertex, argumentName);
            edge = getGraph().addEdge(argumentEdgeId, methodVertex, argumentTypeVertex, Ontology.EDGE_LABEL_METHOD_ARGUMENT, data.getVisibility(), getAuthorizations());
            data.setVisibilityJsonOnElement(edge, getAuthorizations());
            Ontology.ARGUMENT_NAME.setProperty(edge, argumentName, data.createPropertyMetadata(), data.getVisibility(), getAuthorizations());
            createClassReferencesEdge(classVertex, argumentTypeVertex, data);
        }

        // method invokes
        MethodGen mg = new MethodGen(method, javaClass.getClassName(), constants);
        if (mg.isAbstract() || mg.isNative()) {
            return;
        }
        ConstantPoolGen constantPool = mg.getConstantPool();
        for (InstructionHandle ih = mg.getInstructionList().getStart(); ih != null; ih = ih.getNext()) {
            Instruction i = ih.getInstruction();
            if (i instanceof InvokeInstruction) {
                InvokeInstruction ii = (InvokeInstruction) i;
                String methodClassName = ii.getClassName(constantPool);
                String methodName = ii.getMethodName(constantPool);
                String methodSignature = ii.getSignature(constantPool);
                String invokedMethodId = JavaCodeIngestIdGenerator.createMethodId(methodClassName, methodName, methodSignature);
                VertexBuilder invokedMethodVertexBuilder = getGraph().prepareVertex(invokedMethodId, data.getVisibility());
                data.setVisibilityJsonOnElement(invokedMethodVertexBuilder);
                LumifyProperties.TITLE.setProperty(invokedMethodVertexBuilder, method.getSignature(), data.createPropertyMetadata(), data.getVisibility());
                LumifyProperties.CONCEPT_TYPE.setProperty(invokedMethodVertexBuilder, Ontology.CONCEPT_TYPE_METHOD, data.createPropertyMetadata(), data.getVisibility());
                Vertex invokedMethodVertex = invokedMethodVertexBuilder.save(getAuthorizations());

                String methodInvokesMethodEdgeId = JavaCodeIngestIdGenerator.createMethodInvokesMethodEdgeId(methodVertex, invokedMethodVertex);
                edge = getGraph().addEdge(methodInvokesMethodEdgeId, methodVertex, invokedMethodVertex, Ontology.EDGE_LABEL_INVOKED, data.getVisibility(), getAuthorizations());
                data.setVisibilityJsonOnElement(edge, getAuthorizations());

                Vertex invokeMethodClassVertex = createClassVertex(methodClassName, data);
                createClassReferencesEdge(classVertex, invokeMethodClassVertex, data);
            }
        }
    }

    private void createFieldVertex(Field field, Vertex classVertex, JavaClass javaClass, GraphPropertyWorkData data) {
        String fieldId = JavaCodeIngestIdGenerator.createFieldId(javaClass, field);
        VertexBuilder vertexBuilder = getGraph().prepareVertex(fieldId, data.getVisibility());
        data.setVisibilityJsonOnElement(vertexBuilder);
        LumifyProperties.TITLE.setProperty(vertexBuilder, field.getName(), data.createPropertyMetadata(), data.getVisibility());
        LumifyProperties.CONCEPT_TYPE.setProperty(vertexBuilder, Ontology.CONCEPT_TYPE_FIELD, data.createPropertyMetadata(), data.getVisibility());
        Vertex fieldVertex = vertexBuilder.save(getAuthorizations());

        String classContainsFieldEdgeId = JavaCodeIngestIdGenerator.createClassContainsFieldEdgeId(classVertex, fieldVertex);
        Edge edge = getGraph().addEdge(classContainsFieldEdgeId, classVertex, fieldVertex, Ontology.EDGE_LABEL_CLASS_CONTAINS, data.getVisibility(), getAuthorizations());
        data.setVisibilityJsonOnElement(edge, getAuthorizations());

        Vertex fieldTypeVertex = createClassVertex(field.getType().toString(), data);
        String fieldTypeEdgeId = JavaCodeIngestIdGenerator.createFieldTypeEdgeId(fieldVertex, fieldTypeVertex);
        edge = getGraph().addEdge(fieldTypeEdgeId, fieldVertex, fieldTypeVertex, Ontology.EDGE_LABEL_FIELD_TYPE, data.getVisibility(), getAuthorizations());
        data.setVisibilityJsonOnElement(edge, getAuthorizations());
        createClassReferencesEdge(classVertex, fieldTypeVertex, data);
    }

    private void createClassReferencesEdge(Vertex classVertex, Vertex typeVertex, GraphPropertyWorkData data) {
        String classReferencesEdgeId = JavaCodeIngestIdGenerator.createClassReferencesEdgeId(classVertex, typeVertex);
        Edge edge = getGraph().addEdge(classReferencesEdgeId, classVertex, typeVertex, Ontology.EDGE_LABEL_CLASS_REFERENCES, data.getVisibility(), getAuthorizations());
        data.setVisibilityJsonOnElement(edge, getAuthorizations());
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (!property.getName().equals(LumifyProperties.RAW.getPropertyName())) {
            return false;
        }

        String fileName = LumifyProperties.FILE_NAME.getPropertyValue(element);
        if (fileName == null || !fileName.endsWith(".class")) {
            return false;
        }

        return true;
    }
}
