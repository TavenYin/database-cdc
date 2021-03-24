### Oracle 开启归档日志

Database log mode: Archive Mode 代表为已开启归档日志，可跳过此步骤
```shell script
SQL> archive log list;
Database log mode              No Archive Mode
Automatic archival             Disabled
Archive destination            USE_DB_RECOVERY_FILE_DEST
Oldest online log sequence     13
Current log sequence           15
```

根据实际情况设置这两个参数
```shell script
SQL> show parameter db_recovery_file_dest

NAME                                 TYPE        VALUE
------------------------------------ ----------- ------------------------------
db_recovery_file_dest                string      /opt/oracle/oradata/fast_recov
                                                 ery_area
db_recovery_file_dest_size           big integer 4560M
```

由于我会多造一些数据进行测试，这里我将闪回区稍微设置大一些
```shell script
alter system set db_recovery_file_dest_size = 20G;    
```

执行以下命令开启归档日志
```shell script
shutdown immediate;
startup mount;
alter database archivelog;
alter database open;
```
