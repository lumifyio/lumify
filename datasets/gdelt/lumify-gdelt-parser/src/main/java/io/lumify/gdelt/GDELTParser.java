package io.lumify.gdelt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GDELTParser {
    private Map<String, Integer> headerMapping = new HashMap<String, Integer>();

    public GDELTParser() {
        loadHeaderMapping("CSV.header.fieldids.txt");
    }

    private void loadHeaderMapping(String fileName) {
        InputStream is = GDELTParser.class.getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\t");
                headerMapping.put(fields[0].trim(), Integer.parseInt(fields[1].trim()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                System.err.println("Failed to close reader: " + e.toString());
            }
        }
    }

    public GDELTEvent parseLine(String gdeltTabDelimitedLine) throws ParseException {
        String[] columns = gdeltTabDelimitedLine.split("\\t");
        GDELTEvent event = new GDELTEvent();

        List<Method> methods = getGDELTMethods(GDELTEvent.class);
        for (Method method : methods) {
            method.setAccessible(true);

            GDELTField annotation = method.getAnnotation(GDELTField.class);
            int index = headerMapping.get(annotation.name());
            if (index > columns.length - 1) {
                throw new ParseException("Annotated field index is beyond source column array index", index);
            }

            String column = columns[index];
            if (column == null || column.trim().equals("")) {
                if (annotation.required()) {
                    throw new ParseException(annotation.name() + " field is required", index);
                }
                continue;
            }
            column = column.trim();

            Class type = method.getParameterTypes()[0];

            try {
                if (type.equals(String.class)) {
                    method.invoke(event, column);
                } else if (type.equals(Date.class)) {
                    String formatString = annotation.dateFormat();
                    if (formatString == null) {
                        throw new ParseException("Date type requires dataFormat annotation parameter", index);
                    }
                    Date date = new SimpleDateFormat(formatString).parse(column);
                    method.invoke(event, date);
                } else if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
                    method.invoke(event, Integer.parseInt(column));
                } else if (type.equals(Double.class) || type.equals(Double.TYPE)) {
                    method.invoke(event, Double.parseDouble(column));
                } else if (type.equals(Boolean.class) || type.equals(Boolean.TYPE)) {
                    method.invoke(event, column.equals("1"));
                } else {
                    throw new ParseException(type + " is not supported", index);
                }
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        }

        return event;
    }

    private List<Method> getGDELTMethods(Class<GDELTEvent> clazz) {
        List<Method> gdeltMethods = new ArrayList<Method>();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getAnnotation(GDELTField.class) != null) {
                gdeltMethods.add(method);
            }
        }

        return gdeltMethods;
    }

}