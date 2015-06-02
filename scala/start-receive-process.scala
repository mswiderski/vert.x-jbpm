import org.vertx.scala.core.json._

val obj = Json.obj("service" -> "process", "operation" -> "start", "id" -> "receive-data.receive-data-process", "data" -> Json.obj("address" -> "my.custom.address"))

vertx.eventBus.send("org.jbpm:receive-data:1.0", obj, { reply: Message[JsonObject] =>
    container.logger.info("Received reply: " + reply.body())
  })