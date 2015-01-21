# Build

        mvn package -pl tools/backup-restore -am -DskipTests

# Backup

        java -jar tools/backup-restore/target/lumify-backup-restore-0.4.1-SNAPSHOT-with-dependencies.jar \
          --accumuloInstanceName=lumify \
          --accumuloUsername=root \
          --accumuloPassword=password \
          --zookeeperServers=lumify-dev \
          --hadoopFsDefaultFS=lumify-dev:8020 \
          --hadoopDfsClientUseDatanodeHostname \
          --hadoopUsername=root \
          backup

# Restore

**WARNING:**
Restore is a destructive process and will move the tablet files from the backup directories.
See _org.apache.accumulo.core.client.admin.TableOperations.importTable_

        java -jar tools/backup-restore/target/lumify-backup-restore-0.4.1-SNAPSHOT-with-dependencies.jar \
          --accumuloInstanceName=lumify \
          --accumuloUsername=root \
          --accumuloPassword=password \
          --zookeeperServers=lumify-dev \
          --hadoopFsDefaultFS=lumify-dev:8020 \
          --hadoopDfsClientUseDatanodeHostname \
          --hadoopUsername=root \
          --hdfsRestoreDirectory=/backup/20150121T1442 \
          restore
