### Oracle 12c 配置

> 12c 以及以上版本应该配置方式都是一样的

1. 安装数据库

    要求 11.2.0.4/12.1.0.2或以上版本

    参考：https://github.com/TavenYin/database-cdc/blob/master/doc/oracle/oracle-install.md

2. 开启归档日志

    参考：https://github.com/TavenYin/database-cdc/blob/master/doc/oracle/open-archive-log.md

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
   /
    ```
   
- 创建xstream用户
   ```shell script
    CREATE TABLESPACE xstream_tbs DATAFILE '/opt/oracle/oradata/ORCLCDB/xstream_tbs.dbf' SIZE 25M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED;
    alter session set container = ORCLPDB1;
    CREATE TABLESPACE xstream_tbs DATAFILE '/opt/oracle/oradata/ORCLCDB/ORCLPDB1/xstream_tbs.dbf' SIZE 25M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED;
    alter session set container = CDB$ROOT;
    
    CREATE USER c##xstrm IDENTIFIED BY password DEFAULT TABLESPACE xstream_tbs QUOTA UNLIMITED ON xstream_tbs CONTAINER=ALL;
    GRANT CREATE SESSION TO c##xstrm CONTAINER=ALL;
    GRANT SET CONTAINER TO c##xstrm CONTAINER=ALL;
    GRANT SELECT ON V_$DATABASE to c##xstrm CONTAINER=ALL;
    GRANT FLASHBACK ANY TABLE TO c##xstrm CONTAINER=ALL;
    GRANT SELECT ANY TABLE to c##xstrm CONTAINER=ALL;
    GRANT LOCK ANY TABLE TO c##xstrm CONTAINER=ALL;
    grant select_catalog_role to c##xstrm CONTAINER=ALL;
    ```
   
- alter database supplemental log
       
   ```
   alter database add supplemental log data (all) columns;

   # 推荐使用如下配置，将Oracle redo log 捕获的信息量降到最低
   ALTER TABLE schema.tablename ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
   ```
  
- 创建XStream出站服务器

    使用c##xstrmadmin登录

    ```shell script
    DECLARE
      tables DBMS_UTILITY.UNCL_ARRAY;
      schemas DBMS_UTILITY.UNCL_ARRAY;
    BEGIN
      tables(1) := NULL;
      schemas(1) := 'SCOTT';
      DBMS_XSTREAM_ADM.CREATE_OUTBOUND(
        server_name     =>  'dbzxout',
        table_names     =>  tables,
        schema_names    =>  schemas);
    END;
    /
    ```
- sys用户登录，执行以下命令，运行xstrm用户连接出站服务器
  
    ```shell script
    BEGIN
       DBMS_XSTREAM_ADM.ALTER_OUTBOUND(
          server_name  => 'dbzxout',
          connect_user => 'c##xstrm'
       );
    END;
    /
    ```