#!/usr/bin/env bash
# Teste de fumaça: instala o APK no emulador, abre cada aba, tira prints e falha se o app travar.
set -u
PKG=br.app.relatoriocampo
mkdir -p telas
MISSING=0
adb install -r saida/RelatorioCampo.apk
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb logcat -c

tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1
  local xy
  xy=$(python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
want = sys.argv[1]
for n in ET.parse('ui.xml').iter('node'):
    if n.get('text') == want or n.get('content-desc') == want:
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; sleep 2; else echo "Texto não encontrado: $1" | tee -a telas/resumo.txt; MISSING=$((MISSING+1)); fi
}
# toca na n-ésima ocorrência de qualquer um dos textos (separados por |), sem diferenciar maiúsculas
tap_any() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1
  local xy
  xy=$(python3 - "$1" "${2:-1}" <<'PY'
import re, sys, xml.etree.ElementTree as ET
wants = [w.lower() for w in sys.argv[1].split('|')]
n = int(sys.argv[2]); found = 0
for node in ET.parse('ui.xml').iter('node'):
    label = (node.get('text') or node.get('content-desc') or '').strip().lower()
    if label in wants:
        found += 1
        if found == n:
            x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; sleep 2; return 0; fi
  return 1
}
shot() { adb exec-out screencap -p > "telas/$1.png"; }

adb shell am start -W -n $PKG/.MainActivity
sleep 6
shot 1-mes
adb shell input swipe 540 1800 540 600 400; sleep 1; shot 2-mes-rolado

tap_text "Registros"; shot 3-registros
tap_text "+1 h"; tap_text "+30 min"; tap_text "Adicionar"; sleep 1
adb shell input swipe 540 1800 540 700 400; sleep 1; shot 4-registro-adicionado

tap_text "Estudos"; shot 5-estudos
tap_text "Ano"; shot 6-ano
tap_text "Ajustes"; shot 7-ajustes

# ---- widgets: adiciona os 4 pela tela de Ajustes e confirma a janela do Android ----
for k in 1 2 3 4; do
  ok=0
  for tentativa in 1 2 3 4; do
    if tap_any "Adicionar" "$k"; then ok=1; break; fi
    adb shell input swipe 540 1700 540 1100 300; sleep 1
  done
  if [ $ok -eq 1 ]; then
    sleep 2; shot "w$k-dialogo"
    tap_any "Add to home screen|Add automatically|Adicionar à tela inicial|Add" || { echo "Confirmação do widget $k não encontrada" | tee -a telas/resumo.txt; MISSING=$((MISSING+1)); adb shell input keyevent KEYCODE_BACK; }
    sleep 2
  else
    echo "Botão Adicionar $k não encontrado" | tee -a telas/resumo.txt; MISSING=$((MISSING+1))
  fi
done
adb shell input keyevent KEYCODE_HOME; sleep 4
shot w5-tela-inicial
adb shell input swipe 900 1200 150 1200 300; sleep 2; shot w6-tela-inicial-2
adb shell input swipe 150 1200 900 1200 300; sleep 2
if tap_any "+15" || { adb shell input swipe 900 1200 150 1200 300; sleep 2; tap_any "+15"; }; then
  sleep 4; shot w7-apos-mais-15
else
  echo "Widget do cronômetro não encontrado na tela inicial" | tee -a telas/resumo.txt; MISSING=$((MISSING+1))
fi
adb shell am start -W -n $PKG/.MainActivity; sleep 3
tap_text "Ajustes"
for i in 1 2 3; do adb shell input swipe 540 600 540 1800 200; done
for i in 1 2 3 4 5; do adb shell input swipe 540 1800 540 500 300; sleep 1; done
tap_text "Escuro"; sleep 1; shot 8-ajustes-escuro
tap_text "Mês"; sleep 1; shot 9-mes-escuro
tap_text "ES"; sleep 1; shot 10-mes-espanhol

adb logcat -d > telas/logcat.txt
adb logcat -d -s AndroidRuntime:E > telas/erros.txt || true
echo "Botões não encontrados: $MISSING" | tee -a telas/resumo.txt
if grep -E "FATAL EXCEPTION|AndroidRuntime: Process: $PKG" telas/logcat.txt; then
  echo "O app travou" | tee -a telas/resumo.txt; exit 1
fi
adb shell pidof $PKG || { echo "O app não está rodando"; exit 1; }
[ "$MISSING" -eq 0 ] || { echo "Navegação incompleta"; exit 1; }
echo "Teste no emulador OK" | tee -a telas/resumo.txt
