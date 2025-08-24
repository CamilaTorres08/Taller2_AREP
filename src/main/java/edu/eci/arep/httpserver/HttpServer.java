package edu.eci.arep.httpserver;

import edu.eci.arep.classes.Task;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static edu.eci.arep.classes.TaskManager.getTaskManager;

public class HttpServer {
    static Map<String,Service> services = new HashMap<>();
    static String dir;
    static int port = 35000;
    public static void start(int serverPort) throws Exception {
        port = serverPort;
        start();
    }
    public static void start() throws Exception {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        }
        Socket clientSocket = null;
        Boolean running = true;
        while (running) {
            try {
                System.out.println("Listo para recibir ...");
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()));
            OutputStream outputStream = clientSocket.getOutputStream();
            String inputLine, firstLine="";
            boolean isFirstLine = true;
            int contentLength = 0;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received: " + inputLine);
                if (isFirstLine) {
                    isFirstLine = false;
                    firstLine = inputLine;
                }
                if (inputLine.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(inputLine.split(":")[1].trim());
                }
                if (inputLine.isEmpty() || inputLine.trim().isEmpty()) {
                    break;
                }
            }
            String body = "";
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                in.read(bodyChars, 0, contentLength);
                body = new String(bodyChars);
            }
            if(!firstLine.isEmpty()) manageRequest(firstLine,body,outputStream);
            outputStream.close();
            in.close();
            clientSocket.close();
        }
        serverSocket.close();
    }
    /**
     * Manages an HTTP request by processing the method, resource, and body,
     * and writing the corresponding response.
     * @param inputLine the first line of the HTTP request (contains method and resource)
     * @param body      the body of the request, if present
     * @param out       the output stream used to send the response back to the client
     * @throws IOException if an error occurs while writing to the output stream
     */
    private static void manageRequest(String inputLine, String body,OutputStream out) throws IOException {
        HttpResponse response = new HttpResponse();
        try {
            String[] dividedUri = inputLine.split(" ");
            URI requestUri = new URI(dividedUri[1]);
            String path = requestUri.getPath();
            String method = dividedUri[0];
            if(method.equals("GET") && path.startsWith("/app")) {
                response = processRequest(requestUri);
            }else if(method.equals("POST") && path.startsWith("/app")){
                    String taskName = "";
                    String taskDescription = "";
                    String[] values = body.split(",");
                    for(String value : values){
                        String[] pair = value.split(":",2);
                        String key = pair[0].replace("\"","").replace("{","").replace("}","").replace(" ","").trim();
                        String val = pair[1].replace("\"","").replace("{","").replace("}","").trim();
                        if(key.equals("name")) taskName = val;
                        if(key.equals("description")) taskDescription = val;
                    }
                    if(!taskName.isEmpty() && !taskDescription.isEmpty()) response = saveTask(taskName, taskDescription);
                    else response = new HttpResponse(400,"Missing values, Task Name and Task Description are required");

            }else if(method.equals("GET") && (path.equals("/") || path.endsWith("html") || path.endsWith("js") || path.endsWith("css")
                    || path.endsWith("png") || path.endsWith("jpg") || path.endsWith("jpeg"))){
                response = getResources(path);
            }else{
                response = new HttpResponse(405,"Method "+method+" "+path+" not supported");
            }
        }catch (FileNotFoundException e){
            response = new HttpResponse(404,e.getMessage());
        }catch (Exception e) {
            response = new HttpResponse(500,e.getMessage());
        }finally {
            //if the response does not have content-type assign automatically text/plain
            response.getHeaders().putIfAbsent("Content-Type","text/plain");
            byte[] bodyResponse = response.getBody();
            //if response does not have body set status No Content
            if(bodyResponse == null && response.getStatusCode() == 200) response.setStatusCode(204);
            //Build full response
            StringBuilder sb = new StringBuilder()
                    .append("HTTP/1.1 ").append(response.getStatusCode()).append(" ").append(response.getStatusMessage()).append("\r\n");
            response.getHeaders().forEach((k,v) -> sb.append(k).append(": ").append(v).append("\r\n"));
            sb.append("\r\n");
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            //if response have body include it
            if(bodyResponse != null) out.write(response.getBody());
            out.flush();
        }
    }
    /**
    *Manages GET methods
    * @param  route The resource
     * @param service the function to execute
     */
    public static void get(String route, Service service){
        services.put(route,service);
    }
    /**
    *Set the directory of the files
     * @param path the path where the files will save
     * @throws IOException if an error occurs while creating the directory
     * @throws IllegalArgumentException if the path is not correct
     **/
    public static void staticfiles(String path) throws IOException, IllegalArgumentException {
        if(path == null || path.isEmpty()) throw new IllegalArgumentException("Static Files: path cannot be null/blank");
        String root = "src/main";
        Path configured = Paths.get(root + path);

        if (Files.exists(configured)) {
            if (!Files.isDirectory(configured)) {
                throw new IllegalArgumentException("staticfiles: no es un directorio: " + configured);
            }
            if (!Files.isReadable(configured)) {
                throw new IllegalArgumentException("staticfiles: directorio no legible: " + configured);
            }
        } else {
            Files.createDirectories(configured);
        }
        dir = root + path;
    }

    /**
     * Process the GET Request
     * @param requestURI the resource
     * @return Response
     */
    private static HttpResponse processRequest(URI requestURI) {
        String serviceRoute = requestURI.getPath().substring(4);
        Service service = services.get(serviceRoute);
        HttpRequest req = new HttpRequest(requestURI);
        HttpResponse res = new HttpResponse();
        if(service == null) return res.status(405).body("Method GET "+requestURI+" not supported");
        return service.executeService(req, res);
    }

    /**
     * Manage disk files.
     *
     * @param path resource of the request
     * @throws IOException if an error occurs while writing to the output stream
     * @return Response
     */
    private static HttpResponse getResources(String path) throws IOException {
        String fullPath = dir;
        if(path.equals("/")){
            fullPath += "/" + "pages/index.html";
        }
        else if(path.endsWith("html")){
            fullPath += "/" + "pages" + path;
        }else {
            fullPath += path;
        }
        if(fullPath.endsWith("html") || fullPath.endsWith("css") || fullPath.endsWith("js")){
            return sendTextFile(fullPath);
        }
        return sendImageFile(fullPath);
    }
    /**
     * Read text files (html, css and javascript).
     *
     * @param filePath full path of the file
     * @throws IOException if an error occurs while reading the file
     * @return File content
     */
    private static String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    /**
     * Gets the header based on the file extension.
     * @param path full path of the file
     * @return content-type header
     */
    private static String getHeader(String path){
        if (path.endsWith(".html") || path.endsWith(".htm")) return "text/html";
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".js"))   return "application/javascript";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
    /**
     * Read and send the file text (html, css or javascript)
     * @param fullPath full path of the file
     * @throws IOException if an error occurs while writing to the output stream
     * @return Response
     */
    private static HttpResponse sendTextFile(String fullPath)throws IOException{
        byte[] output = readFile(fullPath).getBytes(StandardCharsets.UTF_8);
        return new HttpResponse(200,output).contentType(getHeader(fullPath));
    }
    /**
     * Read and send images (png, jpg or jpeg)
     * @param fullPath full path of the file
     * @throws IOException if an error occurs while reading the file image
     * @return Response
     */
    private static HttpResponse sendImageFile(String fullPath)throws IOException {
        Path filePath = Paths.get(fullPath);
        if (!Files.exists(filePath)) {
            return new HttpResponse(404,"Image not found");
        }
        byte[] fileContent = Files.readAllBytes(filePath);
        return new HttpResponse(200,fileContent)
                .contentType(getHeader(fullPath))
                .header("Content-Length",String.valueOf(fileContent.length));
    }
    /**
     * Save a task in memory, by post request is not implemented using lambdas
     * we use singletton pattern to obtain the instance of taskManager and save the task
     * @param name name of the task
     * @param description description of the task
     * @return Request
     */
    private static HttpResponse saveTask(String name, String description){
        Task newTask = getTaskManager().addTask(name,description);
        return new HttpResponse(200,newTask).contentType("application/json");
    }

}
