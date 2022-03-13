package com.vertx;

import com.vertx.database.MongoManager;
import com.vertx.dto.MyItem;
import com.vertx.entity.Product;
import com.vertx.resources.ProductResource;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Application extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private static MongoClient mongoClient = null;

    public static void main(String[] args) {

        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setClustered(true);
        Vertx.clusteredVertx(vertxOptions, results -> {
            if (results.succeeded()) {
                Vertx vertx = results.result();
                ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
                configRetriever.getConfig(config -> {
                    if(config.succeeded()) {
                        JsonObject configJson = config.result();
                        LOGGER.info(configJson.encodePrettily());
                        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(configJson);
                        vertx.deployVerticle(new Application(), deploymentOptions);
                    }
                });
            }
        });
    }

    @Override
    public void start() {
        LOGGER.info("Vertx Started");
        processJson();
        // createBasicWebServer(vertx);
        // createWebServer(vertx);
        createEventBus(vertx);
    }

    public void createEventBus(Vertx vertx) {
        Router router = Router.router(vertx);

        router.get("/mongofind").handler(this::getAllProductsFromMongoDB);

        JsonObject dbConfig = new JsonObject();

        dbConfig.put("connection_string", "mongodb://localhost:27017/Mongotest");
        // Config for authentication
		dbConfig.put("username", "testUser");
		dbConfig.put("password", "test");
		dbConfig.put("authSource", "Mongotest");
        dbConfig.put("useObjectId", true);

        mongoClient = MongoClient.createShared(vertx, dbConfig);

        // if using mongo manager
//        MongoManager mongoManager = new MongoManager(mongoClient);
//
//        mongoManager.registerConsumer(vertx);

        // Consumer for listening events
//		vertx.createHttpServer().requestHandler(router::accept).listen(8080);
//
//		vertx.eventBus().consumer("com.vertx", message -> {
//
//			System.out.println("Recevied message: " + message.body());
//
//			message.reply(new JsonObject().put("responseCode", "OK").put("message", "This is your response to your event"));
//
//		});

        vertx.setTimer(5000, handler ->{
            sendTestEvent();
        });
    }

    private void sendTestEvent() {

        JsonObject testInfo = new JsonObject();

        testInfo.put("info", "Hi");

        System.out.println("Sending message=" + testInfo.toString());

        vertx.eventBus().send("com.vertx", testInfo.toString(), reply -> {
            if (reply.succeeded()) {
                JsonObject replyResults = (JsonObject) reply.result().body();
                System.out.println("Got Reply message=" + replyResults.toString());
            }
        });
    }

    public void createBasicWebServer(Vertx vertx) {
        vertx.createHttpServer()
		.requestHandler(routingContext -> routingContext.response().end("<h1>Welcome to Vert.x Intro</h1>"))
		.listen(8080);
    }

    public void createWebServer(Vertx vertx){
        Router router = Router.router(vertx);

        // Create ProductResource object
        ProductResource productResources = new ProductResource();

        // Map subrouter for Products
        router.mountSubRouter("/api/", productResources.getAPISubRouter(vertx));

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
        vertx.createHttpServer().requestHandler(router::accept).listen(config().getInteger("http.port"), asyncResult -> {
            if (asyncResult.succeeded()) {
                LOGGER.info("HTTP server running on port " + config().getInteger("http.port"));
            }
            else {
                LOGGER.error("Could not start a HTTP server", asyncResult.cause());
            }
        });
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


    // Get all products as array of products
    private void getAllProductsFromMongoDB(RoutingContext routingContext) {
        FindOptions findOptions = new FindOptions();
        //findOptions.setLimit(1);
        mongoClient.findWithOptions("products", new JsonObject(), findOptions, results -> {
            try {
                List<JsonObject> objects = results.result();
                if (objects != null && objects.size() != 0) {
                    System.out.println("Got some data len=" + objects.size());
                    JsonObject jsonResponse = new JsonObject();
                    jsonResponse.put("products", objects);
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(200)
                            .end(Json.encodePrettily(jsonResponse));
                } else {
                    JsonObject jsonResponse = new JsonObject();
                    jsonResponse.put("error", "No items found");
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(400)
                            .end(Json.encodePrettily(jsonResponse));
                }

            } catch (Exception e) {
                LOGGER.info("getAllProducts Failed exception e=", e.getLocalizedMessage());
                JsonObject jsonResponse = new JsonObject();
                jsonResponse.put("error", "Exception and No items found");
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .setStatusCode(500)
                        .end(Json.encodePrettily(jsonResponse));
            }
        });
    }

    @Override
    public void stop() {
        LOGGER.debug("Vertx Stopped");
    }
}
