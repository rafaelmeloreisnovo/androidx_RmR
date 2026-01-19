# RmR Autoria, Atribuição e Licenciamento

## Propósito

Este documento registra **quem criou o quê**, **qual licença se aplica** e **como o módulo RmR se relaciona com o AndroidX original**. O objetivo é evitar plágio, tornar a autoria explícita e manter as obrigações legais claras.

---

## 1. Escopo

Este documento se aplica ao diretório `rmr/` e seus submódulos (`rmr-core`, `rmr-lifecycle`, `rmr-navigation`, `rmr-preference`, `rmr-room` e `rafaelia`).

---

## 2. AndroidX Original (Projeto Upstream)

- **Projeto:** AndroidX (Google e contribuidores)
- **Licença:** Apache License 2.0
- **Onde a licença está neste repositório:** [`LICENSE.txt`](../LICENSE.txt)

**Observação importante:** o AndroidX é um projeto upstream separado. O módulo RmR **não** é uma cópia do código‑fonte do AndroidX. É uma **nova implementação e conjunto de documentos** que utiliza conceitos e ideias públicas. Se algum arquivo contiver código derivado do AndroidX, ele **deve** ser explicitamente listado na seção “Conteúdo Derivado ou de Terceiros” abaixo.

---

## 3. Autoria do RmR (Este Módulo)

- **Autor:** Rafael Melo Reis
- **Copyright:** © 2026 Rafael Melo Reis
- **Licença:** Apache License 2.0
- **Arquivo de licença do módulo:** [`rmr/LICENSE.md`](LICENSE.md)

### Escopo de autoria
Todos os arquivos em `rmr/` são autoria de Rafael Melo Reis **salvo indicação explícita** em notas por arquivo ou na seção abaixo.

---

## 4. Conteúdo Derivado ou de Terceiros (Listagem Obrigatória)

Se algum arquivo do RmR incluir código externo ou upstream, **liste aqui** com atribuição completa:

| Arquivo(s) | Fonte | Licença | Observações |
| --- | --- | --- | --- |
| _Nenhum no momento_ | _N/A_ | _N/A_ | Até esta revisão, nenhum código do AndroidX ou de terceiros foi copiado para `rmr/`. |

---

## 5. Separação das Contribuições

Para manter a autoria clara:

- O conteúdo do **AndroidX original** permanece no projeto upstream. Este repositório o referencia apenas nos termos de licença aplicáveis ao AndroidX como um todo.  
- O conteúdo **RmR** é autoral e mantido separadamente em `rmr/`, com licença e documentação próprias.

Se um arquivo incorporar ou adaptar código upstream, ele **deve**:

1. Conter cabeçalho com a referência da fonte original,
2. Registrar a licença original,
3. Declarar as mudanças feitas, e
4. Ser listado na Seção 4 acima.

---

## 6. Notas Legais e de Conformidade (Informativo)

Este documento é informativo e **não** constitui aconselhamento jurídico. Conceitos comuns de direito de software incluem:

- **Direitos autorais** (proteção da expressão criativa)
- **Concessões de licença** (permissões e condições)
- **Atribuição** (créditos e obrigações de NOTICE)

Consulte o texto da Apache License 2.0 para as condições oficiais. Em caso de dúvida, procure assessoria jurídica qualificada.

---

## 7. Controle de Mudanças

Qualquer alteração de autoria, licenciamento ou conteúdo externo deve atualizar este documento e a tabela da Seção 4.
