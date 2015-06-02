import org.vertx.scala.core.json._

val obj = Json.obj("service" -> "process", 
"operation" -> "start", 
"id" -> "send-data.send-data-process", 
"data" -> Json.obj("address" -> "my.custom.address", "message" -> "testing jBPM with vert.x by Scala"))

vertx.eventBus.send("org.jbpm:send-data:1.0", obj, { reply: Message[JsonObject] =>
    container.logger.info("Received reply: " + reply.body())
  })