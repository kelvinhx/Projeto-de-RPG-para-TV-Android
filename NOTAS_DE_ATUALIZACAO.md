# NOTAS DE ATUALIZAÇÃO - WHATISRPG? VOICE ENGINE

Este arquivo de controle registra as modificações de build e novas implementações entregues à plataforma.

## [Build v3.8] - Versão do Aplicativo: 3.8
**Data e Hora do Envio:** 12 de Junho de 2026, às 17:15:00 BRT / 20:15:00 UTC

### O que há de novo nesta Build:

1. **Fluidez Dinâmica com Animações Inteligentes (Smooth Collapsible Flow):**
   - Integrado o modificador `.animateContentSize()` do Jetpack Compose sob todos os painéis e sanfonas de dados retráteis (Painel do Herói, Atributos de Alma, Grimório Secular, Sinfonia do Mundo e Sobreviventes).
   - Agora, a expansão e o recolhimento das seções laterais ocorrem por meio de uma transição deslizante ultra-suave, extinguindo transições abruptas e proporcionando um visual premium em telas grandes e TVs de 32" a 75".

2. **Reforço de Segurança e Sincronia de Modelos em Tempo Real:**
   - Realizada auditoria profunda nos mapeamentos estruturais do banco de dados Room local para assegurar compatibilidade absoluta de dados ao ler e serializar itens do inventário, magias do grimório e logs narrativos, eliminando travamentos de banco durante grandes saltos de turno.
   - Otimizada a inicialização resiliente do Text-to-Speech (TTS) nativo e sintonizador de áudio, provendo respostas mais velozes e fluidas guiadas pelos comandos direcionais do controle e por cliques nos balões narrativos.

3. **Sincronia Global de Versão (GitHub Pipeline Alignment):**
   - Todas as instâncias internas de versão no Activity principal, rodapé de sistema, prompts de popups e constantes de lógica do ViewModel foram consolidadas rigorosamente sob a nova release 3.8.

---

## [Build v3.7] - Versão do Aplicativo: 3.7
**Data e Hora do Envio:** 12 de Junho de 2026, às 14:35:00 BRT / 17:35:00 UTC

### O que há de novo nesta Build:

1. **Acordeões Retráteis Otimizados para TV e D-Pad D-Pad:**
   - Adicionada estrutura de visualização compacta/expandida para todos os painéis laterais (Status do Herói, Atributos de Alma, Grimório Secular, Sinfonia do Mundo, e Sobreviventes).
   - Ocultamento inteligente de listas extensas de submenus por padrão para que o visor da TV de 32" mantenha o máximo de espaço livre para a leitura da narrativa do jogo.

2. **Interatividade Fluida e Direta (D-Pad Action Flow):**
   - Integrada a capacidade de utilizar itens e lançar feitiços diretamente clicando nos submenus correspondentes. Clicar em um item da mochila ativa o comando `usar o item <nome>`, enquanto clicar em um feitiço ativo no Grimório engatilha a conjuração instantânea `Conjurar a magia <nome>`.
   - Adicionados controles focáveis `[+]` individuais ao lado de cada atributo (FOR, AGI, INT, VIT, PER, WIL) sempre que houver pontos não atribuídos disponíveis para distribuição.

3. **Feedback Falado Integrado por Comando de Foco:**
   - Cada expansão, redução de painéis de status ou acúmulo de pontos livre dispara um feedback falado dinâmico do Narrador e leitor vocal, eliminando totalmente a barreira de digitação textual do console.

---

## [Build v3.6] - Versão do Aplicativo: 3.6
**Data e Hora do Envio:** 12 de Junho de 2026, às 14:15:00 BRT / 17:15:00 UTC

### O que há de novo nesta Build:

1. **Reformulação Minimalista da Interface de TV (Android TV/Leanback):**
   - Transição para um layout de TV de alta gama e esteticamente polido com painel lateral translúcido reduzido a 32% da largura.
   - Design focado em grandes contrastes tipográficos, preenchimentos generosos de inlays e bordas douradas/violetas suaves brilhando em estados selecionados por controle.

2. **Sistema Totalmente Falado por Voz ("Narrador Integral"):**
   - Integrado reator de inicialização no motor TTS onde, ao carregar a sessão iniciada, o Narrador automaticamente relembra ou introduz o jogador lendo o último evento relevante do histórico.
   - Ativado suporte de clique nos balões de história do console e cartões de status: o jogador pode utilizar o direcional D-Pad e pressionar OK em qualquer balão de log, NPC, ou atributo para que a Inteligência Artificial narre integralmente o conteúdo em voz ativa em tempo real.

3. **Equalizador de Voz Conectado com Halo Pulsante:**
   - Adicionada animação visual reativa de sístole/diástole baseada no estado de gravação, onde halos concêntricos vermelhos expandem-se de maneira contínua simulando sensibilidade de áudio sob a TV enquanto o controle escuta o jogador.

---

## [Build v3.5] - Versão do Aplicativo: 3.5
**Data e Hora do Envio:** 12 de Junho de 2026, às 13:39:00 BRT / 16:39:00 UTC

### O que há de novo nesta Build:

1. **Automação de Compilação do APK via GitHub Actions:**
   - Adicionado o arquivo `.github/workflows/android.yml` contendo a pipeline de CI de construção profissional do pacote Android.
   - Configurado o gatilho reativo automático para construir o APK no JDK 17 e enviá-lo como um artefato para download seguro sempre que um commit ou pull-request for enviado às ramificações `main` ou `master`.

2. **Decodificação de Assinatura Automática:**
   - Integrado script Shell inteligente de decodificação na pipeline do GitHub Actions para reconstruir o `debug.keystore` a partir do backup `debug.keystore.base64` do repositório, mantendo integridade e consistência estrita de chaves de assinatura em ambientes distribuídos.

---

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
