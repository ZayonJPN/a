# ZarTabNPC 2.1.0 — fallback Bedrock (experimental)

O plugin agora cria uma segunda representacao da cabeca usando ArmorStand invisivel com capacete PLAYER_HEAD. Jogadores Java veem o ItemDisplay original; jogadores identificados pelo Floodgate veem a representacao alternativa. A entidade Interaction continua sendo usada para cliques.

## Requisitos
- Paper 1.21.x, Java 21, Geyser e Floodgate instalados e configurados para identificar jogadores Bedrock.
- GitHub Actions > Build > Run workflow ou push para compilar; baixar artifact `plugin`.

## Instalacao
1. Fazer backup do JAR e de `plugins/ZarTabNPC/heads.yml` e `config.yml`.
2. Substituir o JAR e reiniciar o servidor em janela de manutencao.
3. Em `config.yml`, `heads.bedrock-fallback.enabled: true`.
4. Testar com um jogador Java e outro Bedrock.

## Limitacoes
- A cabeca Bedrock e do tamanho de um capacete em ArmorStand, nao reproduz a escala gigante de ItemDisplay.
- O resultado visual e a interacao no Bedrock dependem da versao e configuracao do Geyser/Floodgate. Nao foi testado em servidor real.
- Sem Floodgate, o plugin nao identifica automaticamente jogadores Bedrock e mostra apenas a entidade Java.
- Caso a textura de cabeca personalizada nao apareca, testar uma cabeca vanilla ou perfil valido: skins personalizadas podem ter restricoes adicionais no Bedrock.
