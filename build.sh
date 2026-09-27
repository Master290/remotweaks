#!/bin/bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$(cd "$DIR/../.." && pwd)"

python3 -c '
import os, subprocess, shutil, zipfile

server_dir = "'"$SERVER_DIR"'"
src_dir = os.path.join(server_dir, "plugins_src/remotweaks")
classes_dir = os.path.join(src_dir, "target/classes")
os.makedirs(classes_dir, exist_ok=True)

jars = []
for root, dirs, files in os.walk(os.path.join(server_dir, "libraries")):
    for f in files:
        if f.endswith(".jar"):
            jars.append(os.path.join(root, f))
for root, dirs, files in os.walk(os.path.join(server_dir, "versions")):
    for f in files:
        if f.endswith(".jar"):
            jars.append(os.path.join(root, f))

cp = ":".join(jars)

java_files = []
for root, dirs, files in os.walk(os.path.join(src_dir, "src/main/java")):
    for f in files:
        if f.endswith(".java"):
            java_files.append(os.path.join(root, f))

javac = "/home/daniar/.local/jdk/jdk-25.0.4.1+1/bin/javac"
if not os.path.exists(javac):
    javac = "javac"

cmd = [javac, "-encoding", "UTF-8", "-cp", cp, "-d", classes_dir] + java_files

print(f"Compiling {len(java_files)} Java files...")
res = subprocess.run(cmd, capture_output=True, text=True)
if res.returncode != 0:
    print("STDOUT:", res.stdout)
    print("STDERR:", res.stderr)
    exit(1)

resources_dir = os.path.join(src_dir, "src/main/resources")
for root, dirs, files in os.walk(resources_dir):
    for f in files:
        rel_path = os.path.relpath(os.path.join(root, f), resources_dir)
        dest_path = os.path.join(classes_dir, rel_path)
        os.makedirs(os.path.dirname(dest_path), exist_ok=True)
        shutil.copy2(os.path.join(root, f), dest_path)

target_jar = os.path.join(server_dir, "plugins/RemoTweaks-1.0.0.jar")
with zipfile.ZipFile(target_jar, "w", zipfile.ZIP_DEFLATED) as z:
    for root, dirs, files in os.walk(classes_dir):
        for f in files:
            full_path = os.path.join(root, f)
            arcname = os.path.relpath(full_path, classes_dir)
            z.write(full_path, arcname)

print("Build complete: plugins/RemoTweaks-1.0.0.jar")
'
