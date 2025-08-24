package edu.eci.arep.helpers;

import java.lang.reflect.Field;
import java.util.List;

public class JsonConverter {
    /**
     * Builds the json and convert it to string
     * @param obj object to convert
     * @return json in format string
     */
    public static String toJson(Object obj) {
        if(obj instanceof String) return quote((String) obj);
        if(obj instanceof Number) return obj.toString();
        if(obj instanceof List<?>) {
            return arrayToJson(obj);
        }
        return objectToJson(obj);
    }

    /**
     * Converts list to json format
     * @param array
     * @return
     */
    private static String arrayToJson(Object array) {
        StringBuilder sb = new StringBuilder("[");
        List<?> list = (List<?>) array;
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            sb.append(toJson(item));
            if(i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Converts and object to json
     * @param obj
     * @return
     */
    private static String objectToJson(Object obj) {
        StringBuilder sb = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;

        for (Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(obj);

                if (!first) sb.append(",");
                sb.append("\"").append(field.getName()).append("\":");
                sb.append(toJson(value));
                first = false;
            } catch (IllegalAccessException e) {}
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Converts a string to json
     * @param s
     * @return
     */
    private static String quote(String s) {
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
