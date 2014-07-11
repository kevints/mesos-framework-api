A pure-JVM implementation of the Mesos framework API
(SchedulerDriver+Scheduler and ExecutorDriver+Executor).

NOTE: This code relies on undocumented internal Mesos APIs and is
only intended as a proof of concept. Also note that currently the
only operation implemented is unauthenticated framework registration.

Dependencies

```
$ protoc --version
libprotoc 2.5.0

% java -version
Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=utf8
java version "1.7.0_07"
Java(TM) SE Runtime Environment (build 1.7.0_07-b10)
Java HotSpot(TM) 64-Bit Server VM (build 23.3-b01, mixed mode)
```

Running the test tool

First build mesos and run a local master

```
$ git clone https://github.com/apache/mesos
$ ./bootstrap
$ ./configure
$ make
$ ./bin/mesos-master.sh
```

Check out the traffic between the 2
```
$ sudo tcpdump -i lo -vvv -A port 5050 or port 8080
```

Now build the test tool and talk to it (you get to see your framework ID).
```
$ git clone https://github.com/kevints/mesos-framework-api
$ ./gradlew run
```
