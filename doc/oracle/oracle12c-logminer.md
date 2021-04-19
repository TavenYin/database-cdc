### Oracle 配置

1. 安装数据库

    参考：https://github.com/TavenYin/database-cdc/blob/master/doc/oracle/oracle-install.md

2. 开启归档日志

    参考：https://github.com/TavenYin/database-cdc/blob/master/doc/oracle/open-archive-log.md

3. 安装LogMiner

    检查LogMiner是否安装，若有信息返回，说明LogMiner已安装
    ```shell script
    desc DBMS_LOGMNR
    desc DBMS_LOGMNR_D
    ```

    安装LogMiner
    ```shell script
    @$ORACLE_HOME/rdbms/admin/dbmslm.sql
    @$ORACLE_HOME/rdbms/admin/dbmslmd.sql
    ```

    创建LogMiner用户权限
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
    
    GRANT CREATE TABLE TO c##logminer_privs CONTAINER=ALL;
    grant select_catalog_role TO c##logminer_privs CONTAINER=ALL;
    
    grant select on SYSTEM.LOGMNR_COL$ to c##logminer_privs container=all;
    grant select on SYSTEM.LOGMNR_OBJ$ to c##logminer_privs container=all;
    grant select on SYSTEM.LOGMNR_USER$ to c##logminer_privs container=all;
    grant select on SYSTEM.LOGMNR_UID$ to c##logminer_privs container=all;
    grant select on V_$DATABASE to c##logminer_privs container=all;
    grant select_catalog_role to c##logminer_privs container=all;
    ```

    创建LogMiner用户
    ```shell script
    create user c##logminer identified by oracle default tablespace users container=all;
    grant c##logminer_privs to c##logminer container=all;
    alter user c##logminer quota unlimited on users container=all;
    ```

    开启补充日志
    ```
    alter database add supplemental log data (all) columns;

    # 推荐使用如下配置，将Oracle redo log 捕获的信息量降到最低
    ALTER TABLE schema.tablename ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
    ```
   
   ### 备注
   12c以上的版本安装步骤应该都是差不多的（我没试过）
   
   11g可以参考一下下面的那个华为的文档或者Debezium文档
   
   ### 参考
   - https://debezium.io/documentation/reference/connectors/oracle.html
   
   - https://support.huaweicloud.com/usermanual-roma/fdi-ug-190624013.html