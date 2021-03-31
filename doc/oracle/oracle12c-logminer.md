### Oracle 配置

1. 安装数据库

    参考：https://github.com/TavenYin/database-cdc/blob/master/doc/oracle/oracle-install.md

2. 开启归档日志

    参考：https://github.com/TavenYin/database-cdc/blob/master/doc/oracle/open-archive-log.md

3. 安装LogMiner

```shell script
desc DBMS_LOGMNR
desc DBMS_LOGMNR_D
```

```shell script
@$ORACLE_HOME/rdbms/admin/dbmslm.sql
@$ORACLE_HOME/rdbms/admin/dbmslmd.sql
```

```shell script
create role c##logminer_privs container=all;
grant create session,
 execute_catalog_role,
 select any transaction,
flashback any table,
select any table,
lock any table,
logmining,
set container,
 select any dictionary to c##logminer_privs container=all;
grant select on SYSTEM.LOGMNR_COL$ to c##logminer_privs container=all;
grant select on SYSTEM.LOGMNR_OBJ$ to c##logminer_privs container=all;
grant select on SYSTEM.LOGMNR_USER$ to c##logminer_privs container=all;
grant select on SYSTEM.LOGMNR_UID$ to c##logminer_privs container=all;
grant select on V_$DATABASE to c##logminer_privs container=all;
grant select_catalog_role to c##logminer_privs container=all;
```

```shell script
create user c##logminer identified by oracle default tablespace users container=all;
grant c##logminer_privs to c##logminer container=all;
alter user c##logminer quota unlimited on users container=all;
```

```
alter database add supplemental log data (all) columns;
```