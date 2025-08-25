package edu.eci.arep.httpserver;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    URI uri;
    Map<String, String> parameters = new HashMap<>();
    public HttpRequest(URI requri){
        this.uri = requri;
        if(uri.getQuery() != null) setParamValues();
    }

    /**
     * Stores the parameters
     */
    public void setParamValues(){
        String[] values = uri.getQuery().split("&");
        for(String value : values){
            String[] keyValue = value.split("=");
            if(keyValue.length > 1){
                parameters.put(keyValue[0], keyValue[1]);
            }
        }
    }

    /**
     * Retrieves the value of a query parameter
     * @param paraName the name of the parameter to look up
     * @return the parameter value
     */
    public String getValues(String paraName){
        return parameters.get(paraName);
    }
}
