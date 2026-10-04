# Toma Esse Block 🛡️📵

App Android em **Kotlin** para identificar e bloquear chamadas indesejadas — inspirado no Whoscall.

## Baixar para testar

**[⬇️ Baixar o APK (versão mais recente)](https://github.com/daltonfontes/tomaesseblock/releases/latest/download/tomaesseblock.apk)** · [todas as versões](https://github.com/daltonfontes/tomaesseblock/releases)

Requer Android 10 ou mais novo. Para instalar:

1. Abra o link acima **no celular** e baixe o `tomaesseblock.apk`.
2. Toque no arquivo baixado. Na primeira vez, o Android pede para **permitir instalar apps desta fonte**
   (o navegador ou o gerenciador de arquivos) — permita e volte.
3. Abra o app → **Ativar bloqueio de chamadas** → escolha *Toma Esse Block* como app de identificação de chamadas e spam.

Versões novas instalam por cima da anterior, sem perder listas nem histórico. Encontrou um problema?
[Abra uma issue](https://github.com/daltonfontes/tomaesseblock/issues) contando o modelo do celular, a versão do
Android e a versão do app (*Ajustes → Sobre*).

## Funcionalidades

- **Bloqueio silencioso** (como no Whoscall): o telefone nem toca, nada aparece na tela e a chamada fica
  registrada como **bloqueada** no histórico de chamadas do próprio telefone (e no histórico do app).
  Usa a API oficial `CallScreeningService` (Android 10+).
- **Lista de bloqueio** por número exato ou **por prefixo** (ex.: `0303`, um DDD inteiro, `1140…`).
- **Telemarketing 0303** bloqueado por padrão (prefixo obrigatório da Anatel).
- **Denúncias de spam** por categoria (telemarketing, golpe, cobrança, robô, pesquisa…), com limite configurável de denúncias
  e escolha de **quais categorias bloqueiam** (as demais só identificam).
- **Lista de permitidos** ("sempre permitir"), por número ou prefixo: sempre toca, por cima de qualquer bloqueio.
- **Bloquear ligações internacionais** (+1, +44, +62…), opcional.
- **Deixar tocar se ligar de novo**: quem foi barrado por um bloqueio amplo (fora dos contatos, internacional ou
  prefixo) e liga de novo em até 5 minutos consegue chamar. Bloqueios escolhidos, 0303 e spam continuam valendo.
- **Backup manual**: exportar/importar listas e denúncias num arquivo JSON (o backup automático do Android fica desligado).
- Opcional, em Ajustes (desligado por padrão): notificação a cada bloqueio e alerta "⚠ Possível spam" para
  chamadas suspeitas que não foram bloqueadas.
- **Buscar número**: consulte qualquer número para ver denúncias e o que o app faria com ele.
- **Histórico** de chamadas bloqueadas/identificadas, com menu ⋮ para bloquear, desbloquear, sempre permitir ou
  denunciar, e estatísticas na tela inicial.
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

Ordem de decisão: oculto → permitidos → lista de bloqueio (exato, prefixo) → contatos → 0303 → denúncias →
internacionais → modo rigoroso; por fim, a exceção "ligou de novo" para os bloqueios amplos.

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
./gradlew recordPaparazziDebug # capturas de tela em app/src/test/snapshots/images/
```

O GitHub Actions (`.github/workflows/android.yml`) roda os testes e publica como artefatos, a cada push, o APK de debug,
o pacote `.aab` de release e as capturas de tela de todas as abas (tema claro e escuro, `tomaesseblock-screenshots`).

No celular: abra o app → **Ativar bloqueio de chamadas** → escolha *Toma Esse Block* como app de identificação de chamadas e spam → permita contatos e notificações.

> Observação: quando o app não é o discador padrão, o Android só consulta o serviço para números **fora** da agenda — o que é justamente o caso de spam.

## Versões

- **Versão a lançar:** `appVersion` no `gradle.properties` (hoje `1.0.0`).
- **`versionCode`:** calculado da versão, `major × 10000 + minor × 100 + patch` (1.0.0 → 10000, 1.2.3 → 10203).
  Sempre cresce junto com a versão, como o Play exige. `minor` e `patch` vão de 0 a 99.
- **Builds de cada push (CI) e do Android Studio:** `versionName` = `1.0.0-dev+<execução>`, para não serem confundidos
  com um lançamento. Não envie esses para o Play.
- A versão instalada aparece em **Ajustes → Sobre**.

## Publicar no Google Play

### Uma vez só: chave de upload
1. Crie a chave (guarde o arquivo e as senhas em local seguro; perder a chave complica as atualizações):
   ```bash
   keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Em *Settings → Secrets and variables → Actions* do repositório, crie os secrets:
   - `RELEASE_KEYSTORE_BASE64` — saída de `base64 -w0 upload.jks`
   - `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` (`upload`) e `RELEASE_KEY_PASSWORD`
3. No Play Console, ative o *Play App Signing*.

### A cada lançamento
1. Atualize `appVersion` no `gradle.properties` (ex.: `1.0.1` para correções, `1.1.0` para novidades) e faça o merge na `main`.
2. Crie e envie a tag com a mesma versão:
   ```bash
   git checkout main && git pull
   git tag v1.0.0
   git push origin v1.0.0
   ```
   (ou pelo GitHub: *Releases → Draft a new release → Choose a tag → `v1.0.0`*).
3. O workflow **Release** (`.github/workflows/release.yml`) roda os testes, gera o `.aab` e o `.apk` assinados e cria o
   Release no GitHub com os dois anexados. Se a tag não bater com `appVersion`, ou se faltarem os secrets, ele falha
   avisando o motivo.
4. Envie o `tomaesseblock-v1.0.0.aab` no Play Console.

Também é preciso, no Play Console: link da [política de privacidade](https://daltonfontes.github.io/tomaesseblock/privacidade.html), formulário de
*Segurança dos dados* (contatos usados só no aparelho, nada coletado ou compartilhado), ícone
512×512, imagem de destaque 1024×500 e capturas de tela.

### Site e política de privacidade (GitHub Pages)

A pasta `docs/` tem o site do app e a política de privacidade, publicados em
<https://daltonfontes.github.io/tomaesseblock/privacidade.html>. Para ativar (uma vez só): *Settings → Pages → Build and deployment → Source:
Deploy from a branch → Branch: `main` / pasta `/docs`*.

## Próximos passos

- Base **comunitária** de denúncias em um servidor (hoje as denúncias ficam no aparelho).
