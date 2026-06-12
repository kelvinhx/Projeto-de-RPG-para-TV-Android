# NOTAS DE ATUALIZAÇÃO - WHATISRPG? VOICE ENGINE

Este arquivo de controle registra as modificações de build e novas implementações entregues à plataforma.

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
