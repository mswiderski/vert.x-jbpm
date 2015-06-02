def eb = vertx.eventBus

def json = ["service": "process", "operation": "start", "id": "send-data.send-data-process", "data": ["address": "my.custom.address", "message": "testing jBPM with vert.x by Groovy"]]

eb.send("org.jbpm:send-data:1.0", json, { reply ->
    println "Received reply ${reply.body()}"
  })  