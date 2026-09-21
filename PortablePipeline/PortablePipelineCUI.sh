#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$(readlink -f "$0" 2>/dev/null || echo "$0")")" && pwd)"
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
  "$SCRIPT_DIR/src/application/JobParam.java" \
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
    "$SCRIPT_DIR/src/application/JobParam.java" \
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
# Javaはロケールからファイル名の文字コードを決めるので、UTF-8のロケールが有効でない場合
# （sshでLANGが未設定の場合など）はUTF-8を指定し、非ASCII文字を含むパスを扱えるようにする。
case "${LC_ALL:-${LC_CTYPE:-${LANG:-}}}" in
  *[Uu][Tt][Ff]-8*|*[Uu][Tt][Ff]8*) ;;
  *)
    # このシステムに実在するUTF-8ロケールにだけ切り替える。Javaに必要なのはLC_CTYPEだけなので、
    # 利用者が非UTF-8のLC_ALLを明示していた場合に限りLC_ALLを置き換える
    pp_locales="$(locale -a 2>/dev/null || true)"
    for pp_loc in C.UTF-8 C.utf8 en_US.UTF-8 en_US.utf8; do
      if printf '%s\n' "$pp_locales" | grep -qix "$pp_loc"; then
        if [ -n "${LC_ALL:-}" ]; then export LC_ALL="$pp_loc"; else export LC_CTYPE="$pp_loc"; fi
        break
      fi
    done ;;
esac
exec "$JAVA_BIN" \
  -DPP_BIN_DIR="$SCRIPT_DIR" \
  -DPP_OUT_DIR="$SCRIPT_DIR" \
  -cp "$CLASS_DIR:$SCRIPT_DIR/lib/*" \
  application.PortablePipelineCLI "$@"
