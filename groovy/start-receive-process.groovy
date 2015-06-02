def eb = vertx.eventBus

def json = ["service": "process", "operation": "start", "id": "receive-data.receive-data-process", "data": ["address": "my.custom.address"]]

eb.send("org.jbpm:receive-data:1.0", json, { reply ->
    println "Received reply ${reply.body()}"
  })  