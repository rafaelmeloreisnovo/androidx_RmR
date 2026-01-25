# Guia de Performance Bruta (CPU/GC/IOPS) — RmR + AndroidX

> **Objetivo:** listar, de forma direta, onde arrancar performance bruta no stack (do maior ganho por linha ao maior ganho por arquitetura) e como executar. Foco em determinismo e hot paths (Compose/Camera/Room/loops Java), com prioridade para redução de alocação, cache misses, branches e cópias.

---

## Regra de ouro do ganho bruto

**Ordem de impacto real (quase sempre):**

1. **Tirar GC/alocação do hot path**
2. **Reduzir cache-miss / melhorar locality**
3. **Reduzir branches e chamadas virtuais**
4. **Evitar cópias / conversões**
5. **Evitar syscalls / I/O**

Se você fizer 1 + 2 direito, o resto vira polimento.

---

## 1) Onde já há ganhos no RmR — e onde ainda dá para ganhar mais

### 1.1 RmRMatrixOps / RmRState — reuse de buffer

**Problema atual (GC):** criação recorrente de `double[]` em operações e transposes cria lixo e dispara GC.

**Como ganhar bruto (método):**

- Criar variantes *Into* e/ou *Buffer Pool*:
  - `multiplyInto(left, right, out)`
  - `applyInto(input, out)`
  - `transposeInto(right, transposedTmp)`
- Implementar `ThreadLocal<Buffers>`:
  - `double[] out`
  - `double[] transposed`
  - `double[] tmp`

**Ganho esperado:**

- **+10% a +60%** em loops médios.
- **Até 2×** em cenários de alta frequência (GC praticamente some).

---

### 1.2 Heurística de `multiply` — auto-tuning

**Problema:** bloco fixo (ex.: 32) pode ser subótimo dependendo de L1/L2 e do shape da matriz.

**Como ganhar bruto:**

- Ajustar tamanho de bloco por CPU/cache:
  - Testar 32, 48, 64
- Heurística por shape:
  - `cols` pequeno → unroll + transpose OFF
  - `resultCols` grande → transpose ON

**Ganho esperado:** **+5% a +25%** (dependente do shape).

---

### 1.3 RmRNavigationState / RmRLifecycleState — remover `clone`

**Problema:** `stateVector.clone()` aloca toda vez.

**Como ganhar bruto:**

- Expor referência imutável (sem clone):
  - **Opção A:** documentar “não mutar”.
  - **Opção B:** devolver wrapper *read-only* (sem alocar).

**Ganho esperado:** **+1% a +10%** se for chamado com frequência.

---

### 1.4 RmRPreferenceStore — hash paralelo + normalização

**Como ganhar bruto:**

- Manter `int[] hashes` paralelo a `keys[]` para evitar `equals` caro.
- Normalizar keys (intern/pool) quando a origem é controlada.

**Ganho esperado:** **+5% a +30%** em workloads grandes.

---

### 1.5 RmRQueryCache — linear search adaptativo

**Problema:** busca linear escala mal com tamanho do cache.

**Como ganhar bruto:**

- `int[] keyHash` paralelo.
- Fast path para índices 0 e 1.
- Bloom 64-bit (bitset) para negar miss rápido.

**Ganho esperado:** **+10% a +100%** (dependente do hit-rate e tamanho).

---

## 2) AndroidX (peso bruto real)

### 2.1 Compose — maior retorno bruto

**Como ganhar bruto:**

- Reduzir recomposição:
  - Garantir `@Stable` / `@Immutable`.
  - Evitar criar lambdas/objetos em cada frame.
  - `remember` e `derivedStateOf`.
  - Evitar listas mutáveis sem chave.
- Reduzir alocação por frame:
  - Evitar `map/filter` dentro de composables.
  - Mover cálculos para fora e memoizar.
- Layout:
  - Evitar nesting profundo.
  - Preferir `Lazy*` com keys estáveis.

**Ganho esperado:** **+5% a +30% FPS** em UI pesada.

---

### 2.2 Camera — estabilidade e init

**Como ganhar bruto:**

- Reduzir fallback loops / retries.
- Cache de configurações por device.
- Evitar conversões YUV→RGB desnecessárias.
- Reusar buffers e evitar cópias.

**Ganho esperado:** **+0% a +15%** (device-dependent), menor aquecimento.

---

### 2.3 Room / SQLite — apps offline

**Como ganhar bruto:**

- Query cache real.
- Evitar mapeamento repetido / alloc de cursor.
- Prepared statements e reuso.

**Ganho esperado:** **+5% a +40%** em workloads de DB.

---

## 3) Sistema (celular) — ganhos sem mexer em código

### 3.1 I/O e armazenamento

- Manter trabalho em `/data/data/...` (evita camada lenta do `/sdcard`).
- Evitar milhares de arquivos pequenos (zip/tar).
- Usar `tmpfs` quando possível.

**Ganho esperado:** **+10% a +50%** em operações de build/scripts e manipulação de repo.

### 3.2 Afinidade térmica

- Evitar saturar big cores por muito tempo.
- Executar em bursts curtos para reduzir throttling.

**Ganho esperado:** melhora de performance sustentada (não pico).

---

## 4) Checklist “GANHAR TUDO” (ordem de execução)

### Fase 1 — tirar GC do RmR

1. Buffer reuse em `MatrixOps` + `State`
2. Remover `clone()` de vetores
3. Hashes paralelos em store/cache

### Fase 2 — atacar Compose

4. Cortar recomposição e alocação por frame
5. Estabilizar `state/keys`

### Fase 3 — Camera/Room

6. Caches e buffers, reduzir cópias/fallbacks

---

## Conclusão direta

Se você quer o maior ganho determinístico no menor tempo, priorize **buffer reuse em MatrixOps/State**. Isso é o que mais corta GC e entrega performance bruta de forma consistente.

