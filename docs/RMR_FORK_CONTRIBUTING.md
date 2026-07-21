# RmR Fork — Guia de Contribuicao

Este documento descreve o processo de contribuicao especifico do fork
`rafaelmeloreisnovo/androidx_RmR`. Para contribuicao ao AndroidX upstream
oficial, consulte `CONTRIBUTING.md` na raiz do repositorio.

## Escopo do fork

Este fork adiciona os modulos `rmr/` ao AndroidX:

| Modulo | Funcao |
|--------|--------|
| `rmr/rmr-core` | Primitivas nativas: CRC32c, arena, CAS, dispatch |
| `rmr/rafaelia` | Integracao Rafaelia (benchmark, trace, bridge) |
| `rmr/rafaelia-core` | Componentes core da plataforma Rafaelia |
| `rmr/rmr-room` | Extensao Room com checksums RmR |
| `rmr/rmr-navigation` | Extensao Navigation com auditoria de rotas |
| `rmr/rmr-lifecycle` | Extensao Lifecycle com supervisor de processos |
| `rmr/rmr-preference` | Extensao Preference com cifra Android Keystore |

## Como contribuir ao fork

### Mudancas nos modulos rmr/

1. Crie uma branch a partir de `androidx-main`: `git checkout -b rmr/sua-feature`
2. Faca suas modificacoes somente em `rmr/`
3. Execute o CI local: `./gradlew :rmr:rmr-core:test :rmr:rafaelia:test`
4. O workflow `.github/workflows/rmr-native-ci.yml` valida automaticamente em push

### Mudancas no AndroidX upstream

Nao modifique modulos AndroidX fora de `rmr/` neste fork.
Contribuicoes ao AndroidX upstream devem ser feitas diretamente em
`https://cs.android.com/androidx/platform/frameworks/support`.

### Sincronizacao com upstream

Veja `docs/UPSTREAM_DRIFT_LOG.md` para o historico de merges do upstream.

## CI do fork

O workflow `rmr-native-ci.yml` executa em pull requests e pushs para `main`:
- Builda todos os 7 modulos rmr/ com ABI `arm64-v8a`
- Aceita ABI customizada via `workflow_dispatch` (input `native_abis`)

## Contato

Owner: `rafaelmeloreisnovo` — abra issues neste repositorio para duvidas
sobre os modulos rmr/. Para duvidas sobre o AndroidX base, use o tracker
oficial em `https://issuetracker.google.com/`.
