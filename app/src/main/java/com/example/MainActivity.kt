package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rpg.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0C0A0F) // Ultra-dark immersive slate background
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        VoiceRPGApp(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceRPGApp(viewModel: GameViewModel) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Modo teclado ativado devido à permissão negada.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val transcription by viewModel.transcription.collectAsStateWithLifecycle()
    val errorMsg by viewModel.errorMessage.collectAsStateWithLifecycle()

    // Screen error handler
    errorMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Mensagem da Masmorra", color = Color(0xFFFFD43F), fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = Color.White) },
            confirmButton = {
                TVButton(onClick = { viewModel.clearError() }) {
                    Text("Compreendido", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1C24)
        )
    }

    // Permission explanation barrier if mic required but denied initially
    if (!hasMicPermission) {
        PermissionScreen(
            onRequestPermission = { requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onContinueKeyboardOnly = { hasMicPermission = true } // Allow user to skip and play purely with buttons/D-pad!
        )
    } else {
        // Main Game Layout
        MainGameLayout(
            gameState = gameState,
            audioState = audioState,
            transcription = transcription,
            viewModel = viewModel
        )
    }
}

@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit,
    onContinueKeyboardOnly: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1124), Color(0xFF0C0A0F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(0.8f)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF8B8B),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "PERMISSÃO DE ÁUDIO REQUERIDA",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "WhatIsRPG? é conduzido inteiramente por comandos de voz gravados a partir do microfone do controle remoto de sua Android TV. Precisamos da permissão de gravação para transcrever seus sussurros ancestrais via Inteligência Artificial.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFC0BAC4),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TVButton(onClick = onRequestPermission, modifier = Modifier.testTag("request_mic_permission")) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Green)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Conceder Permissão", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(20.dp))
                TVButton(onClick = onContinueKeyboardOnly, modifier = Modifier.testTag("continue_keyboard_button")) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Modo Sem Voz (Navegação D-Pad)", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun MainGameLayout(
    gameState: GameState,
    audioState: AudioState,
    transcription: String,
    viewModel: GameViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09080C))
    ) {
        // SIDEBAR STATS & GRIMOIRE PANEL (35% Width)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.35f)
                .background(Color(0xFF131118))
                .border(1.dp, Color(0xFF1E1B24))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            GameTitleSection()
            Spacer(modifier = Modifier.height(16.dp))

            // Player Stats Area
            if (gameState.creationStep == "RUNNING") {
                PlayerStatsView(gameState.playerState, viewModel)
                Spacer(modifier = Modifier.height(20.dp))
                WorldStatusView(gameState.worldState)
                Spacer(modifier = Modifier.height(20.dp))
                ActiveNpcListView(gameState.npcs)
            } else {
                CharacterCreationStepPanel(gameState.creationStep, gameState.playerState)
            }
        }

        // NARRATIVE CENTER & CORE INTERACTION PANEL (65% Width)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Narrative logs scroll
            NarrativeBox(
                history = gameState.history,
                creationStep = gameState.creationStep,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // AI Speech and Remote Mic Record Indicator / Visualizer Area
            VoiceRecorderConsole(
                audioState = audioState,
                transcription = transcription,
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Curated Options Buttons (Perfect for D-Pad click interaction on TV)
            CuratedOptionsPanel(
                options = gameState.options,
                onOptionSelect = { text -> viewModel.enterCommandLine(text) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun GameTitleSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD43F),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "WhatIsRPG?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                color = Color.White,
                fontSize = 24.sp
            )
        }
        Text(
            text = "O Eco da Podridão",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8A6BFF),
            letterSpacing = 2.sp,
            fontSize = 11.sp
        )
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(0.5f)
                .height(1.dp)
                .background(Color(0xFF2C2536))
        )
    }
}

@Composable
fun CharacterCreationStepPanel(step: String, player: PlayerState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B26)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "CRIAÇÃO DO HERÓI",
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD43F),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            CreationStatItem("Nome", player.name.ifEmpty { "A definir..." })
            CreationStatItem("Aparência", player.appearance.ifEmpty { "A definir..." })
            CreationStatItem("Gênero", player.gender.ifEmpty { "A definir..." })
            CreationStatItem("Sexualidade", player.sexuality.ifEmpty { "A definir..." })
            CreationStatItem("Raça", player.race.ifEmpty { "A definir..." })
            CreationStatItem("Classe", player.className.ifEmpty { "A definir..." })
            CreationStatItem("Subclasse", player.subclass.ifEmpty { "A definir..." })

            Spacer(modifier = Modifier.height(16.dp))
            
            val progress = when(step) {
                "NOT_STARTED" -> 0f
                "NOME" -> 0.15f
                "APARENCIA" -> 0.3f
                "GENERO" -> 0.45f
                "SEXUALIDADE" -> 0.6f
                "RACE" -> 0.75f
                "CLASS" -> 0.9f
                else -> 1f
            }

            LinearProgressIndicator(
                progress = { progress },
                color = Color(0xFF8A6BFF),
                trackColor = Color(0xFF131118),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Etapa: $step",
                color = Color(0xFF8A6BFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CreationStatItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFFC0BAC4), fontSize = 12.sp)
        Text(
            text = value,
            color = if (value.contains("definir")) Color.DarkGray else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.6f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun PlayerStatsView(player: PlayerState, viewModel: GameViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1B22)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Identity Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = player.name.uppercase(),
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${player.race} • ${player.className} (${player.subclass})",
                        color = Color(0xFF8A6BFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF2C243B), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NÍVEL ${player.level}",
                        color = Color(0xFFFFD43F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // XP Progression bar
            Text(
                text = "EXPERIÊNCIA: ${player.experience} / ${player.maxExperience} XP",
                fontSize = 10.sp,
                color = Color(0xFFC0BAC4),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { player.experience.toFloat() / player.maxExperience.toFloat() },
                color = Color(0xFF8A6BFF),
                trackColor = Color(0xFF131118),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Vitality Resources
            StatGauge("HP / VIDA", player.hp, player.maxHp, Color(0xFFFF4E4E))
            Spacer(modifier = Modifier.height(8.dp))
            StatGauge("MP / MANA", player.mp, player.maxMp, Color(0xFF4579FF))

            Spacer(modifier = Modifier.height(16.dp))

            // Economy
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD43F),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${player.gold} PEÇAS DE OURO",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Core Attributes Checklist
            Text(
                text = "ATRIBUTOS DO CORE",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD43F)
            )
            Spacer(modifier = Modifier.height(8.dp))

            AttributeRow("FORÇA (FOR)", player.attributes.strength, player.unassignedPoints > 0, viewModel, "FOR")
            AttributeRow("AGILIDADE (AGI)", player.attributes.agility, player.unassignedPoints > 0, viewModel, "AGI")
            AttributeRow("INTELIGÊNCIA (INT)", player.attributes.intelligence, player.unassignedPoints > 0, viewModel, "INT")
            AttributeRow("VITALIDADE (VIT)", player.attributes.vitality, player.unassignedPoints > 0, viewModel, "VIT")
            AttributeRow("PERCEPÇÃO (PER)", player.attributes.perception, player.unassignedPoints > 0, viewModel, "PER")
            AttributeRow("VONTADE (WIL)", player.attributes.willpower, player.unassignedPoints > 0, viewModel, "WIL")

            if (player.unassignedPoints > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Você possui ${player.unassignedPoints} pontos livres! Distribua acima.",
                    color = Color.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            // Scars and Titles display if exists
            if (player.titles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "TÍTULOS RECONHECIDOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8A6BFF)
                )
                player.titles.forEach { title ->
                    Text(text = "🛡️ \"$title\"", color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun StatGauge(label: String, value: Int, max: Int, color: Color) {
    val displayedVal = value.coerceIn(0, max)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(text = "$displayedVal / $max", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { displayedVal.toFloat() / max.toFloat() },
        color = color,
        trackColor = Color(0xFF131118),
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
    )
}

@Composable
fun AttributeRow(
    name: String, 
    value: Int, 
    canPlus: Boolean, 
    viewModel: GameViewModel,
    statId: String
) {
    var isPlusFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, color = Color(0xFFC0BAC4), fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            if (canPlus) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (isPlusFocused) Color(0xFF8A6BFF) else Color(0xFF2C2930), 
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp, 
                            if (isPlusFocused) Color.White else Color.Transparent, 
                            RoundedCornerShape(4.dp)
                        )
                        .focusable(true)
                        .onFocusChanged { isPlusFocused = it.isFocused }
                        .clickable { viewModel.distributeStat(statId) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = "+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun WorldStatusView(world: WorldState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15141B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ESTADO DO MUNDO VIVO",
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD43F),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            WorldRow(Icons.Default.Info, "Região Atual", world.region)
            WorldRow(Icons.Default.Refresh, "Tempo e Ciclo", world.timeOfDay)
            
            // Living rot indicator
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Podridão do Solo", color = Color(0xFFFF8B8B), fontSize = 11.sp)
                Text(text = "${world.rotLevel}% Corrompido", color = Color(0xFFFF4E4E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { world.rotLevel.toFloat() / 100f },
                color = Color(0xFFFF4E4E),
                trackColor = Color(0xFF131118),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
fun WorldRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF8A6BFF), modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, color = Color.Gray, fontSize = 10.sp)
            Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ActiveNpcListView(npcs: List<NpcState>) {
    if (npcs.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "SOBREVIVENTES ENCONTRADOS",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8A6BFF),
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        npcs.forEach { npc ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1920)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = npc.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        Text(text = npc.description, color = Color(0xFFC0BAC4), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = "Emoção: ${npc.emotion}", color = Color.Gray, fontSize = 9.sp)
                    }
                    // Mini affinity layout
                    Box(
                        modifier = Modifier
                            .background(
                                if (npc.affinity >= 0) Color(0xFF223624) else Color(0xFF3B1E1E),
                                CircleShape
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (npc.affinity >= 0) "+${npc.affinity} Amizade" else "${npc.affinity} Repúdio",
                            color = if (npc.affinity >= 0) Color.Green else Color(0xFFFF5252),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NarrativeBox(
    history: List<LogEntry>,
    creationStep: String,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto Scroll to last narrative entries automatically
    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF110E14)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF231E2A)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF8A6BFF))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(history) { entry ->
                        val isNarrator = entry.speaker == "Narrador"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isNarrator) Arrangement.Start else Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(
                                        color = if (isNarrator) Color(0xFF1E1B26) else Color(0xFF2C243B),
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isNarrator) 0.dp else 16.dp,
                                            bottomEnd = if (isNarrator) 16.dp else 0.dp
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isNarrator) Color(0xFF332049) else Color(0xFF513875),
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isNarrator) 0.dp else 16.dp,
                                            bottomEnd = if (isNarrator) 16.dp else 0.dp
                                        )
                                    )
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = entry.speaker,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNarrator) Color(0xFFFFD43F) else Color(0xFFB19EFF),
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Voz Ativa",
                                            color = Color.DarkGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = entry.message,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Serif,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceRecorderConsole(
    audioState: AudioState,
    transcription: String,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    var isButtonFocused by remember { mutableStateOf(false) }
    var isStopSpokenFocused by remember { mutableStateOf(false) }
    var isResetFocused by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition()
    val recordScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (audioState == AudioState.Recording) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (audioState == AudioState.Recording) 0.8f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14121A)),
        border = BorderStroke(1.dp, Color(0xFF231E29)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left block: mic state text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Mic glowing ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    if (audioState == AudioState.Recording) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .scale(recordScale)
                                .background(Color(0xFFFF3E3E).copy(alpha = haloAlpha), CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                when (audioState) {
                                    AudioState.Recording -> Color(0xFFFF3E3E)
                                    AudioState.Transcribing -> Color(0xFFFFD43F)
                                    AudioState.ProcessingTurn -> Color(0xFF8A6BFF)
                                    AudioState.SpeakingNarrator -> Color(0xFF45FFB2)
                                    else -> Color(0xFF332D3F)
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (audioState) {
                                AudioState.Recording -> Icons.Default.PlayArrow
                                AudioState.Transcribing -> Icons.Default.Refresh
                                AudioState.ProcessingTurn -> Icons.Default.Info
                                AudioState.SpeakingNarrator -> Icons.Default.PlayArrow
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = when (audioState) {
                            AudioState.Recording -> "GRAVANDO... Fale no comando da TV"
                            AudioState.Transcribing -> "TRANSCREVENDO SUBSURROS VIA IA..."
                            AudioState.ProcessingTurn -> "DUNGEON MASTER SIMULANDO REALIDADE..."
                            AudioState.SpeakingNarrator -> "SUSSURRANDO VOZ DO NARRADOR..."
                            else -> "CONECTAR AO MICROFONE DO CONTROLE"
                        },
                        fontWeight = FontWeight.Bold,
                        color = when (audioState) {
                            AudioState.Recording -> Color(0xFFFF3E3E)
                            AudioState.Transcribing -> Color(0xFFFFD43F)
                            AudioState.ProcessingTurn -> Color(0xFF8A6BFF)
                            AudioState.SpeakingNarrator -> Color(0xFF45FFB2)
                            else -> Color(0xFFC0BAC4)
                        },
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (transcription.isNotEmpty()) "Último áudio: \"$transcription\"" else "Pressione OK abaixo ou no controle remoto para falar.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right block: Action buttons (OK and options to skip or reset)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 1. Microphone Toggle
                TVButton(
                    onClick = { viewModel.toggleVoiceRecording() },
                    modifier = Modifier
                        .onFocusChanged { isButtonFocused = it.isFocused }
                        .border(
                            2.dp, 
                            if (isButtonFocused) Color(0xFFFFD43F) else Color.Transparent, 
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("record_voice_button")
                ) {
                    Icon(
                        imageVector = if (audioState == AudioState.Recording) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (audioState == AudioState.Recording) Color.Red else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (audioState == AudioState.Recording) "PARAR DE FALAR" else "FALAR (OK)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 2. Stop spoken narration option (if talking)
                if (audioState == AudioState.SpeakingNarrator) {
                    TVButton(
                        onClick = { viewModel.stopSpokenNarration() },
                        modifier = Modifier
                            .onFocusChanged { isStopSpokenFocused = it.isFocused }
                            .border(
                                2.dp, 
                                if (isStopSpokenFocused) Color.White else Color.Transparent, 
                                RoundedCornerShape(12.dp)
                            )
                            .testTag("silence_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.LightGray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Silenciar", fontSize = 11.sp, color = Color.LightGray)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // 3. Complete game restart slot
                TVButton(
                    onClick = { viewModel.resetGame() },
                    modifier = Modifier
                        .onFocusChanged { isResetFocused = it.isFocused }
                        .border(
                            2.dp, 
                            if (isResetFocused) Color(0xFFFF4E4E) else Color.Transparent, 
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("restart_game_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFFF4E4E), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deletar Salve / Reset", color = Color(0xFFFF4E4E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CuratedOptionsPanel(
    options: List<String>,
    onOptionSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "AÇÕES DISPONÍVEIS (SELECIONE COM O DIRECIONAL DO CONTROLE OU FALE):",
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFFD43F),
            letterSpacing = 1.sp,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Horizontal list of D-Pad reachable quick choices
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { option ->
                TVButton(
                    onClick = { onOptionSelect(option) },
                    modifier = Modifier.testTag("option_${option.replace(" ", "_")}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFFB19EFF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = option,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// Custom specialized D-Pad focus-sensitive wrapper optimized for television viewport
@Composable
fun TVButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    val borderColor by animateColorAsState(if (isFocused) Color(0xFF8A6BFF) else Color(0xFF2E243B))
    val backgroundFillColor by animateColorAsState(if (isFocused) Color(0xFF272133) else Color(0xFF191621))

    Box(
        modifier = modifier
            .scale(scale)
            .background(backgroundFillColor, RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .focusable(true)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}
