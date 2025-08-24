package edu.eci.arep.httpserver;

public interface Service {
    HttpResponse executeService (HttpRequest req , HttpResponse res);
}
