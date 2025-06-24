#!/bin/bash

# 项目中 lib 目录路径
LIB_DIR="./lib"

echo "🔧 开始安装本地 JAR 到 Maven 仓库..."

# 1. 安装 LLVM 核心
mvn install:install-file \
  -Dfile=${LIB_DIR}/llvm-13.0.1-1.5.7.jar \
  -DgroupId=org.bytedeco \
  -DartifactId=llvm \
  -Dversion=13.0.1-1.5.7 \
  -Dpackaging=jar

# 2. 安装 LLVM 平台绑定
mvn install:install-file \
  -Dfile=${LIB_DIR}/llvm-platform-13.0.1-1.5.7.jar \
  -DgroupId=org.bytedeco \
  -DartifactId=llvm-platform \
  -Dversion=13.0.1-1.5.7 \
  -Dpackaging=jar

# 3. 安装 JavaCPP 本体
mvn install:install-file \
  -Dfile=${LIB_DIR}/javacpp-1.5.7.jar \
  -DgroupId=org.bytedeco \
  -DartifactId=javacpp \
  -Dversion=1.5.7 \
  -Dpackaging=jar

# 4. 安装 LLVM4J 封装库
mvn install:install-file \
  -Dfile=${LIB_DIR}/llvm4j-0.1.1-SNAPSHOT.jar \
  -DgroupId=com.llvm4j \
  -DartifactId=llvm4j \
  -Dversion=0.1.1-SNAPSHOT \
  -Dpackaging=jar

# 5. 安装 Kotlin 标准库
mvn install:install-file \
  -Dfile=${LIB_DIR}/kotlin-stdlib-1.9.0.jar \
  -DgroupId=org.jetbrains.kotlin \
  -DartifactId=kotlin-stdlib \
  -Dversion=1.9.0 \
  -Dpackaging=jar

echo "✅ 所有 JAR 安装完成！"
