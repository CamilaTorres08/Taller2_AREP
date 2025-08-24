package edu.eci.arep;

import edu.eci.arep.classes.Task;
import edu.eci.arep.classes.TaskManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static edu.eci.arep.classes.TaskManager.getTaskManager;
import static edu.eci.arep.httpserver.HttpServer.*;

public class Main {
    public static void main(String[] args) {
        try{
            //set the directory of static files
            staticfiles("/resources");
            //value of pi
            get("/pi", (req, resp) -> {
                return resp.body(String.valueOf(Math.PI)).contentType("text/html");
            });
            //greet
            get("/hello", (req, resp) -> {
                String value = req.getValues("name");
                if(value == null){
                    return resp.body("hello world!").contentType("text/html");
                }
                return resp.body("Hello " + value).contentType("text/html");
            });
            //return list of tasks
            get("/tasks", (req, res) -> {
                String param = req.getValues("name");
                if (param == null) {
                    return res.status(400).body("Missing filter parameter");
                }
                List<Task> tasks;
                if(!param.equals("All")){
                    tasks = getTaskManager().getTasksByName(param);
                }else{
                    tasks = getTaskManager().getTasks();
                }
                return res.body(tasks);
            });
            //return first n numbers in a list
            get("/numbers", (request, response) -> {
                String param = request.getValues("n");
                if(param == null){
                    return response.status(400).body("Missing n parameter");
                }
                List<Integer> list = new ArrayList<>();
                for(int i=1;i<=Integer.parseInt(param);i++){
                    list.add(i);
                }
                return response.body(list);
            });

            start();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}