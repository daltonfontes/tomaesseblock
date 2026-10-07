# Política de Segurança

## Versões suportadas

O Calloff ainda está em desenvolvimento inicial. Apenas a versão mais recente da branch `main`
recebe correções de segurança.

| Versão            | Suportada |
| ----------------- | --------- |
| `main` (0.1.x)    | ✅        |
| Versões anteriores | ❌        |

## Como reportar uma vulnerabilidade

**Não abra uma issue pública** para relatar problemas de segurança.

Use o recurso de reporte privado do GitHub:

1. Acesse a aba **Security** do repositório.
2. Clique em **Report a vulnerability**.
3. Descreva o problema com o máximo de detalhes possível:
   - o que acontece e qual o impacto (ex.: vazamento de dados, bloqueio indevido de chamadas);
   - passos para reproduzir;
   - versão do app, modelo do aparelho e versão do Android;
   - prova de conceito, logs ou capturas de tela, se houver.

O relato fica visível apenas para os mantenedores até que a correção seja publicada.

### O que esperar

- **Confirmação de recebimento:** em até 7 dias.
- **Avaliação inicial** (se é uma vulnerabilidade e qual a gravidade): em até 14 dias.
- **Correção:** o prazo depende da gravidade; problemas críticos têm prioridade.
- Você será informado do andamento e, se quiser, receberá crédito na nota da correção.

Pedimos que não divulgue publicamente o problema antes de a correção estar disponível.

## Escopo

O app lida com dados sensíveis do usuário, por isso estes pontos são especialmente relevantes:

- **Triagem de chamadas** (`CallBlockerService`): qualquer forma de outro app ou de um terceiro fazer
  chamadas legítimas serem bloqueadas, ou chamadas bloqueadas tocarem, sem a vontade do usuário.
- **Dados locais**: lista de bloqueio, denúncias de spam, histórico de chamadas e ajustes ficam
  apenas no aparelho (Room e DataStore). O backup do Android está desligado (`allowBackup="false"` e
  regras de extração que excluem tudo), então esses dados não vão para a nuvem nem para outro aparelho.
  Acesso a esses dados por outros apps é uma vulnerabilidade.
- **Contatos**: o app lê os contatos apenas para verificar se um número está na agenda e não os
  armazena nem os envia para lugar nenhum.
- **Componentes exportados**: o serviço de triagem só aceita conexões do sistema
  (`BIND_SCREENING_SERVICE`); o receptor das ações de notificação não é exportado.
- **Rede**: o app não faz requisições de rede. Qualquer envio de dados para fora do aparelho é um
  problema de segurança.

### Fora do escopo

- Problemas que exigem aparelho com root ou acesso físico ao aparelho desbloqueado.
- Limitações do próprio Android (ex.: o sistema só consultar o app para números fora da agenda
  quando ele não é o discador padrão).
- Números de spam que não foram bloqueados: isso é falha de detecção, não de segurança — abra uma
  issue normal.
