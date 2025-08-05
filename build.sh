#!/bin/bash
set -e

# === 配置项 ===
CLASSES_DIR="out"
JAR_NAME="Compiler.jar"
STUDENT_CODE_DIR="src"
CLASSPATH="lib/antlr-4.13.1-complete.jar"

# === 查找所有 Java 文件 ===
JAVA_FILE_LIST=$(find "$STUDENT_CODE_DIR" -name "*.java")

# === 创建输出目录 ===
mkdir -p "$CLASSES_DIR"

# === 编译 Java 源文件 ===
echo "Compiling Java source files..."
javac -d "$CLASSES_DIR" -encoding utf-8 -cp "$CLASSPATH" -sourcepath "$STUDENT_CODE_DIR" $JAVA_FILE_LIST

# === 打包 JAR 文件 ===
echo "Creating JAR..."
cd "$CLASSES_DIR"

# 如果有 package，比如 package cn.edu.nju; 请修改为 cn.edu.nju.Compiler
MAIN_CLASS="Compiler"

jar cfe "../$JAR_NAME" "$MAIN_CLASS" $(find . -name "*.class")

cd ..

echo "✅ Build completed: $JAR_NAME"
