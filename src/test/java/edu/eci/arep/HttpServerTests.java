package edu.eci.arep;
import edu.eci.arep.classes.Task;
import org.junit.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.eci.arep.connection.URLConnection;

import static edu.eci.arep.classes.TaskManager.getTaskManager;
import static edu.eci.arep.httpserver.HttpServer.*;
import static edu.eci.arep.httpserver.HttpServer.get;
import static org.junit.Assert.*;

public class HttpServerTests {
    private static Thread serverThread;
    private static final int port = 35001;
    static URLConnection urlConnection;
    @BeforeClass
    /*
    *We define other lambda expressions to make tests
     */
    public static void setUp() throws Exception {
        staticfiles("/resources");
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

        //Send a json
        get("/json", (req, res) -> {
            return res.body("{\"status\":\"ok\"}")
                    .contentType("text/html");
        });
        //Get the square of a number
        get("/square", (req, res) -> {
            String val = req.getValues("n");
            if (val == null) {
                res.status(400);
                return res.body("Missing query param 'n'")
                        .contentType("text/plain");
            }
            int n = Integer.parseInt(val);
            return res.body("Square: " + (n * n));
        });
        //Return OK without header
        get("/status",(req,res) -> {
            return res.body("No status");
        });
        //Return No Content without status and body
        get("/empty", (req, res) -> {
            return res;
        });
        //Return Other status different to 200 OK without body
        get("/emptyBody", (req, res) -> {
            return res.status(500);
        });

        //start the server
        urlConnection = new URLConnection(port);
        serverThread = new Thread(() -> {
            try { start(port); } catch (Exception ignored) {}
        }, "http-server-test");
        serverThread.start();
        Thread.sleep(150);
    }
    @AfterClass
    public static void tearDown() throws Exception {
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }
    /*
     *Returns index.html file
     */
    @Test
    public void testGetHtmlFile() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/");
        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 200 OK",200, responseCode);
        getConnection.disconnect();
        HttpURLConnection getConnectionWithFile = urlConnection.createGetConnection("/index.html");
        int responseCodeWithFile = getConnectionWithFile.getResponseCode();
        assertEquals("Should return 200 OK",200, responseCodeWithFile);
        getConnectionWithFile.disconnect();
    }
    /*
     *Returns style.css file
     */
    @Test
    public void testGetCssFile() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/styles/style.css");
        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 200 OK",200, responseCode);
        getConnection.disconnect();
    }
    /*
     *Returns script.js file
     */
    @Test
    public void testGetJsFile() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/scripts/script.js");
        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 200 OK",200, responseCode);
        getConnection.disconnect();
    }
    /*
     *Returns image file
     */
    @Test
    public void testGetImageFile() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/images/logo.png");
        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 200 OK",200, responseCode);
        getConnection.disconnect();
    }
    /*
     *Trying to get an unkown file
     */
    @Test
    public void testGetUnkownFile() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/prueba.html");
        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 404 Not Found",404, responseCode);
        getConnection.disconnect();
    }

    /*
     *Get all tasks successfully
     * In the lambda, we did not specify the content-type and the status 200 OK,
     * the server should return automatically 200 OK and the header content-type : application/json automatically with the
     * list of tasks
     */
    @Test
    public void testGetTasks() throws Exception {
        String jsonPayload = "{\"name\":\"Get Test Task\",\"description\":\"For GET test\"}";
        HttpURLConnection postConnection = urlConnection.createPostConnection("/app/saveTask", jsonPayload);
        postConnection.getResponseCode(); // Ejecutar POST
        postConnection.disconnect();

        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/tasks?name=All");

        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 200 OK",200, responseCode);

        String responseBody = urlConnection.readResponse(getConnection);
        assertTrue("Response should contain tasks",responseBody.length() > 0);

        String responseHeader = getConnection.getHeaderField("Content-Type");
        assertEquals("Should return application/json",responseHeader,"application/json");
        getConnection.disconnect();
    }
    /*
     *Get tasks by filter name successfully
     *
     */
    @Test
    public void testGetTasksByName() throws Exception {
        String jsonPayload = "{\"name\":\"Task GET\",\"description\":\"For GET test\"}";
        HttpURLConnection postConnection = urlConnection.createPostConnection("/app/save", jsonPayload);
        postConnection.getResponseCode();
        postConnection.disconnect();

        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/tasks?name=Task%20GET");

        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 200 OK",200, responseCode);

        String responseBody = urlConnection.readResponse(getConnection);
        String[] inner = responseBody.trim().substring(1, responseBody.length()-1).trim().split("},");
        assertTrue("Response should contain one task",inner.length == 1);;
        getConnection.disconnect();
    }

    /*
     *Trying to get tasks by sending an empty parameter
     * the server should return automatically 400 Bad Request, the header content-type : text/plain
     * and the error message
     */
    @Test
    public void testGetTasksEmptyParam() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/tasks?name=");

        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 400 Bad Request",400, responseCode);
        String responseBody = urlConnection.readResponse(getConnection);
        assertTrue("Response should contain error about missing parameter",
                responseBody.toLowerCase().contains("filter"));
        String responseHeader = getConnection.getHeaderField("content-type");
        assertEquals("Should return text/plain",responseHeader,"text/plain");
        getConnection.disconnect();
    }

    /*
     *Send an invalid method
     */
    @Test
    public void testMethodNotAllowed() throws Exception {
        HttpURLConnection connection = urlConnection.createConnection("/app/save", "PUT");

        int responseCode = connection.getResponseCode();
        assertEquals("Should return 405 Method Not Allowed",405, responseCode);

        connection.disconnect();
    }
    /*
    *Testing the /json lambda
    * In this case we specify the content-type,
    * the server should respect it and not set the content-type automatically
     */
    @Test
    public void testGetJson() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/json");
        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 200 OK",200, responseCode);
        String responseHeader = getConnection.getHeaderField("Content-Type");
        assertEquals("Should return text/html",responseHeader,"text/html");
        getConnection.disconnect();
    }
    /*
    *Testing the /square lambda, should return the square of 5 and 200 OK
     */
    @Test
    public void testGetSquare() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/square?n=5");
        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 200 OK",200, responseCode);
        String responseBody = urlConnection.readResponse(getConnection);
        assertTrue("Response should contain 25",
                responseBody.toLowerCase().contains("25"));
        getConnection.disconnect();
    }
    /*
     *Testing the /square lambda without param, should return 400 bad request
     */
    @Test
    public void testGetSquareParam() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/square");
        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 400 Bad Request",400, responseCode);
        getConnection.disconnect();
    }

    /*
    *Testing sending a response without specify content-type,
    * The server should assign automatically the content-type based on the body object
    * In this case the content-type should be 'text/plain'
     */
    @Test
    public void testHeader() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/status");
        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 200 OK",200, responseCode);
        String responseBody = urlConnection.readResponse(getConnection);
        assertEquals("Should have the body message",responseBody,"No status");
        String responseHeader = getConnection.getHeaderField("Content-Type");
        assertEquals("Should return text/plain",responseHeader,"text/plain");
    }

    /*
     *Testing sending a response without specify content-type and body,
     * The server should assign automatically the content-type based on the body object
     * In this case the content-type should be 'text/plain'.
     * When there is an empty body and the status is 200 OK, automatically send 204 No content
     * Should not have body
     */
    @Test
    public void testEmpty() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/empty");
        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 204 No content",204, responseCode);
        String responseHeader = getConnection.getHeaderField("Content-Type");
        assertEquals("Should return text/plain",responseHeader,"text/plain");
    }
    /*
     *Testing sending another status without body
     * In this case, the server should return the status and empty body
     */
    @Test
    public void testEmptyBody() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/emptyBody");
        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 500 Internal Server",500, responseCode);
        String responseHeader = getConnection.getHeaderField("Content-Type");
        assertEquals("Should return text/plain",responseHeader,"text/plain");
    }

}
