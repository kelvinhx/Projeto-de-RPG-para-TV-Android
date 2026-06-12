# NOTAS DE ATUALIZAÇÃO - WHATISRPG? VOICE ENGINE

Este arquivo de controle registra as modificações de build e novas implementações entregues à plataforma.

## [Build v3] - Versão do Aplicativo: 3.0
**Data e Hora do Envio:** 12 de Junho de 2026, às 16:30:00 BRT / 19:30:00 UTC

### O que há de novo nesta Build:

1. **Central de Atualizações de Jogo Conectada ao GitHub:**
   - Construído o `GitHubUpdateService.kt` que interage diretamente com o repositório público do GitHub especificado nas configurações para baixar e parsear de forma reativa o arquivo `NOTAS_DE_ATUALIZACAO.md`.
   - Implementado analisador léxico de Markdown que extrai de forma automática o nome da build, número de versão correspondente, data de submissão e lista detalhada de modificações.

2. **Console de Configurações Dinâmicas do Repositório para Android TV:**
   - Integrado um painel elegante de "Configurações do Jogo" acessado diretamente com a seleção D-Pad através da ativação do foco e clique no título principal na barra lateral.
   - Fornecidos campos interativos focáveis para que proprietários e desenvolvedores possam alterar em tempo real o Proprietário (*GitHub Owner*) e o nome do Repositório (*GitHub Repository*) salvando instantaneamente no armazenamento privativo de `SharedPreferences`.

3. **Status de Atualização em Tempo Real e Feedback Dinâmico:**
   - Adicionado indicador de status interativo comparando a versão corrente do aplicativo (`v3.0 - Build 3`) com a última versão disponibilizada remotamente no GitHub.
   - Renderização especializada de estados: Animação de Carregamento (*CircularProgressIndicator*), badges de conformidade estática ("estável / atualizado" ou "atualização disponível" com tons vibrantes de Material 3) e tratativa inteligente de erros com orientações corretivas em caso de conexão instável ou digitação inválida.

4. **Notificação de Versão Não-Invasiva de Primeiro Acesso:**
   - Integrado o banner reativo flutuante `VersionNotificationPrompt` na inicialização do aplicativo assim que o jogador instala uma nova distribuição de builds.
   - O aviso é totalmente não invasivo e possui dismiss amigável pelo D-Pad que grava o ID de build no `SharedPreferences` para garantir que o prompt nunca trespasse em sessões subsequentes de jogabilidade.

---

## [Build v2] - Versão do Aplicativo: 2.0
**Data e Hora do Envio:** 12 de Junho de 2026, às 16:25:00 BRT / 19:25:00 UTC

### O que há de novo nesta Build:

1. **Detecção de Voz Dinâmica e Totalmente Mãos-Livres (VAD):**
   - Integrado o cálculo em tempo real de Root Mean Square (RMS) e limiares inteligentes ajustados no `AudioRecorder.kt`.
   - Adicionado contador de silêncio contínuo (~1.5 segundos) que ativa automaticamente o término e transcrição silenciosa da voz do jogador.
   - Conectado o callback de silêncio no `GameViewModel.kt` para que o fluxo de tomada de decisões ocorra sem dependência de engatilhamento manual ou múltiplos cliques do controle.

2. **Interface Minimalista e Exclusiva para Televisão:**
   - Removidos inputs manuais por teclado/caixas de diálogo (`OutlinedTextField` e botões de Enviar manual) do `CuratedOptionsPanel` para garantir uma interface perfeitamente alinhada a TVs TCL de 32" e sistemas Android TV 11+ sem poluição visual.
   - Refinados os controles principais táticos sob seleção exclusiva de direcionais D-Pad do controle remoto ou ativação vocal passiva.

3. **Garantia de Estabilidade com Ícones Nativos do Core M3:**
   - Substituídos todos os pacotes instáveis e dependências dependentes de `material-icons-extended` por ícones equivalentes otimizados pertencentes ao motor nativo estrito (`Icons.Default.Warning`, `Icons.Default.Check`, `Icons.Default.PlayArrow`, `Icons.Default.Star`, `Icons.Default.Info`, `Icons.Default.Refresh`, `Icons.Default.Close`). Isso impede conflito de links e garante compilação com zero erros de referências.

---

## [Build v1] - Versão do Aplicativo: 1.0
**Data e Hora do Envio:** 12 de Junho de 2026, às 16:05:00 BRT / 19:05:00 UTC (Local: 2026-06-12T11:58:29-07:00)

### O que há de novo nesta Build:

1. **Estrutura Base para Android TV (TCL 32" / Android TV 11 - API 30):**
   - Configurada compatibilidade perfeita para Leanback e layouts de TV com `<category android:name="android.intent.category.LEANBACK_LAUNCHER" />` no `AndroidManifest.xml`.
   - Adicionadas as diretivas de hardware `android.software.leanback`, `android.hardware.microphone` e `android.hardware.touchscreen` (com `required="false"`) para assegurar suporte absoluto na console de TV.
   - Declaradas permissões de Gravação de Áudio (`RECORD_AUDIO`) e Internet (`INTERNET`).

2. **Navegação Exclusiva Otimizada para D-Pad (Controle Remoto):**
   - Implementado o componente customizável `TVButton` em Jetpack Compose. Ele reage ao foco do controle remoto escalando suavemente de tamanho (1.05x) com efeitos de animação harmônica (*Spring*) e bordas brilhantes (*Glowing Border*), tornando a interface totalmente acessível sem toques.

3. **Arquitetura RPG WhatIsRPG? Engine Integrada com Gemini AI:**
   - **Módulo 1 (Core Loop):** Sistema de loop contínuo de simulação lógica do estado construído em arquitetura reativa (`GameState`, `LogEntry` e `GameViewModel`).
   - **Módulo 2 (Combate e Grimório):** Gestão tática de HP/MP, atributos clássicos (FOR, AGI, INT, VIT, PER, WIL), árvore de grimórios aprendidos persistentes, e geração procedural de magias baseada em subclasses que evitam feitiços genéricos de RPGs vulgares.
   - **Módulo 3 (Mundo Vivo e NPCs Inteligentes):** Estrutura dinâmica de afeição (-100 a +100), reações emocionais dos NPCs (Medo, Raiva, Confiança, Ambição) com memórias persistentes guardadas em formato histórico.
   - **Módulo 4 (Evolução e Economia):** Sistema completo de níveis, experiência linear, inventário estendido, e moedas de ouro com sincronização de conquistas de cicatrizes raras e títulos honorários do jogador.
   - **Módulo 5 (Criação de Personagem Sequencial):** Inicialização guiada através do comando de voz "Vamos começar a jornada" progredindo rigorosamente em ordem (Nome, Aparência física, Gênero, Sexualidade, Raça, Classe e Subclasse) antes do início do jogo.

4. **Persistência Offline Total via Room Database:**
   - Criação do banco de dados relacional SQLite local por meio do Room (`AppDatabase`, `GameSaveDao` e `GameSave`). O estado total da realidade é auto-salvo como JSON compactado após cada ciclo de ação, permitindo carregar o progresso do jogador automaticamente entre sessões.

5. **Entrada de Áudio Real (Microfone) e Transcrição com `gemini-3.5-flash`:**
   - Implementado gravador de áudio (`AudioRecorder`) baseado no `AudioRecord` nativo do Android focado em 16kHz Mono (padrão otimizado de modulação vocal para inteligência artificial).
   - O áudio capturado é convertido sob cabeçalho WAV legítimo e orquestrado via REST API com o modelo `gemini-3.5-flash` que transcreve comandos de jogador com extrema fidelidade.

6. **Gerador de Narração Clássica Falada (TTS) com `gemini-3.1-flash-tts-preview`:**
   - Integração da IA de conversão textual para voz utilizando o modelo dinâmico oficial `gemini-3.1-flash-tts-preview` gerando vozes teatrais para o Narrador e NPCs.
   - Foi construída uma camada de salvaguarda (*Graceful Fallback*) para o motor vocal local de Android (`TextToSpeech`) caso a conectividade externa ou o limite da cota do usuário falhe, mantendo a narrativa ininterrupta.

---
*Fim das notas da Build v1. Compilado e assinado com sucesso.*
