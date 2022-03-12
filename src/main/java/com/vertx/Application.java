package com.vertx;

import com.vertx.dto.MyItem;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class Application extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Application());
    }

    @Override
    public void start() {
        LOGGER.info("Vertx Started");
        processJson();
        // createBasicWebServer(vertx);
        createWebServer(vertx);
    }

    public void createBasicWebServer(Vertx vertx) {
        vertx.createHttpServer()
		.requestHandler(routingContext -> routingContext.response().end("<h1>Welcome to Vert.x Intro</h1>"))
		.listen(8080);
    }

    public void createWebServer(Vertx vertx){
        Router router = Router.router(vertx);

        router.get("/yo.html").handler(routingContext -> {

            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("webroot/yo.html").getFile());

            String mappedHTML = "";

            try {
                StringBuilder result = new StringBuilder("");
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    result.append(line).append("\n");
                }
                scanner.close();
                mappedHTML = result.toString();
                mappedHTML = replaceAllTokens(mappedHTML, "{name}", "Programmer");

            } catch (IOException e) {
                e.printStackTrace();
            }
            routingContext.response().putHeader("content-type", "text/html").end(mappedHTML);
        });

        // Default if no routes are matched
        router.route().handler(StaticHandler.create().setCachingEnabled(false));
        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }

    public String replaceAllTokens(String input, String token, String newValue) {
        String output = input;
        while (output.indexOf(token) != -1) {
            output = output.replace(token, newValue);
        }
        return output;
    }

    private static void processJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("name", "Programmer1").put("description", "A full stack developer");

        LOGGER.info(jsonObject.getString("name"));
        LOGGER.info(jsonObject.toString());

        MyItem myitem = new MyItem();
        myitem.setName("Programmer1");
        myitem.setDescription("Front end developer");

        MyItem myItem2 = jsonObject.mapTo(MyItem.class);
        LOGGER.info("Name: " + myItem2.getName() + "Description: "+ myItem2.getDescription());

        JsonObject jsonObject2  = JsonObject.mapFrom(myitem);
        LOGGER.info(jsonObject2.toString());
    }

    @Override
    public void stop() {
        LOGGER.debug("Vertx Stopped");
    }
}
