#!/bin/bash
cd "$(dirname "$(readlink -f "$0")")"
# Javaはロケールからファイル名の文字コードを決めるので、UTF-8のロケールが有効でない場合
# （LANGが未設定のデスクトップランチャーやsshなど）はUTF-8を指定し、非ASCII文字を含むパスを扱えるようにする。
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
./jre-21.0.7-full/bin/java -jar PortablePipeline.jar
