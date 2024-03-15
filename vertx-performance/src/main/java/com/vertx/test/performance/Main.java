package com.vertx.test.performance;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

  public static void main(String[] args) {
    System.setProperty("vertx.disableDnsResolver", "true");
    final String hostname = System.getProperty("hostname", "localhost");
    final int port = Integer.getInteger("port", 3000);
    final int numberOfConnections = Integer.getInteger("numberOfConnections", 20);
    final int numberOfRequests = Integer.getInteger("numberOfRequests",2000000);
    final AtomicInteger oneTime = new AtomicInteger(0);

    Vertx vertx = Vertx.vertx(new VertxOptions());
    // Deploy same number of verticles as the connections needed as each verticle has a webclient with max pool size of 1
    CountDownLatch countDownLatch = new CountDownLatch(numberOfConnections);
    for (int i = 0; i < numberOfConnections; i++) {
      vertx.deployVerticle(WebclientTestVerticle.class, new DeploymentOptions().setConfig(JsonObject.of("hostname", hostname, "port", port, "numberOfRequests", numberOfRequests)))
        .onComplete(handler -> countDownLatch.countDown());
    }
    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    System.out.println(Instant.now() + " | Starting the test");
    final long startTime = System.currentTimeMillis();
    vertx.eventBus().publish("START_TEST", JsonObject.of());
    vertx.eventBus().consumer("TEST_COMPLETED", handler -> {
      if(oneTime.getAndIncrement() == 0) {
        final long endTime = System.currentTimeMillis();
        System.out.println(Instant.now() + " | Test Completed.\nMetrics:");
        System.out.println("Start Time: " + startTime);
        System.out.println("End Time: " + endTime);
        System.out.println("Time taken(ms): " + (endTime - startTime));
        System.out.println("Avg Requests/sec: " + ((numberOfRequests * 1000L) / (endTime - startTime)));
        vertx.close();
      }
    });
  }
}
