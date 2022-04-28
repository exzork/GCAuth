package me.exzork.gcauth.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import emu.grasscutter.Grasscutter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

public abstract class AbstractHandler implements HttpHandler {
    void responseJSON(HttpExchange t, Object data) throws IOException {
        // Create a response
        String response = Grasscutter.getGsonFactory().toJson(data);
        // Set the response header status and length
        t.getResponseHeaders().put("Content-Type", Collections.singletonList("application/json"));
        t.sendResponseHeaders(200, response.getBytes().length);
        // Write the response string
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
