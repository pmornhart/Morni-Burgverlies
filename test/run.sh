#!/usr/bin/env bash
# Echter Test von Morni Burgverlies: baut das Plugin und das Test-Netzwerk, richtet einen Paper-Server ein (offline,
# eigener Port, RCON an), startet ihn, schickt Test-Spieler (mineflayer) durch alle Abläufe und hält ihn wieder an.
#
#   test/run.sh                 # alles
#   KEEP=1 test/run.sh          # Server danach laufen lassen (zum Nachschauen)
#   KEEP=1 SKIP_TEST=1 test/run.sh   # nur Server starten
#
# Erwartet (Vorgaben für die MoinMorni-Sandbox, alles per Umgebungsvariable änderbar):
#   BV_JAVA       Java 25 (Ordner; sonst die JDK 25 aus dem Cache, sonst JAVA_HOME)
#   PAPER_JAR     Paper 26.1.2 (mineflayer kann bis 26.1)
#   TEMPLATE      ein schon einmal gestarteter Paper-26.1.2-Ordner (libraries/, cache/, versions/ werden kopiert,
#                 damit nichts heruntergeladen werden muss) – optional
#   BV_NODE_MODULES  node_modules mit mineflayer
#   SEED          fester Startwert der Etagen (z. B. 8: Siegelkammer, Brunnenschacht, Glutkammer, Gruft …)
#   EXTRA_PLUGINS weitere Jars für den Server (Vorgabe: ViaVersion + ViaBackwards aus dem Cache)
#
# Paper 26.3 (wie im Netzwerk): mineflayer kann nur bis 26.1 – darum läuft der Test auf 26.1.2; ViaVersion und
# ViaBackwards sind trotzdem dabei, damit das Plugin neben ihnen geprüft wird.
#
# Verfasst mit Claude 🤖
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(dirname "$HERE")"
CACHE="${MM_CACHE:-/home/user/mm-cache}"
CACHED_JDK="$(ls -d "$CACHE"/jdk/jdk-25* 2>/dev/null | head -1 || true)"
JAVA_HOME="${BV_JAVA:-${CACHED_JDK:-${JAVA_HOME:-}}}"
export JAVA_HOME
JAVA="$JAVA_HOME/bin/java"
PAPER_JAR="${PAPER_JAR:-$CACHE/jars/paper-26.1.2.jar}"
TEMPLATE="${TEMPLATE:-$CACHE/servers/t1}"
SERVER="${SERVER_DIR:-$CACHE/servers/burgverlies-test}"
PORT="${PORT:-25780}"
RCON_PORT="${RCON_PORT:-25880}"
export NODE_PATH="${BV_NODE_MODULES:-$CACHE/node/node_modules}${NODE_PATH:+:$NODE_PATH}"

echo "== Bauen"
(cd "$REPO" && mvn -q -B -DskipTests package)
(cd "$REPO" && mvn -q -B -f test/fake-network/pom.xml package)

echo "== Server einrichten: $SERVER"
mkdir -p "$SERVER/plugins/MorniBurgverlies" "$SERVER/plugins/MoinMorni"
if [ -d "$TEMPLATE" ] && [ ! -d "$SERVER/libraries" ]; then
  cp -r "$TEMPLATE/libraries" "$TEMPLATE/cache" "$TEMPLATE/versions" "$SERVER/" 2>/dev/null || true
fi
cp "$PAPER_JAR" "$SERVER/paper.jar"
echo "eula=true" > "$SERVER/eula.txt"
RCON_PASS="$(head -c 18 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 20)"
printf '%s' "$RCON_PASS" > "$SERVER/.rcon-pass"
chmod 600 "$SERVER/.rcon-pass"
cat > "$SERVER/server.properties" <<EOF
online-mode=false
enforce-secure-profile=false
server-port=$PORT
enable-rcon=true
rcon.port=$RCON_PORT
rcon.password=$RCON_PASS
level-name=world
level-type=minecraft\:flat
generator-settings={"layers"\:[{"block"\:"minecraft\:bedrock","height"\:1}],"biome"\:"minecraft\:plains"}
generate-structures=false
spawn-protection=0
view-distance=6
simulation-distance=4
max-players=20
white-list=false
enforce-whitelist=false
motd=Morni Burgverlies Test
allow-flight=true
EOF
# Testwerte: wenige Räume, kurze Suche, kurzes Ausbluten, kurze Wege.
cat > "$SERVER/plugins/MorniBurgverlies/config.yml" <<EOF2
welt: burgverlies
max-abstiege: 3
raeume-pro-etage: 4
suche:
  warten-sekunden: 5
wiederbelebung:
  anzahl: 2
  schleichen-sekunden: 3
  ausbluten-sekunden: 6
treppe-sekunden: 2
ende-sekunden: 4
wellen-pause-ticks: 20
startwert: ${SEED:-0}
EOF2
# Test-Netzwerk: Bert spricht Englisch, Anna und Bert sind eine Party (Anna führt).
cat > "$SERVER/plugins/MoinMorni/fake.properties" <<'EOF'
lang.Bert=en
party.Anna=Anna,Bert
EOF
rm -f "$SERVER/plugins/MoinMorni/calls.log"
rm -rf "$SERVER/plugins/MorniBurgverlies/spieler" "$SERVER/plugins/MorniBurgverlies/bestenliste.yml"
# Alte Jars weg (auch solche aus einer Fassung mit anderem Namen), damit nie zwei Fassungen laden.
rm -f "$SERVER"/plugins/*.jar
cp "$REPO/target/Morni-Burgverlies.jar" "$SERVER/plugins/"
cp "$REPO/test/fake-network/target/MoinMorni-FakeNetwork.jar" "$SERVER/plugins/"
for jar in ${EXTRA_PLUGINS-$CACHE/jars/ViaVersion-5.12.2.jar $CACHE/jars/ViaBackwards-5.12.2.jar}; do
  cp "$jar" "$SERVER/plugins/"
done

echo "== Server starten (Port $PORT, RCON $RCON_PORT)"
cd "$SERVER"
: > console.log
"$JAVA" -Xms1G -Xmx2G -jar paper.jar --nogui < /dev/null > console.log 2>&1 &
PID=$!
stop_server() {
  if kill -0 "$PID" 2>/dev/null; then
    node "$HERE/rcon.cjs" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" stop >/dev/null 2>&1 || kill "$PID" || true
    for _ in $(seq 1 60); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
    kill -9 "$PID" 2>/dev/null || true
  fi
}
if [ "${KEEP:-0}" != "1" ]; then
  trap stop_server EXIT
fi

for _ in $(seq 1 240); do
  if grep -q "Burgverlies ist bereit" console.log; then break; fi
  if ! kill -0 "$PID" 2>/dev/null; then echo "Server ist abgestürzt:"; tail -40 console.log; exit 1; fi
  sleep 1
done
grep -q "Burgverlies ist bereit" console.log || { echo "Burgverlies wurde nicht bereit:"; tail -40 console.log; exit 1; }
grep -E "Torhaus gebaut|Burgverlies ist bereit|Netzwerk" console.log || true

if [ "${SKIP_TEST:-0}" = "1" ]; then
  echo "Server läuft (PID $PID) – anhalten mit: node $HERE/rcon.cjs 127.0.0.1:$RCON_PORT $SERVER/.rcon-pass stop"
  exit 0
fi

echo "== Test-Spieler"
STATUS=0
node "$HERE/burgverlies-test.cjs" 127.0.0.1 "$PORT" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" "$SERVER" || STATUS=$?

echo "== Fehler im Server-Log:"
if grep -qE "ERROR|Exception" console.log; then
  grep -E "ERROR|Exception" console.log | head -20
else
  echo "keine"
fi
exit $STATUS
