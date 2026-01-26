# Rafaelia Core BitStack

## Objetivo
O módulo `rafaelia-core` fornece um BitStack append-only para registrar eventos em memória com opção de persistência em arquivo. Ele é desenhado para ser simples, previsível e compatível com o monorepo AndroidX, mantendo checksum CRC32C e ganchos (stubs) para paridade.

## API
Principais pontos:
- `record(event: ByteArray)`: registra um evento no BitStack.
- `snapshot()`: captura um snapshot imutável do estado atual.
- `verifySnapshot(snapshot)`: valida checksums e aciona verificação de paridade.

### Exemplo
```kotlin
val stack = BitStack()
stack.record("evento".toByteArray())
val snapshot = stack.snapshot()
val report = stack.verifySnapshot(snapshot)
```

### Storage opcional
Para persistir em arquivo:
```kotlin
val stack = BitStack(
    storage = BitStackStorage.FileBacked(File(filesDir, "bitstack.bin"))
)
```

## Limites e considerações
- O BitStack é append-only: não há remoção ou compactação.
- O buffer cresce quando necessário; use `initialCapacityBytes` para reduzir realocações.
- O hook de paridade é um stub: a implementação real fica a cargo do integrador.
- Snapshots duplicam o buffer em memória para manter imutabilidade.
