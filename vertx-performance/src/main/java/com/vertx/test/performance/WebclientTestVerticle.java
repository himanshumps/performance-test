package com.vertx.test.performance;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;

import java.util.concurrent.atomic.AtomicInteger;

public class WebclientTestVerticle extends AbstractVerticle {
  private WebClient webClient;
  private static final AtomicInteger atomicCounter = new AtomicInteger(0);
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    webClient = WebClient.create(vertx, new WebClientOptions().setDefaultHost(config().getString("hostname")).setDefaultPort(config().getInteger("port")).setProtocolVersion(HttpVersion.HTTP_1_1).setHttp2ClearTextUpgrade(false));
    vertx.eventBus().<JsonObject>consumer("START_TEST", this::startTest);
    atomicCounter.compareAndSet(0, config().getInteger("numberOfRequests"));
    startPromise.complete();
  }

  private void startTest(Message<JsonObject> jsonObjectMessage) {
    webClient.get("/hello")
      .as(BodyCodec.none())
      .send()
      .onComplete(httpResponseAsyncResult -> {
        if(atomicCounter.decrementAndGet() >= 0) {
          startTest(null);
        } else {
          vertx.eventBus().send("TEST_COMPLETED", JsonObject.of());
        }
      });
  }
}
