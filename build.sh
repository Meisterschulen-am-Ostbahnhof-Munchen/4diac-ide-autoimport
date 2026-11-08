#!/usr/bin/env bash
# Builds the plugin jar against the plugins of a 4diac IDE Windows ZIP.
#   IDE_ZIP  path to a local 4diac IDE ZIP (optional; downloaded from IDE_URL otherwise)
# Output: dist/de.meisterschulen.fordiac.ide.autoimport_<version>.jar
set -euo pipefail
cd "$(dirname "$0")"

IDE_URL="https://github.com/Meisterschulen-am-Ostbahnhof-Munchen/4diac-ide-release-history-milestone/releases/download/4diac-ide_3.4.0_milestone_2026-11-05_2214/4diac-ide_3.4.0-win32.win32.x86_64_milestone_2026-11-05_2214.zip"
IDE_SHA256="e6f821ddeb60cb06258292c76a80a36eaf456d6f31f7c679d899ca00b56269c9"

BSN=$(sed -n 's/^Bundle-SymbolicName: *\([^;]*\).*/\1/p' META-INF/MANIFEST.MF | tr -d '\r')
VER=$(sed -n 's/^Bundle-Version: *//p' META-INF/MANIFEST.MF | tr -d '\r')
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
JAR="${JAVA_HOME:+$JAVA_HOME/bin/}jar"

mkdir -p build
if [ -z "${IDE_ZIP:-}" ]; then
  IDE_ZIP=build/ide.zip
  [ -f "$IDE_ZIP" ] || curl -fsSL -o "$IDE_ZIP" "$IDE_URL"
fi
echo "$IDE_SHA256  $IDE_ZIP" | sha256sum -c -

rm -rf build/plugins build/classes dist
unzip -q -o "$IDE_ZIP" '4diac-ide/plugins/org.eclipse.core.resources_*.jar' \
  '4diac-ide/plugins/org.eclipse.core.runtime_*.jar' '4diac-ide/plugins/org.eclipse.core.jobs_*.jar' \
  '4diac-ide/plugins/org.eclipse.equinox.common_*.jar' '4diac-ide/plugins/org.eclipse.equinox.registry_*.jar' \
  '4diac-ide/plugins/org.eclipse.osgi_*.jar' '4diac-ide/plugins/org.eclipse.ui.workbench_*.jar' -d build
CP=$(ls build/4diac-ide/plugins/*.jar | paste -sd "$( [ "${OS:-}" = Windows_NT ] && echo ';' || echo ':')" -)

"$JAVAC" --release 21 -encoding UTF-8 -Xlint:all -Werror -cp "$CP" -d build/classes \
  $(find src -name '*.java')
cp plugin.xml build/classes/
mkdir -p dist
"$JAR" --create --file "dist/${BSN}_${VER}.jar" --manifest META-INF/MANIFEST.MF -C build/classes .
( cd dist && sha256sum "${BSN}_${VER}.jar" > "${BSN}_${VER}.jar.sha256" && cat "${BSN}_${VER}.jar.sha256" )
