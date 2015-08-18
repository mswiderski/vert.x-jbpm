##jBPM 6 integration with Vert.x 2.x
=======================

jBPM 6 integration with Vert.x 2.x to allow to run jBPM as vert.x modules.

####Installation:
- install vert.x 2.1.5 (it was tested with that version)
- build jbpm-vertx-module (mvn clean install)
- in case you'd like to run in managed mode (kie workbench being able to see and interact with processes created by vert.x module)
postgres db on local host will be required - default settings with jbpm as data base, user and password
- in case non managed scenario shall be used, set managed to false in conf/*.conf files to avoid shared db usage

####Build projects:
- go into vert.x-repo/send-data and build the project (mvn clean install)
- go into vert.x-repo/receive-data and build the project (mvn clean install)

####Run:
- go into jbpm-vertx-module

- start first instance of jbpm vert.x module to host receive-data project
```
vertx runzip target/jbpm-vertx-module-1.0.0-mod.zip -conf ../conf/receive-data.conf -cluster -cluster-host localhost
```
- start second instance of jbpm vert.x module to host send-data project
```
vertx runzip target/jbpm-vertx-module-1.0.0-mod.zip -conf ../conf/send-data.conf -cluster -cluster-host localhost
```
now both instances should be up and running and available for executing processes.

####Check what processes are available
- go into js and run get-processes.js
```
vertx run get-processes.js -cluster -cluster-host localhost
```

#### Start receive data process
```
vertx run start-receive-process.js -cluster -cluster-host localhost
```
Now in the console that runs receive-data vert.x instance you should see stuff happening and on the caller side information about process instance id

if you run in managed mode you should see this instance in workbench as well.

#### Start send data process
```
vertx run start-send-process.js -cluster -cluster-host localhost
```
Now in the console that runs send-data vert.x instance you should see stuff happening and on the caller side information about process instance id,
in addition to that on instance that runs receive-data you should see that message was received and instance was completed.