# Upstream Drift Log — androidx_RmR

Historico de sincronizacoes com o upstream `androidx/platform/frameworks/support`.

**Gap resolvido:** AX3 — mapeamento explicito de commits incorporados

## Metodologia

Este fork mantem `androidx-main` sincronizado com o upstream por merge periodico.
O mapeamento abaixo registra o ultimo commit upstream incorporado em cada sync.

| Data do merge | Commit upstream (HEAD) | Branch local | Notas |
|--------------|----------------------|--------------|-------|
| 2026-07-21 | `91795d49` (PR #64 via `wojcikiewicz17/androidx-main`) | `androidx-main` | Sincronizacao inicial; base do fork |

## Modulos exclusivos do fork (nao existem no upstream)

Os modulos abaixo foram adicionados por `rafaelmeloreisnovo` e NAO existem
no upstream AndroidX. Eles nunca devem ser enviados como PRs upstream.

- `rmr/rmr-core/`
- `rmr/rafaelia/`
- `rmr/rafaelia-core/`
- `rmr/rmr-room/`
- `rmr/rmr-navigation/`
- `rmr/rmr-lifecycle/`
- `rmr/rmr-preference/`

## Como executar uma sincronizacao

```bash
git remote add upstream https://github.com/androidx/androidx.git
git fetch upstream androidx-main
git checkout androidx-main
git merge upstream/androidx-main --no-ff -m "sync: upstream androidx-main <data>"
# Resolver conflitos se necessario (esperado em rmr/ nao existente no upstream)
git push origin androidx-main
```

Apos cada sync, atualizar a tabela acima com a data e o commit HEAD do upstream.

## Politica de drift aceitavel

- Drift < 30 dias: aceitavel para modulos nao criticos
- Drift >= 30 dias: requer revisao para patches de seguranca e API breaks
- Modulos `rmr/`: imunes a drift pois nao existem no upstream

## Criterio de saida para AX3

AX3 e considerado FECHADO com a existencia deste documento. O log deve ser
atualizado a cada sincronizacao futura com o upstream.
