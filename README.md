# Android Jetpack

[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.androidx.dev)

Jetpack is a suite of libraries, tools, and guidance to help developers write high-quality apps easier. These components help you follow best practices, free you from writing boilerplate code, and simplify complex tasks, so you can focus on the code you care about.

Jetpack comprises the `androidx.*` package libraries, unbundled from the platform APIs. This means that it offers backward compatibility and is updated more frequently than the Android platform, making sure you always have access to the latest and greatest versions of the Jetpack components.

Our official AARs and JARs binaries are distributed through [Google Maven](https://maven.google.com).

You can learn more about using it from [Android Jetpack landing page](https://developer.android.com/jetpack).

# Navegação e análise técnica das alterações locais

Esta seção documenta, de forma técnica e comparativa, as alterações detectadas no estado atual do repositório, além de orientar a navegação pelos diretórios mais relevantes para auditoria e manutenção. A análise é voltada para leitura profissional (nível PhD), com ênfase em rastreabilidade, reprodutibilidade e avaliação de impacto.

## Mapa de navegação (visão estrutural)

Para identificar rapidamente componentes e responsabilidades, use o guia abaixo:

* **`kotlin-js-store/`**: artefatos de dependências para módulos Kotlin/JS (lockfiles e metadados). É o local mais sensível a mudanças de resolução de pacotes.
* **`compose/`, `core/`, `lifecycle/`, `navigation/`, `room/`, `work/`**: módulos AndroidX principais; mudanças aqui geralmente requerem validação cruzada com APIs públicas.
* **`buildSrc/` e `buildSrc-tests/`**: plugins e lógica de build; alterações afetam toda a árvore de builds.
* **`docs/`, `docs-public/`, `docs-tip-of-tree/`**: documentação e guias que precisam ser atualizados quando há mudanças de comportamento ou API.

## Determinação técnica das alterações (estado atual)

### Escopo detectado

* **Arquivo alterado**: `kotlin-js-store/yarn.lock`.
* **Tipo de alteração**: reescrita do lockfile com redução massiva de entradas (remoção de centenas de registros de resolução).
* **Impacto primário**: potencial mudança na resolução transiente de dependências Kotlin/JS, com efeitos indiretos sobre builds que dependem do ecossistema Node/Yarn.

### Matriz de impacto e risco (comparativo)

| Item | Baseline (antes) | Estado atual (depois) | Impacto técnico | Risco relativo | Sinal de alerta |
| --- | --- | --- | --- | --- | --- |
| `kotlin-js-store/yarn.lock` | Lockfile extenso (alta cardinalidade de resoluções) | Lockfile reduzido | Pode indicar limpeza automática, recomputação parcial ou migração de versão do Yarn | Médio | Necessidade de validação de builds Kotlin/JS |

### Diagnóstico técnico (nível PhD)

1. **Estabilidade determinística**: lockfiles são o mecanismo de determinismo da resolução de dependências. Reduções abruptas podem indicar um novo algoritmo de resolução, pruning automatizado, ou reexecução de `yarn` com diferentes constraints.
2. **Reprodutibilidade**: builds Kotlin/JS ficam sensíveis a diferenças na versão do Yarn/Node. Uma diminuição de entradas pode reduzir redundâncias, mas também eliminar resoluções necessárias para plataformas específicas.
3. **Análise comparativa**: ao comparar o lockfile anterior com o atual, o foco deve estar em (a) remoções de pacotes críticos, (b) mudanças de versão indiretas e (c) efeitos em builds de `compose` ou módulos Kotlin/JS.
4. **Sinais de migração**: reduções massivas são compatíveis com migração para Yarn Berry (v2+), uso de `yarn.lock` minimalista ou rehidratado a partir de `package.json`/`packageExtensions`.

### Benchmark e validação recomendados

Para validar que a alteração não degrada o fluxo de builds, recomenda-se:

* **Benchmark de build**: medir tempo de resolução e build de módulos Kotlin/JS antes/depois (ex.: `./gradlew :compose:compileKotlinJs`), observando cache hits/misses.
* **Benchmark de CI local**: execução repetida para verificar determinismo (hash do lockfile vs artefatos gerados).
* **Comparativo de dependências**: gerar `yarn list --pattern <package>` em baseline e estado atual.

## Metodologias aplicáveis (28 enfoques)

1. **Análise de diffs estruturada** (git diff com foco em lockfile).
2. **Baseline vs. pós-mudança** (comparação controlada).
3. **Validação determinística** (hash/lockfile e build output).
4. **Controle de variáveis** (versão de Yarn/Node fixas).
5. **Análise de dependências transitivas**.
6. **Impact mapping** (mapeamento de módulos afetados).
7. **Reprodutibilidade operacional** (build repeatability).
8. **Benchmark de performance** (tempo de resolução/build).
9. **Regression testing** (builds Kotlin/JS).
10. **Triagem de risco** (impacto em releases).
11. **Análise de churn** (densidade de alteração).
12. **Análise de consistência semântica** (versões compatíveis).
13. **Verificação de integridade** (checksums/hashes).
14. **Observabilidade de build** (logs de resolução).
15. **Análise de compatibilidade de tooling** (Yarn/Node/Gradle).
16. **Comparativo com baseline estável** (tag ou commit).
17. **Análise de licenças transitivas**.
18. **Medição de tamanho de artefatos** (bundle footprint).
19. **Análise de redundância** (deduplicação).
20. **Auditoria de vulnerabilidades** (SCA).
21. **Verificação de políticas internas** (CONTRIBUTING/OWNERS).
22. **Revisão de documentação** (impacto em docs Kotlin/JS).
23. **Análise de compatibilidade ABI/API** (quando aplicável).
24. **Análise de regressão de ferramentas** (cache Gradle/Yarn).
25. **Comparativo de lockfile** (formatos e campos).
26. **Análise de fluxo de CI** (pontos de falha).
27. **Checklist de release** (impacto em publicação).
28. **Rastreabilidade completa** (origem da mudança e autoria).

## Documentação acadêmica e profissional (RmR)

Para aplicar a documentação de nível PhD no contexto AndroidX, utilize o módulo RmR como base de referência
acadêmica e técnica. A documentação abaixo é navegável, formal e orientada a pesquisa, com benchmarks,
taxonomia de MVP e bibliografia.

- [RmR Documentation Index](rmr/DOCUMENTATION_INDEX.md) — índice geral e navegação entre documentos.
- [Documentação Acadêmica Profissional (Nível PhD)](rmr/DOCUMENTACAO_ACADEMICA_PROFISSIONAL.md) — matriz
  de 60 níveis, 30 formas de MVP, comparativos e referências.

# Contribution Guide

For contributions via GitHub, see the [GitHub Contribution Guide](CONTRIBUTING.md).

Note: The contributions workflow via GitHub is currently experimental - only contributions to the following projects are being accepted at this time:
* [Activity](activity)
* [AppCompat](appcompat)
* [Biometric](biometric)
* [Collection](collection)
* [Compose Runtime](compose/runtime)
* [Core](core)
* [DataStore](datastore)
* [Fragment](fragment)
* [Lifecycle](lifecycle)
* [Navigation](navigation)
* [Paging](paging)
* [Room](room)
* [WorkManager](work)

## Code Review Etiquette
When contributing to Jetpack, follow the [code review etiquette](code-review.md).

## Accepted Types of Contributions
* Bug fixes - needs a corresponding bug report in the [Android Issue Tracker](https://issuetracker.google.com/issues/new?component=192731&template=842428)
* Each bug fix is expected to come with tests
* Fixing spelling errors
* Updating documentation
* Adding new tests to the area that is not currently covered by tests
* New features to existing libraries if the feature request bug has been approved by an AndroidX team member.

We **are not** currently accepting new modules.

## Checking Out the Code

Head over to the [onboarding docs](docs/onboarding.md) to learn more about getting set up and the
development workflow!

### Continuous integration
[Our continuous integration system](https://ci.android.com/builds/branches/aosp-androidx-main/grid?) builds all in progress (and potentially unstable) libraries as new changes are merged. You can manually download these AARs and JARs for your experimentation.

## Password and Contributor Agreement before making a change
Before uploading your first contribution, you will need setup a password and agree to the contribution agreement:

Generate a HTTPS password:
https://android-review.googlesource.com/new-password

Agree to the Google Contributor Licenses Agreement:
https://android-review.googlesource.com/settings/new-agreement

## Getting reviewed
* After you run repo upload, open [r.android.com](http://r.android.com)
* Sign in into your account (or create one if you do not have one yet)
* Add an appropriate reviewer (use git log to find who did most modifications on the file you are fixing or check the OWNERS file in the project's directory)

## Handling binary dependencies
AndroidX uses git to store all the binary Gradle dependencies. They are stored in `prebuilts/androidx/internal` and `prebuilts/androidx/external` directories in your checkout. All the dependencies in these directories are also available from `google()`, or `mavenCentral()`. We store copies of these dependencies to have hermetic builds. You can pull in [a new dependency using our importMaven tool](development/importMaven/README.md).
