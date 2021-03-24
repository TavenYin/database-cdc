### Oracle 12c 配置

> 12c 以及以上版本应该配置方式都是一样的

1. 安装数据库

    要求 11.2.0.4/12.1.0.2或以上版本

2. 开启归档日志

3. 配置Xstream相关 

    ```shell script
    CREATE TABLESPACE xstream_adm_tbs DATAFILE '/opt/oracle/oradata/ORCLCDB/xstream_adm_tbs.dbf' SIZE 25M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED;
    alter session set container = ORCLPDB1;
    CREATE TABLESPACE xstream_adm_tbs DATAFILE '/opt/oracle/oradata/ORCLCDB/ORCLPDB1/xstream_adm_tbs.dbf' SIZE 25M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED;
    alter session set container = CDB$ROOT;
    
    CREATE USER c##xstrmadmin IDENTIFIED BY password DEFAULT TABLESPACE xstream_adm_tbs QUOTA UNLIMITED ON xstream_adm_tbs CONTAINER=ALL;
    GRANT CREATE SESSION, SET CONTAINER TO c##xstrmadmin CONTAINER=ALL;
    BEGIN
       DBMS_XSTREAM_AUTH.GRANT_ADMIN_PRIVILEGE(
          grantee                 => 'c##xstrmadmin',
          privilege_type          => 'CAPTURE',
          grant_select_privileges => TRUE,
          container               => 'ALL'
       );
    END;
    ```