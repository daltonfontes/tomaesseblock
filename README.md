# Toma Esse Block 🛡️📵

App Android em **Kotlin** para identificar e bloquear chamadas indesejadas — inspirado no Whoscall.

## Funcionalidades

- **Bloqueio silencioso** (como no Whoscall): o telefone nem toca, nada aparece na tela e a chamada fica
  registrada como **bloqueada** no histórico de chamadas do próprio telefone (e no histórico do app).
  Usa a API oficial `CallScreeningService` (Android 10+).
- **Lista de bloqueio** por número exato ou **por prefixo** (ex.: `0303`, um DDD inteiro, `1140…`).
- **Telemarketing 0303** bloqueado por padrão (prefixo obrigatório da Anatel).
- **Denúncias de spam** por categoria (telemarketing, golpe, cobrança, robô, pesquisa…), com limite configurável de denúncias para bloquear.
- Opcional, em Ajustes (desligado por padrão): notificação a cada bloqueio e alerta "⚠ Possível spam" para
  chamadas suspeitas que não foram bloqueadas.
- **Buscar número**: consulte qualquer número para ver denúncias e o que o app faria com ele.
- **Histórico** de chamadas bloqueadas/identificadas e estatísticas na tela inicial.
- Opções de **bloquear números ocultos** e **modo rigoroso** (só contatos podem ligar).
- Contatos da agenda nunca são bloqueados (a não ser que você os coloque na lista de bloqueio).

## Como funciona

```
Chamada recebida
   └─ CallBlockerService.onScreenCall()
        ├─ normaliza o número (PhoneNumbers)       +55 (11) 9…  →  119…
        ├─ consulta contatos, regras e denúncias   (Room + DataStore)
        ├─ CallDecisionEngine.decide()             → Block / Allow(identificação)
        ├─ responde ao sistema (rejeita ou deixa tocar)
        └─ grava no histórico e mostra notificação
```

Ordem de decisão: oculto → lista de bloqueio (exato, prefixo) → contatos → 0303 → denúncias → modo rigoroso.

## Estrutura

```
app/src/main/java/com/tomaesseblock/
├── domain/   PhoneNumbers, CallDecisionEngine, modelos (lógica pura, testada)
├── data/     Room (regras, denúncias, histórico), DataStore (ajustes), contatos
├── service/  CallBlockerService, notificações e ações de notificação
└── ui/       Jetpack Compose + Material 3 (Início, Bloqueios, Histórico, Buscar, Ajustes)
```

## Compilar e rodar

Requisitos: Android Studio (Ladybug ou mais novo) / JDK 17 / Android SDK 35.

```bash
./gradlew testDebugUnitTest   # testes unitários
./gradlew assembleDebug       # gera app/build/outputs/apk/debug/app-debug.apk
```

O GitHub Actions (`.github/workflows/android.yml`) roda os testes e publica o APK de debug como artefato a cada push.

No celular: abra o app → **Ativar bloqueio de chamadas** → escolha *Toma Esse Block* como app de identificação de chamadas e spam → permita contatos e notificações.

> Observação: quando o app não é o discador padrão, o Android só consulta o serviço para números **fora** da agenda — o que é justamente o caso de spam.

## Próximos passos

- Base **comunitária** de denúncias em um servidor (hoje as denúncias ficam no aparelho).
- Importar/exportar lista de bloqueio e bloqueio de SMS.
