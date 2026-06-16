# AutoResponderShopee

App Android em Kotlin que usa `AccessibilityService` para responder avaliações na Shopee sem cliques por coordenadas fixas.

## Fluxo automatizado

1. Com o botão flutuante em `Play`, inicia a automação.
2. Encontra todos os elementos visíveis com texto ou descrição `Responder`.
3. Ordena os candidatos pela posição na tela e clica no segundo `Responder`, ignorando o título do topo.
4. Aguarda a tela de resposta carregar.
5. Encontra o campo editável com hint/texto `Sua Resposta...`.
6. Insere a mensagem salva no app.
7. Encontra o botão `ENVIAR`.
8. Clica em `ENVIAR`.

## Uso no Android

1. Instale o APK.
2. Abra o app.
3. Edite e salve a mensagem padrão.
4. Toque em **Permitir botão flutuante** e libere a sobreposição.
5. Toque em **Abrir acessibilidade** e ative `AutoResponderShopee`.
6. Abra a Shopee na tela de avaliações.
7. Toque no botão flutuante **Play** para iniciar ou **Pausar** para parar.

## Build no GitHub

O workflow `.github/workflows/android-debug.yml` compila o APK debug e publica o artefato `AutoResponderShopee-debug-apk`.

Para baixar:

1. Abra a aba **Actions** do repositório.
2. Selecione o workflow **Android Debug Build**.
3. Abra a execução mais recente.
4. Baixe o artifact **AutoResponderShopee-debug-apk**.
5. Extraia o ZIP e instale `app-debug.apk` no Android.

Depois de instalar, libere a sobreposição, ative o serviço de acessibilidade e use o botão flutuante **Play** na tela da Shopee.
