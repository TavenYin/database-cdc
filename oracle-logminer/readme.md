### 食用指南
1. 安装Oracle并且配置LogMiner

   参考https://github.com/TavenYin/database-cdc/blob/master/doc/oracle/oracle12c-logminer.md

2. 正确配置 oracle_config.properties

3. 运行LogMinerStarter.java

### 常见问题

1. ORA-04036: 实例使用的 PGA 内存超出 PGA_AGGREGATE_LIMIT
    尝试使用如下命令解决
    ```shell script
    SQL> ALTER SYSTEM SET PGA_AGGREGATE_LIMIT=2G;  
    -- or
    SQL> ALTER SYSTEM SET PGA_AGGREGATE_LIMIT=0; --disables the hard limit
    ```
