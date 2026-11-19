# ZarTabNPC 2.0

Plugin para Paper 1.21.x com duas coisas:

1. **Cabeças gigantes girando** (ItemDisplay) com **comandos no clique** (esquerdo ou direito).
2. **TAB próprio**: header/footer animados, nome na tab, nametag, ordenação por grupo e scoreboard.

## Como compilar
Suba `pom.xml`, `src/` e `.github/` num repositório do GitHub -> aba Actions -> baixe o artifact `plugin`.

## Cabeças
```
/hdb                              (pegue a cabeça e segure na mão)
/zhead create smp                 (cria na sua posição com a cabeça da mão)
/zhead addcmd smp [connect] Semi-anarquia
/zhead scale smp 3
/zhead up smp 0.5
```
Tipos de comando: `[connect] servidor`, `[player] cmd`, `[console] cmd`, `[message] texto`, `[sound] nome`.

## TAB
Tudo no `plugins/ZarTabNPC/config.yml`. Remova o plugin TAB antigo (ou `tab.enabled: false`).
Para prefixos/grupos: `/papi ecloud download LuckPerms` e `/papi reload`.
