#!/usr/bin/env bash
# ============================================================
#  UltraDialogue - build without Gradle (javac only).
#  Downloads its own dependencies into ./libs if missing, compiles, and packs the .jar.
#  Usage: bash compilar.sh
# ============================================================
set -e
cd "$(dirname "$0")"

NAME="UltraDialogue"
VER="1.1.3"
MC="26.1.2.build.74-stable"   # Paper API version this is compiled against

# Paper 26.x is built for Java 25: an older JDK fails with "cannot access Player" and looks like
# an API problem when it's really the compiler version.
JDK_MIN=25
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then JC="$JAVA_HOME/bin/javac"; else JC="javac"; fi
V=$("$JC" -version 2>&1 | sed -n 's/javac \([0-9]*\).*/\1/p')
if [ -z "$V" ] || [ "$V" -lt "$JDK_MIN" ] 2>/dev/null; then
  for c in "$HOME/jdk25"/*/bin/javac* /c/Users/*/jdk25/*/bin/javac*; do
    [ -x "$c" ] && { JC="$c"; break; }
  done
  V=$("$JC" -version 2>&1 | sed -n 's/javac \([0-9]*\).*/\1/p')
  [ -z "$V" ] || [ "$V" -lt "$JDK_MIN" ] 2>/dev/null && {
    echo "ERROR: needs JDK $JDK_MIN or newer (found $V). Install it or set JAVA_HOME."; exit 1; }
fi
echo "javac: $("$JC" -version 2>&1)"

# Dependencies live in ./libs, next to this script: this project compiles on its own, with no
# shared folder between plugins.
LIB="./libs"
mkdir -p "$LIB"

case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";" ;; *) SEP=":" ;; esac

PAPER="https://repo.papermc.io/repository/maven-public"
CENTRAL="https://repo1.maven.org/maven2"
get() { [ -f "$LIB/$2" ] || { echo "Downloading $2..."; curl -sL "$1" -o "$LIB/$2"; }; }

get "$PAPER/io/papermc/paper/paper-api/$MC/paper-api-$MC.jar" "paper-api-26.jar"
get "$CENTRAL/net/kyori/adventure-api/4.26.1/adventure-api-4.26.1.jar" "adventure-api-4.26.1.jar"
get "$CENTRAL/net/kyori/adventure-key/4.26.1/adventure-key-4.26.1.jar" "adventure-key-4.26.1.jar"
get "$CENTRAL/net/kyori/adventure-text-serializer-legacy/4.26.1/adventure-text-serializer-legacy-4.26.1.jar" "adventure-text-serializer-legacy-4.26.1.jar"
get "$CENTRAL/net/kyori/adventure-text-minimessage/4.26.1/adventure-text-minimessage-4.26.1.jar" "adventure-text-minimessage-4.26.1.jar"
get "$CENTRAL/net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar" "examination-api-1.3.0.jar"
get "$CENTRAL/net/kyori/examination-string/1.3.0/examination-string-1.3.0.jar" "examination-string-1.3.0.jar"
get "$PAPER/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar" "bungeecord-chat.jar"
get "$CENTRAL/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar" "gson-2.11.0.jar"
# Paper's API carries JetBrains annotations in its method signatures: without this jar javac aborts
# with "class file for org.jetbrains.annotations.NotNull not found".
get "$CENTRAL/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar" "annotations-26.0.2.jar"
# Guava: Paper uses it in public signatures (Material.getItemAttributes -> Multimap).
get "$CENTRAL/com/google/guava/guava/33.7.1-jre/guava-33.7.1-jre.jar" "guava.jar"
# Only to COMPILE the optional integrations (hook/bq, hook/papi). Never bundled in the jar.
get "https://cdn.modrinth.com/data/ZbvwkXKZ/versions/JKDAEeVt/beautyquests-2.1.0%2Bbuild.144.jar" "beautyquests-2.1.0-b144.jar"
get "https://repo.extendedclip.com/releases/me/clip/placeholderapi/2.11.7/placeholderapi-2.11.7.jar" "placeholderapi.jar"

CP=""
for j in paper-api-26.jar adventure-api-4.26.1.jar adventure-key-4.26.1.jar \
         adventure-text-serializer-legacy-4.26.1.jar adventure-text-minimessage-4.26.1.jar \
         examination-api-1.3.0.jar examination-string-1.3.0.jar bungeecord-chat.jar \
         gson-2.11.0.jar annotations-26.0.2.jar guava.jar \
         beautyquests-2.1.0-b144.jar placeholderapi.jar; do
  CP="$CP${CP:+$SEP}$LIB/$j"
done

echo "Compiling $NAME $VER..."
rm -rf build/classes; mkdir -p build/classes
find src/main/java -name "*.java" > build/sources.txt
"$JC" -Xlint:none -cp "$CP" -d build/classes @build/sources.txt
cp -r src/main/resources/* build/classes/
# The .jar goes to URPlugin/Jars/, where it's uploaded to the server from: a SINGLE copy, so there
# are never two different jars and no doubt about which one is current. Looked up the same way as
# libs/, so this folder can move without breaking anything.
JARS=""; __p=".."
for _ in 1 2 3 4 5; do
  [ -d "$__p/Jars" ] && { JARS="$__p/Jars"; break; }
  __p="$__p/.."
done
# No Jars/ folder above (a repo cloned on its own): the .jar stays in build/ instead of littering
# whatever folder it was cloned into.
if [ -z "$JARS" ]; then JARS="build"; mkdir -p "$JARS"; fi

JARBIN="$(dirname "$JC")/jar"; [ -x "$JARBIN" ] || [ -x "$JARBIN.exe" ] || JARBIN="jar"
( cd build/classes && "$JARBIN" cf "../../$JARS/$NAME-$VER.jar" . )
rm -f build/sources.txt
echo "DONE -> Jars/$NAME-$VER.jar"
