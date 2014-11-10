
# Backup

```
io.lumify.backupRestore.BackupRestore
  --accumuloInstanceName=lumify
  --accumuloUserName=root
  --accumuloPassword=password
  --zookeeperServers=192.168.33.10
  --hdfsLocation=192.168.33.10:8020
  backup
```

# Restore

*WARNING: Restore is a destructive process and will move the tablet files from the backup directories.
  See org.apache.accumulo.core.client.admin.TableOperations.importTable*

```
io.lumify.backupRestore.BackupRestore
  --accumuloInstanceName=lumify
  --accumuloUserName=root
  --accumuloPassword=password
  --zookeeperServers=192.168.33.10
  --hdfsLocation=192.168.33.10:8020
  --hdfsRestoreDirectory=/backup/20141110T0947 restore
```
