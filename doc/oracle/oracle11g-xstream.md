### Oracle 配置

1. 安装数据库

    要求 11.2.0.4/12.1.0.2或以上版本

2. 开启归档日志

3. 配置Xstream相关 

- 开启xstream

    ```
    sqlplus / as sysdba
    alter system set enable_goldengate_replication=true;
    ```

- 创建XStream管理员用户并配置权限

    ```
    CREATE TABLESPACE xstream_adm_tbs DATAFILE '/opt/oracle/oradata/orcl/xstream_adm_tbs.dbf' SIZE 25M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED;
    CREATE USER xstrmadmin IDENTIFIED BY password DEFAULT TABLESPACE xstream_adm_tbs QUOTA UNLIMITED ON xstream_adm_tbs;
    GRANT CREATE SESSION TO xstrmadmin;
    BEGIN
    DBMS_XSTREAM_AUTH.GRANT_ADMIN_PRIVILEGE(
        grantee                 => 'xstrmadmin',
        privilege_type          => 'CAPTURE',
        grant_select_privileges => TRUE,
        container             => 'ALL'
    );
    END;
    ```

    > 其中：
    >- xstream_adm_tbs为XStream管理员用户的表空间名，请根据实际规划设置。
    >- /opt/oracle/oradata/orcl/xstream_adm_tbs.dbf为XStream管理员用户的表空间文件，请根据实际规划设置。
    >- xstrmadmin为XStream管理员用户名，请根据实际规划设置。
    >- password为XStream管理员用户密码，请根据实际规划设置。
    >- container => 'ALL'”仅当Oracle为12c或以上版本时，才需要添加，否则删除此行内容。

- 创建xstream用户
    ```
    CREATE TABLESPACE xstream_tbs DATAFILE '/opt/oracle/oradata/orcl/xstream_tbs.dbf' SIZE 25M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED;
    CREATE USER xstrm IDENTIFIED BY password DEFAULT TABLESPACE xstream_tbs QUOTA UNLIMITED ON xstream_tbs;
    GRANT CREATE SESSION TO xstrm;
    GRANT SELECT ON V_$DATABASE to xstrm;
    GRANT FLASHBACK ANY TABLE TO xstrm;
    GRANT SELECT ANY TABLE to xstrm;
    GRANT LOCK ANY TABLE TO xstrm;
    grant select_catalog_role to xstrm;
    ```

    > 其中：
    > - xstream_tbs为ROMA Connect连接用户的表空间名，请根据实际规划设置。
    > - /opt/oracle/oradata/orcl/xstream_tbs.dbf为ROMA Connect连接用户的表空间文件，请根据实际规划设置。
    > - xstrm为ROMA Connect连接用户名，请根据实际规划设置。
    > - password为ROMA Connect连接用户密码，请根据实际规划设置。


- alter database supplemental log
    ```
    alter database add supplemental log data (all) columns;

    # 推荐使用如下配置，将Oracle redo log 捕获的信息量降到最低
    ALTER TABLE schema.tablename ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
    ```

- 创建Xstream出站服务器 (使用xstrmadmin执行以下命令，注意修改Schema和Table)
    ```
    DECLARE
    tables DBMS_UTILITY.UNCL_ARRAY;
    schemas DBMS_UTILITY.UNCL_ARRAY;
    BEGIN
    tables(1) := NULL;
    schemas(1) := 'FOPTRADE';
    DBMS_XSTREAM_ADM.CREATE_OUTBOUND(
        server_name => 'dbzxout',
        table_names => tables,
        schema_names => schemas
    );
    END;
    /
    ```
    如果是抓取schema下的所有表，就不需要配置tables

- 执行以下命令，运行xstrm用户连接出站服务器
    ```
    BEGIN
    DBMS_XSTREAM_ADM.ALTER_OUTBOUND(
        server_name  => 'dbzxout',
        connect_user => 'xstrm'
    );
    END;
    /
    ```
  
### 参考
- https://debezium.io/documentation/reference/connectors/oracle.html

- https://support.huaweicloud.com/usermanual-roma/fdi-ug-190624013.html
