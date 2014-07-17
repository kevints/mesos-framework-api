A pure-JVM implementation of the Mesos framework API
(SchedulerDriver+Scheduler and ExecutorDriver+Executor).

NOTE: This code relies on undocumented internal Mesos APIs and is
only intended as a proof of concept. Also note that currently the
only operation implemented is unauthenticated framework registration.

Dependencies
============

```
$ protoc --version
libprotoc 2.5.0

% java -version
Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=utf8
java version "1.7.0_07"
Java(TM) SE Runtime Environment (build 1.7.0_07-b10)
Java HotSpot(TM) 64-Bit Server VM (build 23.3-b01, mixed mode)
```

Running the unit tests
======================
```
$ git clone https://github.com/kevints/mesos-framework-api
$ cd mesos-framework-api
$ ./gradlew build
```

Running the test tool
=====================

Build mesos
-----------
```
$ git clone https://github.com/apache/mesos
$ cd mesos
$ ./bootstrap
$ ./configure
$ make
```

Run a mesos master
------------------
```
$ cd mesos
$ ./bin/mesos-master.sh --registry=in_memory --ip=127.0.0.1 --port=5050 --authenticate --credentials=<(echo "test pass")
```

Run a mesos slave
-----------------
```
$ cd mesos
$ ./bin/mesos-slave.sh --master=127.0.0.1:5050
```

Run the demo framework
----------------------
```
$ cd mesos-framework-api
$ ./gradlew run
```
The demo framework authenticates, registers, and launches hello world tasks.
