package io.lumify.palantir.dataImport.sqlrunner;

import io.lumify.core.exception.LumifyException;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SqlRunnerQueryIterable<T> implements Iterable<T> {
    private final ResultSet rs;
    private final Class<T> clazz;
    private final Map<String, Method> setters = new HashMap<String, Method>();
    private final Map<String, Method> columnNameToSetter = new HashMap<String, Method>();
    private Method[] columnSetters;
    private int columnCount;

    public SqlRunnerQueryIterable(ResultSet rs, Class<T> clazz) throws SQLException {
        this.rs = rs;
        this.clazz = clazz;

        for (Method m : clazz.getMethods()) {
            if ((m.getName().startsWith("set") || m.getName().startsWith("is")) && m.getParameterTypes().length == 1) {
                setters.put(m.getName().substring("set".length()), m);
            }
        }

        columnCount = rs.getMetaData().getColumnCount();
        columnSetters = new Method[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            columnSetters[i - 1] = getSetter(rs.getMetaData().getColumnName(i));
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private T next;
            private T current;

            @Override
            public boolean hasNext() {
                loadNext();
                return next != null;
            }

            @Override
            public T next() {
                loadNext();
                if (this.next == null) {
                    throw new IllegalStateException("iterable doesn't have a next element");
                }
                this.current = this.next;
                this.next = null;
                return this.current;
            }

            private void loadNext() {
                if (this.next != null) {
                    return;
                }

                try {
                    if (rs.next()) {
                        this.next = toClass(rs, clazz);
                    }
                } catch (Exception e) {
                    throw new LumifyException("Could not fetch next", e);
                }
            }

            @Override
            public void remove() {
                throw new LumifyException("not supported");
            }
        };
    }

    private T toClass(ResultSet rs, Class<T> clazz) throws Exception {
        T obj = clazz.newInstance();
        for (int i = 1; i <= columnCount; i++) {
            Method setter = columnSetters[i - 1];
            Object columnValue = rs.getObject(i);
            setClassProperty(obj, setter, columnValue);
        }
        return obj;
    }

    private void setClassProperty(T obj, Method setter, Object columnValue) throws InvocationTargetException, IllegalAccessException {
        try {
            Class<?> parameterType = setter.getParameterTypes()[0];
            if (columnValue instanceof BigDecimal && (parameterType == long.class || parameterType == Long.class)) {
                columnValue = ((BigDecimal) columnValue).longValue();
            } else if (columnValue instanceof BigDecimal && parameterType == boolean.class) {
                BigDecimal bd = (BigDecimal) columnValue;
                if (bd.longValue() == 0) {
                    columnValue = false;
                } else if (bd.longValue() == 1) {
                    columnValue = true;
                }
            } else if (columnValue instanceof Clob && parameterType == String.class) {
                Clob clob = (Clob) columnValue;
                columnValue = IOUtils.toString(clob.getAsciiStream());
            } else if (columnValue instanceof Blob && parameterType == byte[].class) {
                Blob blob = (Blob) columnValue;
                columnValue = IOUtils.toByteArray(blob.getBinaryStream());
            } else if (columnValue instanceof Blob && parameterType == InputStream.class) {
                Blob blob = (Blob) columnValue;
                columnValue = blob.getBinaryStream();
            }
            setter.invoke(obj, columnValue);
        } catch (Throwable ex) {
            throw new LumifyException("Could not call setter " + setter + " on obj " + obj + " with value " + columnValue + " (" + (columnValue == null ? "null" : columnValue.getClass().getName()) + ")", ex);
        }
    }

    private Method getSetter(String columnName) {
        Method setter = columnNameToSetter.get(columnName);
        if (setter != null) {
            return setter;
        }

        String setterName = toSetterName(columnName);
        setter = setters.get(setterName);
        if (setter != null) {
            columnNameToSetter.put(columnName, setter);
            return setter;
        }

        throw new LumifyException("Could not find setter on " + clazz.getName() + " for column name " + columnName);
    }

    private String toSetterName(String columnName) {
        StringBuilder result = new StringBuilder();
        columnName = columnName.toLowerCase();
        for (int i = 0; i < columnName.length(); i++) {
            char ch = columnName.charAt(i);
            if (ch == '_') {
                i++;
                ch = Character.toUpperCase(columnName.charAt(i));
                result.append(ch);
            } else {
                if (i == 0) {
                    ch = Character.toUpperCase(ch);
                }
                result.append(ch);
            }
        }
        return result.toString();
    }
}
