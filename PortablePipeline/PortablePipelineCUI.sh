#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/.pp-cui-build"
CLASS_DIR="$BUILD_DIR/classes"
STAMP_FILE="$BUILD_DIR/.stamp"

JAVA_BIN="${JAVA_BIN:-java}"
JAVAC_BIN="${JAVAC_BIN:-javac}"

mkdir -p "$CLASS_DIR"

needs_build=0
if [ ! -f "$STAMP_FILE" ]; then
  needs_build=1
elif find \
  "$SCRIPT_DIR/src/application/PPSetting.java" \
  "$SCRIPT_DIR/src/application/InputItem.java" \
  "$SCRIPT_DIR/src/application/OptionItem.java" \
  "$SCRIPT_DIR/src/application/JobNode.java" \
  "$SCRIPT_DIR/src/application/ConnectSsh.java" \
  "$SCRIPT_DIR/src/application/PPScript.java" \
  "$SCRIPT_DIR/src/application/PPScriptParser.java" \
  "$SCRIPT_DIR/src/application/TerminalFilePicker.java" \
  "$SCRIPT_DIR/src/application/PortablePipelineCommon.java" \
  "$SCRIPT_DIR/src/application/PortablePipelineJobManager.java" \
  "$SCRIPT_DIR/src/application/PortablePipelineCLI.java" \
  -newer "$STAMP_FILE" | grep -q .; then
  needs_build=1
fi

if [ "$needs_build" -eq 1 ]; then
  rm -rf "$CLASS_DIR"
  mkdir -p "$CLASS_DIR"
  "$JAVAC_BIN" -encoding UTF-8 -cp "$SCRIPT_DIR/lib/*" -d "$CLASS_DIR" \
    "$SCRIPT_DIR/src/application/PPSetting.java" \
    "$SCRIPT_DIR/src/application/InputItem.java" \
    "$SCRIPT_DIR/src/application/OptionItem.java" \
    "$SCRIPT_DIR/src/application/JobNode.java" \
    "$SCRIPT_DIR/src/application/ConnectSsh.java" \
    "$SCRIPT_DIR/src/application/PPScript.java" \
    "$SCRIPT_DIR/src/application/PPScriptParser.java" \
    "$SCRIPT_DIR/src/application/TerminalFilePicker.java" \
    "$SCRIPT_DIR/src/application/PortablePipelineCommon.java" \
    "$SCRIPT_DIR/src/application/PortablePipelineJobManager.java" \
    "$SCRIPT_DIR/src/application/PortablePipelineCLI.java"
  touch "$STAMP_FILE"
fi

cd "$SCRIPT_DIR"
exec "$JAVA_BIN" \
  -DPP_BIN_DIR="$SCRIPT_DIR" \
  -DPP_OUT_DIR="$SCRIPT_DIR" \
  -cp "$CLASS_DIR:$SCRIPT_DIR/lib/*" \
  application.PortablePipelineCLI "$@"
