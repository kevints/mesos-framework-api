A pure-JVM implementation of the Mesos framework API
(SchedulerDriver+Scheduler and ExecutorDriver+Executor).

NOTE: This code relies on undocumented internal Mesos APIs and is
only intended as a proof of concept. Also note that currently the
only operation implemented is unauthenticated framework registration.

Dependencies

```
$ lsb_release -ds
Ubuntu 13.10

$ protoc --version
libprotoc 2.4.1

$ thrift -version
Thrift version 0.8.0

$ java -version
java version "1.7.0_51"
OpenJDK Runtime Environment (IcedTea 2.4.4) (7u51-2.4.4-0ubuntu0.13.10.1)
OpenJDK 64-Bit Server VM (build 24.45-b08, mixed mode)

$ git clone https://github.com/kevints/jompactor
$ ./gradlew publishToMavenLocal
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
