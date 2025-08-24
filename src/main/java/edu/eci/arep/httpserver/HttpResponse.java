package edu.eci.arep.httpserver;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpResponse {
    int statusCode=200;
    String statusMessage="OK";
    Map<String, String> headers = new LinkedHashMap<>();
    byte[] body;
    public HttpResponse(int statusCode, Object body) {
        this.statusCode = statusCode;
        this.body = toByte(body);
        setStatusMessage();
    }
    public HttpResponse(){}
    /**
    *Set the status code of the response
     * @param statusCode HTTP code of the response
     * @return HttpResponse object
     */
    public HttpResponse status(int statusCode) {
        this.statusCode = statusCode;
        setStatusMessage();
        return this;
    }
    /**
     *Save headers of the response
     * @param name header name
     * @param value header value
     * @return HttpResponse object
     */
    public HttpResponse header(String name, String value) {
        headers.put(name, value);
        return this;
    }
    /**
     *Set the body of the response
     * @param body object to response
     * @return HttpResponse object
     */
    public HttpResponse body(Object body) {
        this.body = toByte(body);
        return this;
    }

    public int getStatusCode() {
        return statusCode;
    }
    public Map<String, String> getHeaders() {
        return headers;
    }
    public String getStatusMessage() {
        return statusMessage;
    }
    public byte[] getBody() {
        return body;
    }
    /**
     *Set the value of content-type header
     * @param v content-type value
     * @return HttpResponse object
     **/
    public HttpResponse contentType(String v){
        return header("Content-Type", v);
    }
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        setStatusMessage();
    }
    /**
     *Gets the Status Message based on the Status Code
     **/
    public void setStatusMessage() {
        switch (statusCode) {
            case 200:
                statusMessage = "OK";
                break;
            case 204:
                statusMessage = "No Content";
                break;
            case 400:
                statusMessage = "Bad Request";
                break;
            case 401:
                statusMessage = "Unauthorized";
                break;
            case 403:
                statusMessage = "Forbidden";
                break;
            case 404:
                statusMessage = "Not Found";
                break;
            case 405:
                statusMessage = "Method Not Allowed";
                break;
            case 406:
                statusMessage = "Not Acceptable";
                break;
            default:
                statusMessage = "Internal Server Error";
                break;
        }
    }

    /**
     * Converts the body object to bytes
     * @param obj The object to convert
     * @return object converted to byte
     */
    private byte[] toByte(Object obj){
        if(obj instanceof byte[]){
            return (byte[])obj;
        }
        else if(obj instanceof String){
            return ((String)obj).getBytes(StandardCharsets.UTF_8);
        }
        //if the object is type json, and does not have content.type header, set application/json automatically
        headers.putIfAbsent("Content-Type","application/json");
        return toJson(obj).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Builds the json and convert it to string
     * @param obj object to convert
     * @return json in format string
     */
    public String toJson(Object obj) {
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
    private String arrayToJson(Object array) {
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
    private String objectToJson(Object obj) {
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
    private String quote(String s) {
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
