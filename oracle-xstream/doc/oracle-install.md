### Docker安装
以12c r1 为例

1. 首先准备oracle的二进制安装包

    官网好像目前只能下载版本比较新的oracle安装包了...
2. git clone https://github.com/oracle/docker-images

3. cd docker-images/OracleDatabase/SingleInstance/dockerfiles
    
    看好你要安装的版本，将二进制包复制到对应的文件下
4. 执行构建脚本

    ```shell script
    [oracle@localhost dockerfiles]$ ./buildContainerImage.sh -h
   
   Usage: buildContainerImage.sh -v [version] [-e | -s | -x] [-i] [-o] [container build option]
   Builds a container image for Oracle Database.
   
   Parameters:
      -v: version to build
          Choose one of: 11.2.0.2  12.1.0.2  12.2.0.1  18.3.0  18.4.0  19.3.0  
      -e: creates image based on 'Enterprise Edition'
      -s: creates image based on 'Standard Edition 2'
      -x: creates image based on 'Express Edition'
      -i: ignores the MD5 checksums
      -o: passes on container build option
   
   * select one edition only: -e, -s, or -x
   
   LICENSE UPL 1.0
   
   Copyright (c) 2014,2021 Oracle and/or its affiliates.
   
    [oracle@localhost dockerfiles]$ ./buildContainerImage.sh -v 12.1.0.2 -e
    ``` 
5. 启动容器

    ```shell script
    docker run --name oracle_12.1.0.2-ee \
    > -p 1521:1521 -p 5500:5500 \
    > -e ORACLE_SID=ORCLCDB \
    > -e ORACLE_PDB=ORCLPDB1 \
    > -e ORACLE_PWD=oracle \
    > -e ORACLE_CHARACTERSET=AL32UTF8 \
    > oracle/database:12.1.0.2-ee
    ```