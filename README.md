# Relatório de Campo — app Android

App nativo em **Kotlin + Jetpack Compose** (Material 3) para registrar horas e enviar o relatório de serviço de campo. Funciona 100% offline, em português e espanhol.

## Baixar o app (sem instalar nada no computador)

Abra no celular: **https://github.com/junon-dev/relatorio-campo/releases/latest** e toque em **RelatorioCampo.apk**.
Na primeira vez, o Android pede para permitir a instalação pelo navegador; é só permitir. As novas versões instalam por cima da anterior, sem perder os dados.

Cada envio para o branch `main` faz o GitHub Actions rodar os testes, compilar o APK, testá-lo num emulador Android 14 (com prints das telas no branch `ci-telas`) e publicar a nova versão em Releases.

## Compilar no próprio computador (opcional)

1. Instale o **Android Studio** (gratuito): https://developer.android.com/studio
2. Descompacte este `.zip` e, no Android Studio, use **File → Open** e escolha a pasta `RelatorioCampo`.
3. Espere o Gradle sincronizar. Na primeira vez ele baixa as bibliotecas, o que leva alguns minutos.
   - Se aparecer uma sugestão de "Upgrade" do Android Gradle Plugin, pode aceitar.
4. No celular, ative **Opções do desenvolvedor → Depuração USB** (toque 7 vezes em *Número da versão* em Configurações → Sobre o telefone) e conecte-o ao computador pelo cabo.
5. Clique em **Run ▶** (Shift+F10). O app é instalado e aberto no celular.

Para gerar um APK e instalar em outros aparelhos, use **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. O arquivo fica em `app/build/outputs/apk/debug/app-debug.apk`. Para publicar na Play Store, crie uma chave própria em **Build → Generate Signed App Bundle**.

## O que o app faz

| Aba | Função |
|---|---|
| **Mês** | Resumo em destaque, categoria do mês (publicador, pioneiro auxiliar ou regular), participação, estudos, lançamento **por dia** ou **só o total**, cronômetro e envio do relatório (Compartilhar → WhatsApp, e-mail…) |
| **Registros** | Lançamento de horas de campo ou de crédito por dia, com atalhos (+15 min, +30 min, +1 h, +2 h) e opção de desfazer a exclusão |
| **Estudos** | Estudos bíblicos e revisitas, ativos ou inativos |
| **Ano** | Ano de serviço (setembro a agosto) e meta anual proporcional do pioneiro regular |
| **Ajustes** | Nome, lembretes, regras (meta anual, limite de crédito), tema claro/escuro e backup |

**Lembretes nativos:** uma notificação alguns dias antes do fim do mês (5 dias por padrão, opcionalmente todos os dias até o fim) e outra no dia de enviar o relatório. A segunda só aparece se o relatório do mês anterior ainda não estiver marcado como enviado. Os lembretes são reagendados automaticamente quando o celular reinicia.

**Backup compatível com a versão HTML:** o arquivo exportado pela versão `relatorio-servico-campo.html` pode ser importado no app em **Ajustes → Backup → Importar**, e o caminho inverso também funciona.

## Estrutura do código

```
app/src/main/java/br/app/relatoriocampo/
├── domain/        Lógica pura (cálculos, textos PT/ES, datas dos lembretes) — coberta por testes
├── data/          Banco Room, configurações e backup JSON
├── notify/        Notificações (AlarmManager + reagendamento após reiniciar)
└── ui/            Telas em Jetpack Compose e tema
app/src/test/      Testes da lógica (rode com: ./gradlew test)
```

## Versões usadas

Android Gradle Plugin 8.7.3 · Gradle 8.9 · Kotlin 2.0.21 · Compose BOM 2024.12.01 · Room 2.6.1 · minSdk 26 (Android 8.0) · targetSdk 35 (Android 15).

> Confira as regras (meta anual, limite de crédito) com as orientações mais recentes recebidas na congregação. Os valores podem ser alterados em Ajustes.
