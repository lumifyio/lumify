#!/bin/bash

/opt/accumulo/bin/stop-all.sh
/opt/hadoop/sbin/stop-yarn.sh
/opt/hadoop/sbin/stop-dfs.sh
/opt/zookeeper/bin/zkServer.sh stop
service sshd stop
