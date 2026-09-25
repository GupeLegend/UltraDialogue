#!/usr/bin/env bash
# ============================================================
#  UltraDialogue - compilar sin Gradle (solo javac).
#  Baja las dependencias a ../_libs si faltan, compila y arma el .jar.
#  Uso:  bash compilar.sh
# ============================================================
set -e
cd "$(dirname "$0")"

NOMBRE="UltraDialogue"
VER="1.0"
MC="26.1.2.build.74-stable"   # API de Paper contra la que se compila

# Paper 26.x esta compilado para Java 25: con un JDK menor javac dice "cannot access Player"
# y parece un problema de la API cuando en realidad es la version del compilador.
JDK_MIN=25
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then JC="$JAVA_HOME/bin/javac"; else JC="javac"; fi
V=$("$JC" -version 2>&1 | sed 's/javac //;s/\..*//')
if [ "$V" -lt "$JDK_MIN" ] 2>/dev/null; then
  for c in "$HOME/jdk25"/*/bin/javac* /c/Users/*/jdk25/*/bin/javac*; do
    [ -x "$c" ] && { JC="$c"; break; }
  done
  V=$("$JC" -version 2>&1 | sed 's/javac //;s/\..*//')
  [ "$V" -lt "$JDK_MIN" ] 2>/dev/null && {
    echo "ERROR: hace falta JDK $JDK_MIN o mayor (tenes $V). Instalalo o poné JAVA_HOME."; exit 1; }
fi
echo "javac: $("$JC" -version 2>&1)"

# _libs compartido: se busca hacia arriba con ruta RELATIVA, asi esta carpeta se puede mover sin
# romper nada. Relativa a proposito: en Git Bash $PWD da /c/Users/... y javac en Windows no lo
# entiende dentro del classpath.
LIB=""; __p="."
for _ in 1 2 3 4 5 6; do
  [ -d "$__p/_libs" ] && { LIB="$__p/_libs"; break; }
  __p="$__p/.."
done
[ -z "$LIB" ] && LIB="./_libs"
mkdir -p "$LIB"

case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";" ;; *) SEP=":" ;; esac

PAPER="https://repo.papermc.io/repository/maven-public"
CENTRAL="https://repo1.maven.org/maven2"
get() { [ -f "$LIB/$2" ] || { echo "Descargando $2..."; curl -sL "$1" -o "$LIB/$2"; }; }

get "$PAPER/io/papermc/paper/paper-api/$MC/paper-api-$MC.jar" "paper-api-26.jar"
get "$CENTRAL/net/kyori/adventure-api/4.26.1/adventure-api-4.26.1.jar" "adventure-api-4.26.1.jar"
get "$CENTRAL/net/kyori/adventure-key/4.26.1/adventure-key-4.26.1.jar" "adventure-key-4.26.1.jar"
get "$CENTRAL/net/kyori/adventure-text-serializer-legacy/4.26.1/adventure-text-serializer-legacy-4.26.1.jar" "adventure-text-serializer-legacy-4.26.1.jar"
get "$CENTRAL/net/kyori/adventure-text-minimessage/4.26.1/adventure-text-minimessage-4.26.1.jar" "adventure-text-minimessage-4.26.1.jar"
get "$CENTRAL/net/kyori/examination-api/1.3.0/examination-api-1.3.0.jar" "examination-api-1.3.0.jar"
get "$CENTRAL/net/kyori/examination-string/1.3.0/examination-string-1.3.0.jar" "examination-string-1.3.0.jar"
get "$PAPER/net/md-5/bungeecord-chat/1.21-R0.2-deprecated+build.21/bungeecord-chat-1.21-R0.2-deprecated+build.21.jar" "bungeecord-chat.jar"
get "$CENTRAL/net/luckperms/api/5.4/api-5.4.jar" "luckperms-api.jar"
get "$CENTRAL/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar" "gson-2.11.0.jar"
# La API de Paper lleva anotaciones de JetBrains en sus firmas: sin este jar javac aborta con
# "class file for org.jetbrains.annotations.NotNull not found".
get "$CENTRAL/org/jetbrains/annotations/26.0.2/annotations-26.0.2.jar" "annotations-26.0.2.jar"
# Guava: Paper la usa en firmas publicas (Material.getItemAttributes -> Multimap).
get "$CENTRAL/com/google/guava/guava/33.7.1-jre/guava-33.7.1-jre.jar" "guava.jar"

CP=""
for j in paper-api-26.jar adventure-api-4.26.1.jar adventure-key-4.26.1.jar \
         adventure-text-serializer-legacy-4.26.1.jar adventure-text-minimessage-4.26.1.jar \
         examination-api-1.3.0.jar examination-string-1.3.0.jar bungeecord-chat.jar \
         luckperms-api.jar gson-2.11.0.jar annotations-26.0.2.jar guava.jar; do
  CP="$CP${CP:+$SEP}$LIB/$j"
done

echo "Compilando $NOMBRE $VER..."
rm -rf build/classes; mkdir -p build/classes
find src/main/java -name "*.java" > build/sources.txt
"$JC" -Xlint:none -cp "$CP" -d build/classes @build/sources.txt
cp -r src/main/resources/* build/classes/
# El .jar va a URPlugin/Jars/, que es de donde se sube al servidor: UNA sola copia, para no
# terminar con dos jars distintos y no saber cual es el bueno. Se busca hacia arriba igual que
# _libs, asi la carpeta del plugin se puede mover sin romper nada.
JARS=""; __p=".."
for _ in 1 2 3 4 5; do
  [ -d "$__p/Jars" ] && { JARS="$__p/Jars"; break; }
  __p="$__p/.."
done
# Si no hay una carpeta Jars/ arriba (repo clonado suelto), el .jar queda en build/ y no
# se ensucia el directorio padre de quien clono.
if [ -z "$JARS" ]; then JARS="build"; mkdir -p "$JARS"; fi

JARBIN="$(dirname "$JC")/jar"; [ -x "$JARBIN" ] || [ -x "$JARBIN.exe" ] || JARBIN="jar"
( cd build/classes && "$JARBIN" cf "../../$JARS/$NOMBRE-$VER.jar" . )
rm -f build/sources.txt
echo "LISTO -> Jars/$NOMBRE-$VER.jar"
