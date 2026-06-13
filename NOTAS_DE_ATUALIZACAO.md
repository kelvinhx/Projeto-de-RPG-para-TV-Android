# NOTAS DE ATUALIZAÇÃO - WHATISRPG? VOICE ENGINE

Este arquivo de controle registra as modificações de build e novas implementações entregues à plataforma.

## [Build v4.5] - Versão do Aplicativo: 4.5
**Data e Hora do Envio:** 13 de Junho de 2026, às 00:15:00 UTC / 21:15:00 BRT (anterior)

### O que há de novo nesta Build:

1. **Modernização Absoluta do Layout para Android TV (Flat & Expanded Views):**
   - **Remoção de Acordeões Duplos e Colapsáveis:** Eliminada a necessidade de cliques duplos invasivos ou complexidades por D-pad para expandir as abas secundárias. Agora, Atributos de Alma (`PlayerAttributesView`), Grimório de Feitiços (`PlayerGrimoireView`), Sinfonia do Mundo/Ciclos (`WorldStatusView`) e Lista de NPCs Ativos (`ActiveNpcListView`) utilizam layouts planos, totalmente expandidos e prontos para focar e interagir instantaneamente.
   - **Visual Premium e Clean:** Alinhados os tons translúcidos violetas/grafite com bordas de ouro e luz neon roxa de alta qualidade para acompanhar a identidade visual estabelecida na build anterior.

2. **Equalizador Holográfico de Voz Dinâmico e Animado por Canvas ("Equalizer Waves in Console"):**
   - O console de comandos de áudio (`VoiceRecorderConsole`) recebeu um visualizador dinâmico reativo aos diferentes estados de som do motor do jogo, eliminando telas estáticas e fornecendo telemetria animada em tempo real:
     - **Gravando (Recording):** Spikes dinâmicos e picos em rubi neon simulando captação física de áudio por onda analógica tradicional.
     - **IA Traduzindo (Transcribing):** Varredura horizontal por laser dourado neon demonstrando processamento inteligente das intenções fonéticas.
     - **Dungeon Master Simulando (ProcessingTurn):** Ondas violetas pulsantes estelares em formato de pulso cósmico senoidal.
     - **Voz Ativa de Narrador (SpeakingNarrator):** Ripples e pontos verdes esmeralda em fluxo reativo ao ritmo da leitura/TTS.

3. **Guia e Calibração dos Focos de Controle:**
   - Garantido que todas as listas de magias e NPCs informem seus estados por Text-To-Speech nativo no momento imediato de receber foco direcionado no controle remoto.

---

## [Build v4.4] - Versão do Aplicativo: 4.4
**Data e Hora do Envio:** 12 de Junho de 2026, às 19:02:00 BRT / 22:02:00 UTC

### O que há de novo nesta Build:

1. **Adequação e Refatoração Absoluta para Controle Remoto D-Pad (TV-Centric Navigation):**
   - Eliminadas barreiras baseadas em toques/gestos nas telas de jogo, garantindo que 100% das interações ativas possuam seletores visuais de foco adaptados ao controle físico da TV.
   
2. **Sistema de Foco Saudável e Profilaxia contra Focus-Traps ("Tomando cuidado pra não conceder foco em áreas desnecessárias"):**
   - **Histórico Fluido (`NarrativeBox`):** Removida a focusabilidade independente de todas as mensagens antigas do chat, resolvendo o problema no qual o usuário precisava realizar dezenas de cliques com a seta para passar pelos diálogos passados. Agora, apenas o balão mais moderno de fala é focado (facilitando reinterpretar e escutar com o botão central OK), viabilizando saltos imediatos.
   - **Distribuição de Atributos Seculares (`AttributeRow`):** Removidos focos ociosos sobre o rótulo descritivo estático dos Atributos d'Alma, canalizando as paradas do D-pad exclusivamente no botão de ganho numérico `[+]` quando pontos extras estiverem válidos no deck.

3. **Guia Detalhado e Painel Explicativo On-Screen (`TvNavigationGuidePanel`):**
   - Desenvolvido um painel informativo contextual fixado na base do menu de status lateral esquerdo (Sidebar).
   - O guia ilustra de forma clara e legível as ações de D-pad: **Direcionais (▲/▼/◄/►)** para mover o destaque brilhante, **OK / Confirmar** como gatilho centralizador das escolhas, **🎙️ FALAR (OK)** como acionador vocal no console e **⚙️ CONFIG** para o título WhatIsRPG? de modo a alterar repositórios em tempo real.

4. **Diretrizes Rápidas do Mestre Facilitadas:**
   - O cabeçalho das opções rápidas no rodapé central do Layout do Painel foi atualizado para `"OPÇÕES RÁPIDAS (DIRECIONAIS ◄ / ► DO CONTROLE • OK SELECIONA):"`, instruindo visualmente o usuário sobre como se deslocar horizontalmente de forma simplificada e direta.

---

## [Build v4.3] - Versão do Aplicativo: 4.3
**Data e Hora do Envio:** 12 de Junho de 2026, às 18:25:00 BRT / 21:25:00 UTC

### O que há de novo nesta Build:

1. **Aprimoramento Absoluto do Sistema de Atualizações em Tempo Real (Hot-Update Center):**
   - Configurada uma rotina ultra-robusta de atualização automática em segundo plano a cada ciclo de inicialização (com atraso profilático de 3 segundos para estabilidade de conexão de rede).
   - Desenvolvido o painel dialogador inteligente `UpdateOfferDialog` que alerta o usuário de modo não invasivo sobre builds mais recentes com sumário e mudanças em lista interativa retiradas do GitHub, disponibilizando as opções e focos nativos para atualizar imediatamente ("Atualizar Agora") ou adiar ("Mais Tarde").
   - Integrado um visualizador de progresso dinâmico de instalação reativa com barras lineares de carregamento, percentual completo de transferência de dados e status textual descritivo detalhando as etapas internas (conectar, descompactar e persistir no banco local).
   - Acoplado controle de override persistente via armazenamento de dispositivo `SharedPreferences` para a versão atual conectada, atualizando em cascata todas as indicações estáticas da aplicação (título do jogo, rodapés informativos e os balões de logs) dinamicamente.

---

## [Build v4.2] - Versão do Aplicativo: 4.2
**Data e Hora do Envio:** 12 de Junho de 2026, às 18:10:00 BRT / 21:10:00 UTC

### O que há de novo nesta Build:

1. **Amplificação Digital de Áudio (Ganho de 3.5x) para Captação Total do Microfone:**
   - Corrigido o problema em que o som vindo do microfone do controle remoto não era identificado satisfatoriamente devido aos baixos níveis de captação padrão de hardware Android TV.
   - Implementado um pré-processador de ganho digital por software em `AudioRecorder.kt` que amplifica o stream de bytes PCM brutos no buffer de gravação em **3.5x (350% de ganho)** antes que a potência RMS seja medida e o arquivo `.wav` seja compactado e enviado para processamento.
   - Calibrados os limiares de detecção dinâmica de volume por voz (Voice Activity Detection - VAD) no ambiente residencial típico, reduzindo o limiar de ativação de fala (`voiceRmsThreshold`) de `600.0` para `200.0` e o limiar de silêncio (`silenceRmsThreshold`) para `90.0`. Isto garante que o sistema capte "total totalmente" sussurros ou vozes baixas vindas do dispositivo.

2. **Aprimoramento de Controle de Voz Estilo Google NLP (Fuzzy Intent Mapping):**
   - Introduzida uma camada de processamento de linguagem natural heurística baseada nos padrões do Google Assistente diretamente no `GameViewModel`.
   - Conversões fonéticas e textuais tratam as variações típicas de fala em português (remoção de pontuações, normalização para caixa baixa, tratamento de plurais truncados) e fazem um mapeamento semântico dinâmico para garantir o sucesso das criações do personagem e as ações gerais de RPG mesmo diante de leves furos na transcrição gerada pelo ar condicionado ou ruído de TV.

3. **Opções Interativas Predefinidas em 100% dos Setores do Jogo:**
   - Atendida a diretriz de exibição ininterrupta de sugestões estruturadas na tela para que o jogo sempre ofereça saídas por D-pad, em adição ao controle vocal direto.
   - Mesmo nas telas de digitação manual de nome e descrição física do herói (etapas `NOME` e `APARENCIA` da criação de personagem), o console agora exibe seleções e sugestões elegantes prontas para serem selecionadas com o controle remoto (ajudando a prosseguir caso o usuário esteja em ambientes barulhentos).

4. **Notificação de Inicialização Altamente Amigável e Não Invasiva:**
   - Solucionado o travamento da notificação que persistia no topo da interface.
   - Toda a estrutura de `Card` do `VersionNotificationPrompt` na `MainActivity.kt` passou a ser detectável por cliques de forma elástica, permitindo que cliques ou toques em qualquer área da mensagem a descartem instintivamente.
   - Adicionada de forma profilática uma lógica de autodescarte por coroutine que apaga o balão de alerta de modo suave após **10 segundos** do início da aplicação, prevenindo de antemão que qualquer bloqueio visual ou focus-trap atrapalhe o fluxo de jogabilidade na Android TV.

---

## [Build v4.1] - Versão do Aplicativo: 4.1
**Data e Hora do Envio:** 12 de Junho de 2026, às 17:50:00 BRT / 20:50:00 UTC

### O que há de novo nesta Build:

1. **Correção do Erro Crítico Kotlin/KSP no GitHub Actions (IntelliJ OpenAPI ClassNotFound/NPE Fix):**
   - Corrigido o erro fatal durante a compilação do KSP no GitHub: `NullPointerException: Cannot invoke ksp.com.intellij.openapi.application.Application.getService() because getApplication() is null`.
   - Identificado que o parâmetro `kotlin.compiler.execution.strategy=in-process` em `gradle.properties` forçava a execução do compilador Kotlin diretamente sob o processo principal do Gradle. Isso prevenia que o KSP 2.x inicializasse a infraestrutura do compilador IntelliJ corretamente devido ao isolamento do ClassLoader do Gradle.
   - Modificado o arquivo `gradle.properties` para comentar a estratégia `in-process`, restabelecendo a execução nativa baseada em `daemon` (out-of-process). Isso isola perfeitamente o compilador Kotlin com KSP, solucionando o crash de compilação em builds limpas (clean builds) remotas.

2. **Incremento Estável para a Versão 4.1:**
   - Atualizados todos os cabeçalhos de sistema, rodapés no console da Android TV, strings de notificação e constantes internas do `GameViewModel` para a nova release v4.1, garantindo consistência total na telemetria do aplicativo.

---

## [Build v4.0] - Versão do Aplicativo: 4.0
**Data e Hora do Envio:** 12 de Junho de 2026, às 17:40:00 BRT / 20:40:00 UTC

### O que há de novo nesta Build:

1. **Correção Crítica no Pipeline de Compilação do GitHub Actions (CI/CD Build Fix):**
   - Corrigido o erro fatal da tarefa `:app:validateSigningDebug` no pipeline do GitHub, em que o arquivo `debug.keystore` (não versionado por segurança) causava interrupção prematura do build remoto do Android.
   - Refatorado o arquivo `app/build.gradle.kts` para introduzir o carregamento condicional e resiliente do chaveiro de depuração. Agora, a assinatura customizada `debugConfig` é aplicada dinamicamente apenas se o arquivo `debug.keystore` existir fisicamente na raiz do projeto (como ocorre no ambiente emulado da nuvem na AI Studio). Caso o arquivo esteja ausente (como em construções limpas no GitHub runner), o Android Gradle Plugin (AGP) é instruído a utilizar a assinatura de depuração padrão nativa automaticamente, garantindo compatibilidade universal de CI/CD.

2. **Mitigação de Regressão de Versão no Serviço de Atualização:**
   - Incrementado o motor estratégico de versionamento para a versão majoritária v4.0. Ao utilizar a convenção numérica `4.0` em vez de `3.10`, evitou-se o truncamento de conversão em ponto flutuante de strings de versão do serviço (onde `3.10` seria avaliado na lógica interna do Kotlin como `3.1`, gerando uma falsa regressão frente à versão `3.9`).
   - Sincronização completa de strings de interface no painel de TV, rodapés informativos e logs de inicialização.

---

## [Build v3.9] - Versão do Aplicativo: 3.9
**Data e Hora do Envio:** 12 de Junho de 2026, às 17:30:00 BRT / 20:30:00 UTC

### O que há de novo nesta Build:

1. **Sincronia Total e Padrão Nativo com o Repositório GitHub (Fork Alignment):**
   - Configurados e alinhados todos os parâmetros de repositório padrão nas classes internas (`GitHubUpdateService`, `GameViewModel` e tela de UI de Configurações no `MainActivity`) para mapear nativamente o novo repositório fornecido pelo usuário: `kelvinhx/Projeto-de-RPG-para-TV-Android`.
   - Modificado o botão de restauração de fábrica ("Restaurar Padrão") nas opções da Android TV para recuperar esta nova base de repositório agora integrada.

2. **Otimização Geral e Resiliência Estrutural (Macro Resilience):**
   - Refinamento do comportamento do banco de dados Room (local storage) e salvamento de estado do RPG a cada transição de turno para garantir conformidade total, evitando quaisquer falhas estruturais ou travamentos em mutações rápidas de dados.
   - Reforço no mapeamento cruzado de estados anteriores de jogo para evitar conflitos de cache após atualizações do arquivo local do banco ou ao processar logs de narrativas históricas.

3. **Fluidez de Transição Visual Avançada:**
   - Pequenas correções de layout e alinhamento de texto de rodapés mantendo estabilidade visual em vários tamanhos de TV (32", 55", 75").
   - Consolidação globalizada de strings e indicações da versão active atualizada (v3.9) em todos os painéis e widgets nativos.

---

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
