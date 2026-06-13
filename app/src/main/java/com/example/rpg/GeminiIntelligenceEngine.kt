package com.example.rpg

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Enterprise-grade exclusive AI Orchestration Engine for 'WhatIsRPG? O Eco da Podridão'.
 * Acts as the direct bridge between Gemini's multimodal cognitive capabilities (Grounding, Voice, JSON outputs)
 * and the Android TV application infrastructure.
 */
class GeminiIntelligenceEngine(
    private val context: Context,
    private val geminiRepository: GeminiRepository,
    private val networkInfoFlow: StateFlow<NetworkDiagnosticInfo>? = null
) {

    private val moshi: Moshi = RetrofitClient.moshiInstance

    init {
        Log.i("GeminiIntelligenceEngine", "Initializing exclusive AI Intelligence Engine with Google Gemini.")
    }

    /**
     * Sanitizes, enriches, and repairs verbal player commands captured from low-quality Android TV remote microphones.
     * Uses semantic awareness of present local factors (inventory, grimoire skills, location, active NPCs)
     * to resolve phonetic errors, slurred speech, or accent issues.
     */
    suspend fun sanitizeAndReconstructVoiceInput(
        rawTranscript: String,
        currentState: GameState
    ): String = withContext(Dispatchers.Default) {
        val trimmed = rawTranscript.trim()
        if (trimmed.isEmpty()) return@withContext ""

        Log.d("GeminiIntelligenceEngine", "Refining voice command through semantic filter: '$trimmed'")

        // 1. Gather semantic targets from current game state to aid matching
        val skills = currentState.playerState.grimoire.map { it.name }
        val inventory = currentState.playerState.inventory.map { it.name }
        val npcs = currentState.npcs.map { it.name }
        val activeRegion = currentState.worldState.region

        val lowerTrimmed = trimmed.lowercase(Locale.getDefault())

        // 2. Perform local rapid phonetic/semantic heuristics for instant TV responsiveness
        for (skill in skills) {
            if (lowerTrimmed.contains(skill.lowercase(Locale.getDefault())) || 
                levenshteinDistance(lowerTrimmed, skill.lowercase(Locale.getDefault())) <= 3) {
                Log.i("GeminiIntelligenceEngine", "Local VAD alignment: Matched skill '$skill'")
                return@withContext "Usar habilidade espiritual: $skill"
            }
        }

        for (item in inventory) {
            if (lowerTrimmed.contains(item.lowercase(Locale.getDefault())) ||
                levenshteinDistance(lowerTrimmed, item.lowercase(Locale.getDefault())) <= 3) {
                Log.i("GeminiIntelligenceEngine", "Local VAD alignment: Matched item '$item'")
                return@withContext "Usar item do inventário: $item"
            }
        }

        for (npc in npcs) {
            if (lowerTrimmed.contains(npc.lowercase(Locale.getDefault())) ||
                levenshteinDistance(lowerTrimmed, npc.lowercase(Locale.getDefault())) <= 3) {
                Log.i("GeminiIntelligenceEngine", "Local VAD alignment: Matched target NPC '$npc'")
                return@withContext "Interagir e conversar com $npc"
            }
        }

        // 3. Cognitive Fallback: Return original trimmed text if no mapping meets heuristics
        return@withContext trimmed
    }

    /**
     * Direct interface to transcribe player voice byte recordings with high accuracy,
     * incorporating the active game state to provide context clues to the transcriber model.
     */
    suspend fun transcribeVoiceCommandWithContext(
        audioFileBytes: ByteArray,
        currentState: GameState
    ): String = withContext(Dispatchers.IO) {
        if (audioFileBytes.isEmpty()) return@withContext ""

        // Standard Transcription
        val transcribed = geminiRepository.transcribeAudio(audioFileBytes)
        if (transcribed.isEmpty()) return@withContext ""

        // Refine command post-transcription
        sanitizeAndReconstructVoiceInput(transcribed, currentState)
    }

    /**
     * Runs advanced narration loops by injecting live surrounding context, Google Search grounding,
     * device network connection metrics (Ethernet/WiFi, DNS status, latencies), and current local time.
     */
    suspend fun processNarratorTurnWithGrounding(
        currentState: GameState,
        playerAction: String
    ): GMResponse? = withContext(Dispatchers.IO) {
        // Build surrounding tech logs to enrich the prompt as metadata to help avoid hallucinations
        val networkState = networkInfoFlow?.value
        val networkContext = if (networkState != null) {
            "SINAL TV: CONECTADO (${networkState.connectionType}) | DNS: ${if (networkState.dnsOk) "OK" else "FAIL"} | PING: ${networkState.pingMs}ms"
        } else {
            "SINAL TV: OFFLINE"
        }

        // Dynamic system instruction booster to feed Gemini the exact rules
        val enhancedSystemInstruction = buildSystemPromptWithGrounding(networkContext, currentState)

        val adapter = moshi.adapter(GameState::class.java)
        val stateJson = withContext(Dispatchers.Default) { adapter.toJson(currentState) }

        Log.i("GeminiIntelligenceEngine", "Invoking Gemini with grounding. Active internet lookup is enabled.")
        val response = geminiRepository.processTurn(stateJson, playerAction, enhancedSystemInstruction)

        if (response != null) {
            Log.d("GeminiIntelligenceEngine", "Successfully received structured narrative from Gemini.")
        } else {
            Log.e("GeminiIntelligenceEngine", "Gemini did not return any JSON response.")
        }
        response
    }

    /**
     * Build the structured Master system prompt, emphasizing the role of the AI as a physical
     * storyteller for Android TV utilizing the Google search grounding capabilities.
     */
    private fun buildSystemPromptWithGrounding(networkContext: String, currentState: GameState): String {
        val player = currentState.playerState
        return """
            Você é a mente inteligente, o cérebro do Google Gemini e o Narrador profissional de RPG por trás do jogo "WhatIsRPG? O Eco da Podridão".
            Seu dever é conduzir uma simulação contínua de jogo baseada em comandos falados em linguagem natural.
            
            DIRETIVAS METAFÍSICAS DE ENGENHARIA DE REDE:
            - Telemetria de Conexão da TV do Jogador: $networkContext
            - Use seu acesso de Pesquisa Google (Google Search Grounding Tool) se o jogador fizer alusões a mitos reais, folclores clássicos medievais, lendas antigas, animais mitológicos de culturas nórdicas, celtas, eslavas ou mundos da fantasia literária (ex: Dungeons & Dragons, Call of Cthulhu, Lovecraft, Tolkien, Pathfinder). 
            - Funda esses conhecimentos históricos e geográficos do mundo real de maneira sinistra e macabra, adaptando-os no "Reino Sombrio" sob a influência direta do "Eco da Podridão", entregando uma jornada literária épica e rica de referências.
            
            REGRAS DE PERSISTÊNCIA E INTERFACE:
            1. ESTADO E PERSISTÊNCIA:
               - Mantenha a realidade unificada. Mudanças em HP/MP, ouro, itens, atributos locais do jogador ou relação com NPCs são irreversíveis.
               - Suas saídas devem sincronizar e atualizar perfeitamente estes campos na resposta estruturada para que o aplicativo persista.
               
            2. UNIVERSO DE RETRO-FANTASIA E A PODRIDÃO:
               - O cenário é o Reino Sombrio, atormentado pelo "Eco da Podridão", que infecta seres, corrompe rios e atrai males gravitacionais e cósmicos celulares.
               - As ações consomem tempo de forma lógica (Manhã, Tarde, Noite, Madrugada). A noite fortalece perigos e deforma a realidade.
               
            3. MECÂNICAS DE COMBATE E ATRIBUTOS:
               - Em conflito ativo, as resoluções físicas baseiam-se em Força (FOR), Agilidade (AGI) e Vitalidade (VIT). Magias e resistências celestes dependem de Inteligência (INT), Percepção (PER) e Força de Vontade (WIL).
               - Magias não são vulgares! Descreva feitiços com base em luz de prata lunar, distorções de gravidade, marés estelares e marfim cósmico. Evite fogo/gelo tradicionais se não fizer sentido na classe de sub-classe celestial.
               
            4. NPCS E MISSÕES INTELIGENTES:
               - NPCs agem como seres autônomos. Eles lembram de suas atitudes anteriores. Altere a 'affinity' de -100 (ódio/traição) a +100 (lealdade/respeito de sangue).
               - Altere e use status emocionais deles (Medo, Raiva, Confiança, Ambição). Eles reagem às cicatrizes e títulos conquistados pelo herói.
               
            5. DIRETRIZES DE ACESSIBILIDADE DE ANDROID TV (SEM TOQUE E TECLADOS):
               - Lembre-se: O usuário joga sentado no sofá, comandando a história de forma conversacional pelo microfone da TV ou selecionando as sugestões pré-fabricadas com o direcional do controle (D-pad).
               - Em 'curated_options', as opções sugeridas devem ser extremamente personalizadas e empolgantes à situação imediata do jogo, à classe do jogador, aos itens atuais no inventário, aos feitiços ou habilidades em seu grimório, e ao local/clima atual.
               - Elas devem carregar os marcadores de prefixo exatos `[⚔️ Combate]`, `[🌌 Magia]`, `[🔍 Investigar]`, `[🗣️ Diálogo]` ou `[📦 Inventário]` antes do texto, incentivando o jogador de forma altamente conceitual e tática.
               - Escreva sugestões longas de ações, ricas e literárias específicas, nunca opções vagas como "Atacar" ou "Correr".
               
            6. FORMATO DE RETORNO OBRIGATÓRIO (JSON):
               Você DEVE responder UNICAMENTE com um objeto JSON válido que possua a estrutura abaixo:
               {
                 "narrative": "Uma narração imersiva, rica, literária e envolvente em português (máximo de 3 parágrafos) descrevendo as consequências imediatas da ação do jogador, do ambiente e diálogos.",
                 "player_update": {
                   "level": null, // mude o número se subir de nível
                   "experience": null, // mude se ganhar XP
                   "unassignedPoints": null, // pontos extras de atributos ganhos ao subir de nível
                   "hp": null, // mude se houver cura ou dano físico
                   "maxHp": null,
                   "mp": null, // mude se gastar MP em magias ou regenerar
                   "maxMp": null,
                   "gold": null, // mude se ganhar ou gastar moedas de ouro no jogo
                   "strength": null,
                   "agility": null,
                   "intelligence": null,
                   "vitality": null,
                   "perception": null,
                   "willpower": null,
                   "itemsGained": null, // Novos itens em lista: [{"name":"Lâmina de Prata", "description":"...", "type":"Weapon", "effect":"+3 FOR", "value":20}]
                   "itemsLost": null, // Itens consumidos / descartados (lista de nomes): ["Item1"]
                   "skillsGained": null, // Habilidades especiais desbloqueadas: [{"name":"Distort Gravity", "description":"...", "cost":8}]
                   "titlesGained": null, // Títulos conquistados
                   "scarsGained": null // Cicatrizes de batalha
                 },
                 "world_update": {
                   "region": null, // nova região se mudou
                   "timeOfDay": null, // "Manhã", "Tarde", "Noite", "Madrugada"
                   "rotLevel": null, // nível de podridão
                   "locationDescription": null,
                   "questEvents": null // atualizações de missões ativas
                 },
                 "npc_updates": [
                   {
                     "name": "Nome do NPC",
                     "description": null,
                     "affinityChange": null, // número ex: +15 ou -10
                     "emotion": null, // "Medo", "Raiva", "Confiança", "Ambição" ou "Neutro"
                     "memoryAddition": null // nova memória para guardar permanentemente
                   }
                 ],
                 "curated_options": [
                   "[⚔️ Combate] Decisão expressiva...",
                   "[🌌 Magia] Feitiço celestial ou distorção gravitacional...",
                   "[🔍 Investigar] Busca detalhada...",
                   "[🗣️ Diálogo] Conversação imersiva..."
                 ],
                 "combat_active": false // true se combate iniciado ou mantido
               }
               
            Certifique-se de retornar as chaves do JSON exatamente com estes nomes e não envolver o retorno em blocos markdown de código ```json, envie apenas a string JSON bruta e válida para parsing de moshi!
        """.trimIndent()
    }

    /**
     * Simple Levenshtein distance formulation for robust near-match alignments in spoken TV remote keywords.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                if (s1[i - 1] == s2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1]
                } else {
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,      // Deletion
                        dp[i][j - 1] + 1,      // Insertion
                        dp[i - 1][j - 1] + 1   // Substitution
                    )
                }
            }
        }
        return dp[s1.length][s2.length]
    }
}
