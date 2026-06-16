# AutoResponderShopee

App Android em Kotlin que usa `AccessibilityService` para responder avaliações na Shopee sem cliques por coordenadas fixas.

## Fluxo automatizado

1. Encontra o primeiro elemento clicável com texto ou descrição `Responder`.
2. Clica nesse elemento.
3. Aguarda a tela de resposta carregar.
4. Encontra o campo editável com hint/texto `Sua Resposta...`.
5. Insere a mensagem configurada no serviço.
6. Encontra o botão `ENVIAR`.
7. Clica em `ENVIAR`.

## Build no GitHub

O workflow `.github/workflows/android-debug.yml` compila o APK debug e publica o artefato `AutoResponderShopee-debug-apk`.

Para baixar:

1. Abra a aba **Actions** do repositório.
2. Selecione o workflow **Android Debug Build**.
3. Abra a execução mais recente.
4. Baixe o artifact **AutoResponderShopee-debug-apk**.
5. Extraia o ZIP e instale `app-debug.apk` no Android.

Depois de instalar, abra o app e toque em **Abrir acessibilidade** para ativar o serviço.
