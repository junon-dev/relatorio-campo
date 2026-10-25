# Instruções para o Claude neste repositório

App Android **Relatório de Campo** (Kotlin + Jetpack Compose). O dono do projeto é Junon (português do Brasil; o app é bilíngue PT/ES).

## Entrega padrão de cada nova versão
Depois de enviar qualquer mudança para `main`:
1. Acompanhe o workflow "Gerar APK" até terminar (`gh run watch <id> --exit-status`). Os jobs `build` e `emulador` precisam passar.
2. Confira os prints e o `resumo.txt` no branch `ci-telas` (`git clone --depth 1 -b ci-telas ...`); o armazenamento de artefatos do Actions não é acessível pela sessão.
3. Baixe o APK publicado e **envie o arquivo direto na conversa** (SendUserFile), sem esperar o usuário pedir:
   `gh api -H "Accept: application/octet-stream" repos/junon-dev/relatorio-campo/releases/assets/<id> > RelatorioCampo.apk`
   (pegue o `<id>` de `RelatorioCampo.apk` em `gh api repos/junon-dev/relatorio-campo/releases/latest`).

## Notas
- A lógica pura fica em `app/src/main/java/br/app/relatoriocampo/domain/` e tem testes em `app/src/test/` (rodam no CI).
- Textos PT/ES ficam em `domain/Strings.kt`; os dois idiomas precisam ter as mesmas chaves (há teste para isso).
- A chave de assinatura em `keystore/` é fixa de propósito, para que as atualizações instalem por cima sem perder dados.
