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
import androidx.compose.animation.animateContentSize
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
    var showSettingsDialog by remember { mutableStateOf(false) }
    val showFirstRunNotification by viewModel.showFirstRunNotification.collectAsStateWithLifecycle()
    var selectedSidebarTab by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF13101C), Color(0xFF060509)),
                    radius = 2200f
                )
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // SIDEBAR STATS & GRIMOIRE PANEL (32% Width) - Sleek Translucent Minimalist Column
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.32f)
                    .background(Color(0xFF0A080E).copy(alpha = 0.85f))
                    .border(BorderStroke(1.dp, Color(0xFF1F1A2D).copy(alpha = 0.5f)))
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                GameTitleSection(viewModel = viewModel, onSettingsClick = { showSettingsDialog = true })
                Spacer(modifier = Modifier.height(12.dp))

                // Conditionally render player dashboard or creation tracker with high-contrast minimalism
                if (gameState.creationStep == "RUNNING") {
                    // D-Pad Responsive Interactive Tab selector row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabNames = listOf("HERÓI", "HABILIDADES", "MUNDO")
                        val tabIcons = listOf(Icons.Default.Person, Icons.Default.Star, Icons.Default.Home)
                        
                        tabNames.forEachIndexed { index, name ->
                            var isFocused by remember { mutableStateOf(false) }
                            val isSelected = selectedSidebarTab == index
                            
                            val scale by animateFloatAsState(if (isFocused) 1.05f else 1.0f)
                            val containerBg = if (isSelected) Color(0xFF221633) else if (isFocused) Color(0xFF161224) else Color.Transparent
                            val strokeColor = if (isSelected) Color(0xFF8A6BFF) else if (isFocused) Color(0xFF4C3A66) else Color(0xFF1F1A2D)
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .scale(scale)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(containerBg)
                                    .border(1.dp, strokeColor, RoundedCornerShape(8.dp))
                                    .focusable(true)
                                    .onFocusChanged { 
                                        isFocused = it.isFocused 
                                        if (it.isFocused) {
                                            selectedSidebarTab = index
                                            viewModel.speakGenericText("Aba $name selecionada.")
                                        }
                                    }
                                    .clickable {
                                        selectedSidebarTab = index
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = tabIcons[index],
                                        contentDescription = name,
                                        tint = if (isSelected) Color(0xFFFFD43F) else if (isFocused) Color.White else Color(0xFF8A6BFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = name,
                                        color = if (isSelected || isFocused) Color.White else Color(0xFF9E95A8),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    when (selectedSidebarTab) {
                        0 -> {
                            PlayerStatsView(gameState.playerState, viewModel)
                        }
                        1 -> {
                            PlayerAttributesView(gameState.playerState, viewModel)
                            Spacer(modifier = Modifier.height(10.dp))
                            PlayerGrimoireView(gameState.playerState, viewModel)
                        }
                        2 -> {
                            WorldStatusView(gameState.worldState, viewModel)
                            Spacer(modifier = Modifier.height(10.dp))
                            ActiveNpcListView(gameState.npcs, viewModel)
                            Spacer(modifier = Modifier.height(10.dp))
                            TvNavigationGuidePanel()
                        }
                    }
                } else {
                    CharacterCreationStepPanel(gameState.creationStep, gameState.playerState, viewModel)
                    Spacer(modifier = Modifier.height(12.dp))
                    TvNavigationGuidePanel()
                }
            }

            // Divider vertical line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color(0xFF1B1626))
            )

            // NARRATIVE CENTER & CORE INTERACTION PANEL (68% Width) - Pure Spacious Immersive Space
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Main Narrative Display Scroll Panel
                NarrativeBox(
                    history = gameState.history,
                    creationStep = gameState.creationStep,
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Glowing Assistive Voice Recorder & Micro Radar View
                VoiceRecorderConsole(
                    audioState = audioState,
                    transcription = transcription,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // D-Pad Friendly Curated Options Panels
                CuratedOptionsPanel(
                    options = gameState.options,
                    onOptionSelect = { text -> viewModel.enterCommandLine(text) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // State variables for auto elements
        val showUpdateOfferNotification by viewModel.showUpdateOfferNotification.collectAsStateWithLifecycle()
        val isApplyingUpdate by viewModel.isApplyingUpdate.collectAsStateWithLifecycle()
        val updateProgress by viewModel.updateProgress.collectAsStateWithLifecycle()
        val updateStatusText by viewModel.updateStatusText.collectAsStateWithLifecycle()

        // Overlay 1: Non-intrusive Update notification prompt at startup
        if (showFirstRunNotification) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Transparent)
            ) {
                VersionNotificationPrompt(viewModel = viewModel, onDismiss = { viewModel.dismissFirstRunNotification() })
            }
        }

        // Overlay 2: Dynamic Auto Update Offer / Installer Progress modal
        if (showUpdateOfferNotification != null || isApplyingUpdate) {
            val targetInfo = showUpdateOfferNotification ?: com.example.rpg.GitHubUpdateInfo(
                buildName = "Hot Update",
                appVersion = viewModel.installedVersion.collectAsStateWithLifecycle().value + 0.1,
                dateAndHour = "Instante Recente",
                changesList = emptyList(),
                rawNotesSection = "Preparando a aplicação de novos pacotes de dados...",
                isMoreRecent = true
            )
            UpdateOfferDialog(
                updateInfo = targetInfo,
                isApplying = isApplyingUpdate,
                progress = updateProgress,
                statusText = updateStatusText,
                onApply = {
                    showUpdateOfferNotification?.let {
                        viewModel.applyHotUpdate(it.appVersion)
                    }
                },
                onDefer = {
                    viewModel.deferUpdate()
                }
            )
        }

        // Overlay 3: Full-screen modal settings of the update center
        if (showSettingsDialog) {
            SettingsAndUpdatesDialog(
                viewModel = viewModel,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
fun GameTitleSection(viewModel: GameViewModel, onSettingsClick: () -> Unit) {
    var isHeaderFocused by remember { mutableStateOf(false) }
    
    // Dynamic spring properties scaled perfectly for TV remote control focus
    val scale by animateFloatAsState(
        targetValue = if (isHeaderFocused) 1.03f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isHeaderFocused) Color(0xFF1E162D).copy(alpha = 0.9f) else Color.Transparent)
            .border(
                1.dp, 
                if (isHeaderFocused) Color(0xFFFFD43F) else Color(0xFF1F1A2D), 
                RoundedCornerShape(12.dp)
            )
            .focusable(true)
            .onFocusChanged { isHeaderFocused = it.isFocused }
            .clickable { onSettingsClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Configurações",
                tint = if (isHeaderFocused) Color(0xFFFFD43F) else Color(0xFFB19EFF),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "WhatIsRPG?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = Color.White,
                fontSize = 22.sp,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val installedVer by viewModel.installedVersion.collectAsStateWithLifecycle()
            Text(
                text = "O ECO DA PODRIDÃO v$installedVer",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isHeaderFocused) Color.White else Color(0xFF8A6BFF),
                letterSpacing = 1.5.sp,
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFF2C243B), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("CONFIGS", color = Color(0xFFFFD43F), fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(0.4f)
                .height(1.dp)
                .background(Color(0xFF201B2B))
        )
    }
}

@Composable
fun CharacterCreationStepPanel(step: String, player: PlayerState, viewModel: GameViewModel) {
    var isCreationFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isCreationFocused) 1.02f else 1.0f)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0D15)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isCreationFocused) Color(0xFF8A6BFF) else Color(0xFF1E172B)),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .focusable(true)
            .onFocusChanged { isCreationFocused = it.isFocused }
            .clickable {
                viewModel.speakGenericText(
                    "Criação do herói em andamento. Etapa atual: $step. " +
                    "Nome: ${player.name.ifEmpty { "Ainda não definido" }}. " +
                    "Raça: ${player.race.ifEmpty { "Ainda não definida" }}. " +
                    "Classe: ${player.className.ifEmpty { "Ainda não definida" }}."
                )
            }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CRIAÇÃO DO HERÓI",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD43F),
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                if (isCreationFocused) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Ouvir",
                        tint = Color(0xFF45FFB2),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            CreationStatItem("Nome", player.name.ifEmpty { "A definir..." })
            CreationStatItem("Aparência", player.appearance.ifEmpty { "A definir..." })
            CreationStatItem("Gênero", player.gender.ifEmpty { "A definir..." })
            CreationStatItem("Sexualidade", player.sexuality.ifEmpty { "A definir..." })
            CreationStatItem("Raça", player.race.ifEmpty { "A definir..." })
            CreationStatItem("Classe", player.className.ifEmpty { "A definir..." })
            CreationStatItem("Subclasse", player.subclass.ifEmpty { "A definir..." })

            Spacer(modifier = Modifier.height(14.dp))
            
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
                trackColor = Color(0xFF0A080D),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ETAPA ATUAL: $step",
                color = Color(0xFF8A6BFF),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun CreationStatItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF9E95A8), fontSize = 11.sp)
        Text(
            text = value,
            color = if (value.contains("definir")) Color.DarkGray else Color.White,
            fontSize = 11.sp,
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF100D1C).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF261D38)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Player basic header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "👤 ${player.name.uppercase()}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 130.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF20162F), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFFFD43F), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Lvl ${player.level}",
                            color = Color(0xFFFFD43F),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Text(
                    text = player.className.ifEmpty { "Viajante" },
                    color = Color(0xFF8A6BFF),
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${player.race} · ${player.subclass.ifEmpty { "Sem Subclasse" }}",
                color = Color(0xFFB19EFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // EXP meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXPERIÊNCIA DE ALMA",
                    fontSize = 8.sp,
                    color = Color(0xFF9E95A8),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${player.experience} / ${player.maxExperience} XP",
                    fontSize = 9.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { player.experience.toFloat() / player.maxExperience.toFloat() },
                color = Color(0xFF8A6BFF),
                trackColor = Color(0xFF0A080D),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // HP and MP gauges
            StatGauge("HP / VITALIDADE", player.hp, player.maxHp, Color(0xFFFF4E4E))
            Spacer(modifier = Modifier.height(6.dp))
            StatGauge("MP / ENERGIA CELESTE", player.mp, player.maxMp, Color(0xFF3B66FF))

            Spacer(modifier = Modifier.height(10.dp))

            // Gold badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF221A0F), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFFFD43F).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD43F), modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("MOEDAS DE OURO", color = Color(0xFFFFD43F), fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text("${player.gold} PEÇAS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
				}
			}

            if (player.titles.isNotEmpty() || player.scars.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ALCUNHAS & CICATRIZES DO HERÓI",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF8A6BFF),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                player.titles.forEach { title ->
                    Text(text = "🛡️ \"$title\"", color = Color.LightGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                player.scars.forEach { scar ->
                    Text(text = "🤕 $scar", color = Color(0xFFFF8B8B), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "🎒 MOCHILA E DIRETÓRIO DE ITENS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD43F),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (player.inventory.isEmpty()) {
                Text(text = "Nenhum item na mochila.", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
            } else {
                player.inventory.forEach { item ->
                    var isItemFocused by remember { mutableStateOf(false) }
                    val itemScale by animateFloatAsState(targetValue = if (isItemFocused) 1.04f else 1.0f)
                    val cardBgColor = if (isItemFocused) Color(0xFF221633) else Color(0xFF130F1F)
                    val borderColor = if (isItemFocused) Color(0xFFFFD43F) else Color(0xFF1F1A2D)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .scale(itemScale)
                            .focusable(true)
                            .onFocusChanged { 
                                isItemFocused = it.isFocused 
                                if (it.isFocused) {
                                    viewModel.speakGenericText("Item focado: ${item.name}. ${item.description}")
                                }
                            }
                            .clickable {
                                viewModel.enterCommandLine("usar o item ${item.name}")
                                viewModel.speakGenericText("Utilizando item ${item.name}!")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                Text(text = item.description, color = Color(0xFF9E95A8), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF20162F), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = item.effect ?: "Uso", color = Color(0xFF45FFB2), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerAttributesView(player: PlayerState, viewModel: GameViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF100D1C).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF261D38)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⚔️ ATRIBUTOS D'ALMA",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD43F),
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    if (player.unassignedPoints > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF122C1A), RoundedCornerShape(6.dp))
                                .border(BorderStroke(1.dp, Color.Green), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+${player.unassignedPoints} PT LIVRES",
                                color = Color.Green,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            AttributeRow("FORÇA (FOR)", player.attributes.strength, player.unassignedPoints > 0, viewModel, "FOR")
            AttributeRow("AGILIDADE (AGI)", player.attributes.agility, player.unassignedPoints > 0, viewModel, "AGI")
            AttributeRow("INTELIGÊNCIA (INT)", player.attributes.intelligence, player.unassignedPoints > 0, viewModel, "INT")
            AttributeRow("VITALIDADE (VIT)", player.attributes.vitality, player.unassignedPoints > 0, viewModel, "VIT")
            AttributeRow("PERCEPÇÃO (PER)", player.attributes.perception, player.unassignedPoints > 0, viewModel, "PER")
            AttributeRow("VONTADE (WIL)", player.attributes.willpower, player.unassignedPoints > 0, viewModel, "WIL")

            if (player.unassignedPoints > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Você tem ${player.unassignedPoints} pontos livres! Navegue com o D-pad e pressione [OK] no controle para distribuir.",
                    color = Color.Green,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun PlayerGrimoireView(player: PlayerState, viewModel: GameViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF100D1C).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF261D38)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📜 GRIMÓRIO SECULAR",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD43F),
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2C243B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${player.grimoire.size} FEITIÇOS",
                            color = Color(0xFFB19EFF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            if (player.grimoire.isEmpty()) {
                Text(text = "Seu grimório está vazio. Aprenda feitiços progredindo na história do Mestre.", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
            } else {
                player.grimoire.forEach { skill ->
                    var isSkillFocused by remember { mutableStateOf(false) }
                    val skillScale by animateFloatAsState(targetValue = if (isSkillFocused) 1.04f else 1.0f)
                    val cardBgColor = if (isSkillFocused) Color(0xFF221633) else Color(0xFF130F1F)
                    val borderColor = if (isSkillFocused) Color(0xFF8A6BFF) else Color(0xFF1F1A2D)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .scale(skillScale)
                            .focusable(true)
                            .onFocusChanged { 
                                isSkillFocused = it.isFocused 
                                if (it.isFocused) {
                                    viewModel.speakGenericText("Focado feitiço ${skill.name}. Consome ${skill.cost} de mana. ${skill.description}")
                                }
                            }
                            .clickable {
                                viewModel.enterCommandLine("Conjurar a magia ${skill.name}")
                                viewModel.speakGenericText("Conjurando feitiço ${skill.name}!")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = skill.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF351212), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "${skill.cost} MP", color = Color(0xFFFF5252), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(text = skill.description, color = Color(0xFF9E95A8), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF20162F), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(text = skill.type, color = Color(0xFF8A6BFF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(text = "$displayedVal / $max", color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
    Spacer(modifier = Modifier.height(3.dp))
    LinearProgressIndicator(
        progress = { displayedVal.toFloat() / max.toFloat() },
        color = color,
        trackColor = Color(0xFF0A080D),
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
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
            .background(Color.Transparent, RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name, 
            color = Color(0xFF9E95A8), 
            fontSize = 11.sp, 
            fontWeight = FontWeight.Normal
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
            if (canPlus) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (isPlusFocused) Color(0xFF8A6BFF) else Color(0xFF20162B), 
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
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = "+", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun WorldStatusView(world: WorldState, viewModel: GameViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF100D1C).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF261D38)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🌍 REINO VIVO & CRONOMETRIA",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD43F),
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            WorldRow(Icons.Default.Info, "Região Atual", world.region)
            WorldRow(Icons.Default.Refresh, "Tempo e Ciclo", world.timeOfDay)
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Podridão Cósmica do Solo", color = Color(0xFFFF8B8B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(text = "${world.rotLevel}% Corrompido", color = Color(0xFFFF4E4E), fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { world.rotLevel.toFloat() / 100f },
                color = Color(0xFFFF4E4E),
                trackColor = Color(0xFF0A080D),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
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
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF8A6BFF), modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, color = Color.Gray, fontSize = 9.sp)
            Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TvNavigationGuidePanel() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0913)),
        border = BorderStroke(1.dp, Color(0xFF261D38)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFFFD43F),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GUIA DO CONTROLE REMOTO (TV)",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD43F),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            
            TvGuideItem("▲/▼/◄/►", "Navegação", "Use as setas direcionais do controle remoto para mover o foco brilhante entre as opções do jogo.")
            Spacer(modifier = Modifier.height(8.dp))
            TvGuideItem("[OK]", "Confirmar ação", "Pressione o botão central de confirmação ao focar em um botão de escolha para narrar sua decisão.")
            Spacer(modifier = Modifier.height(8.dp))
            TvGuideItem("🎙️ FALAR (OK)", "Comando de voz", "Foque o botão 'FALAR (OK)' no console, pressione [OK], diga seu comando verbal e pressione novamente para enviar.")
            Spacer(modifier = Modifier.height(8.dp))
            TvGuideItem("⚙️ CONFIG", "Central GitHub", "Foque no título do topo 'WhatIsRPG?' e pressione [OK] para configurar rep_owner, repositório ou buscar updates.")
        }
    }
}

@Composable
fun TvGuideItem(key: String, title: String, desc: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF22163A), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = key,
                    color = Color(0xFFB19EFF),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = desc,
            color = Color(0xFF9E95A8),
            fontSize = 9.sp,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun ActiveNpcListView(npcs: List<NpcState>, viewModel: GameViewModel) {
    if (npcs.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF100D1C).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF261D38)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "👥 SOBREVIVENTES QUE CONHECE",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD43F),
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2C243B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${npcs.size} COMPANHEIROS",
                            color = Color(0xFF8A6BFF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            npcs.forEach { npc ->
                var isNpcFocused by remember { mutableStateOf(false) }
                val itemScale by animateFloatAsState(targetValue = if (isNpcFocused) 1.04f else 1.0f)
                val cardBgColor = if (isNpcFocused) Color(0xFF221633) else Color(0xFF130F1F)
                val borderColor = if (isNpcFocused) Color(0xFFFFD43F) else Color(0xFF1F1A2D)

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(itemScale)
                        .focusable(true)
                        .onFocusChanged { 
                            isNpcFocused = it.isFocused 
                            if (it.isFocused) {
                                viewModel.speakGenericText("NPC focado: ${npc.name}. Humor atual: ${npc.emotion}. ${npc.description}")
                            }
                        }
                        .clickable {
                            viewModel.enterCommandLine("Falar com ${npc.name}")
                            viewModel.speakGenericText("Iniciando contato com ${npc.name}")
                        }
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = npc.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                if (isNpcFocused) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Falar", tint = Color(0xFF45FFB2), modifier = Modifier.size(12.dp))
                                }
                            }
                            Text(text = npc.description, color = Color(0xFF9E95A8), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = "Humor: ${npc.emotion}", color = Color.Gray, fontSize = 9.sp)
                        }
                        // Mini affinity capsule indicator
                        Box(
                            modifier = Modifier
                                .background(
                                    if (npc.affinity >= 0) Color(0xFF122C1A) else Color(0xFF351212),
                                    CircleShape
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (npc.affinity >= 0) "+${npc.affinity} Amizade" else "${npc.affinity} Ódio",
                                color = if (npc.affinity >= 0) Color.Green else Color(0xFFFF5252),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
    viewModel: GameViewModel,
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A080C)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF191424)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF8A6BFF))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history) { entry ->
                        val isNarrator = entry.speaker == "Narrador"
                        val isLast = entry == history.lastOrNull()
                        var isCardFocused by remember { mutableStateOf(false) }
                        val visualScale by animateFloatAsState(targetValue = if (isCardFocused) 1.02f else 1.0f)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isNarrator) Arrangement.Start else Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (isCardFocused) 0.92f else 0.88f)
                                    .scale(visualScale)
                                    .background(
                                        color = if (isNarrator) {
                                            if (isCardFocused) Color(0xFF1B1626) else Color(0xFF0F0D15)
                                        } else {
                                            if (isCardFocused) Color(0xFF291E3B) else Color(0xFF1D152A)
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .border(
                                        width = if (isCardFocused) 2.dp else 1.dp,
                                        color = if (isCardFocused) {
                                            if (isNarrator) Color(0xFFFFD43F) else Color(0xFF8A6BFF)
                                        } else {
                                            if (isNarrator) Color(0xFF1A1524) else Color(0xFF261A36)
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .focusable(isLast)
                                    .onFocusChanged { if (isLast) isCardFocused = it.isFocused }
                                    .clickable(enabled = isLast) {
                                        // Voice narration play on OK selection
                                        viewModel.speakGenericText(entry.message)
                                    }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isNarrator) Icons.Default.PlayArrow else Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (isNarrator) Color(0xFFFFD43F) else Color(0xFFB19EFF),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = entry.speaker.uppercase(),
                                                fontWeight = FontWeight.Black,
                                                color = if (isNarrator) Color(0xFFFFD43F) else Color(0xFFB19EFF),
                                                fontSize = 10.sp,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                        if (isCardFocused) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Ouvir",
                                                    tint = Color(0xFF45FFB2),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "OUVIR (OK)",
                                                    color = Color(0xFF45FFB2),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "Voz Ativa",
                                                color = Color.DarkGray,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = entry.message,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Serif,
                                        lineHeight = 20.sp
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
    
    // Ambient pulsation simulation for futuristic virtual microphone wave
    val infiniteTransition = rememberInfiniteTransition(label = "RadarMicPulse")
    
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (audioState == AudioState.Recording) 1.5f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (audioState == AudioState.Recording) 0f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0D15)),
        border = BorderStroke(1.dp, Color(0xFF1E172B)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side: Glowing Dome of Microphone
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(52.dp)
                    ) {
                        // Pulsating radar halo ring represent audio capture activity
                        if (audioState == AudioState.Recording) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(pulseScale1)
                                    .background(Color(0xFFFF3E3E).copy(alpha = pulseAlpha1), CircleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    when (audioState) {
                                        AudioState.Recording -> Color(0xFFFF3E3E)
                                        AudioState.Transcribing -> Color(0xFFFFD43F)
                                        AudioState.ProcessingTurn -> Color(0xFF8A6BFF)
                                        AudioState.SpeakingNarrator -> Color(0xFF45FFB2)
                                        else -> Color(0xFF221C2F)
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
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = when (audioState) {
                                AudioState.Recording -> "FALE NO CONTROLE REMOTO..."
                                AudioState.Transcribing -> "IA TRADUZINDO SUBSURROS..."
                                AudioState.ProcessingTurn -> "DUNGEON MASTER SIMULANDO REALIDADE..."
                                AudioState.SpeakingNarrator -> "SUSSURRANDO VOZ ATIVA..."
                                else -> "MICROFONE CONECTADO NO CONTROLE"
                            },
                            fontWeight = FontWeight.Black,
                            color = when (audioState) {
                                AudioState.Recording -> Color(0xFFFF3E3E)
                                AudioState.Transcribing -> Color(0xFFFFD43F)
                                AudioState.ProcessingTurn -> Color(0xFF8A6BFF)
                                AudioState.SpeakingNarrator -> Color(0xFF45FFB2)
                                else -> Color(0xFF8A6BFF)
                            },
                            letterSpacing = 0.5.sp,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (transcription.isNotEmpty()) "Comando falado: \"$transcription\"" else "Navegue ao botão FALAR e aperte OK do rádio.",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right block: Premium Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 1. Microphone capture toggle (remote OK press)
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
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (audioState == AudioState.Recording) "PARAR DE FALAR" else "FALAR (OK)",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 2. Stop spoken narration if speaking active
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
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mudar Voz", fontSize = 10.sp, color = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // 3. System hard reboot slot
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
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFFF4E4E), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reiniciar RPG", color = Color(0xFFFF4E4E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Dynamic Holographic Equalizer canvas panel below commands
            if (audioState != AudioState.Idle) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(Color(0xFF08060B), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E152B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val colCount = 40
                        val spacing = 6f
                        val barWidth = (size.width - (colCount - 1) * spacing) / colCount
                        
                        when (audioState) {
                            AudioState.Recording -> {
                                // Red dynamic voice spikes
                                val timeMs = System.currentTimeMillis()
                                for (i in 0 until colCount) {
                                    val phase = (timeMs / 180.0 + i * 0.35).toFloat()
                                    val factor = kotlin.math.sin(phase) * 0.5f + 0.5f
                                    val barHeight = 6f + factor * (size.height - 12f)
                                    val color = Color(0xFFFF3E3E).copy(alpha = 0.4f + factor * 0.6f)
                                    
                                    drawRoundRect(
                                        color = color,
                                        topLeft = androidx.compose.ui.geometry.Offset(i * (barWidth + spacing), (size.height - barHeight) / 2f),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                                    )
                                }
                            }
                            AudioState.Transcribing -> {
                                // Golden laser sweep lines
                                val scanPos = ((System.currentTimeMillis() / 8L) % (size.width.toLong())).toFloat()
                                drawLine(
                                    color = Color(0xFFFFD43F),
                                    start = androidx.compose.ui.geometry.Offset(scanPos, 0f),
                                    end = androidx.compose.ui.geometry.Offset(scanPos, size.height),
                                    strokeWidth = 3f
                                )
                                drawLine(
                                    color = Color(0xFFFFD43F).copy(alpha = 0.2f),
                                    start = androidx.compose.ui.geometry.Offset(scanPos - 15f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(scanPos - 15f, size.height),
                                    strokeWidth = 6f
                                )
                                drawLine(
                                    color = Color(0xFFFFD43F).copy(alpha = 0.1f),
                                    start = androidx.compose.ui.geometry.Offset(scanPos - 30f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(scanPos - 30f, size.height),
                                    strokeWidth = 10f
                                )
                            }
                            AudioState.ProcessingTurn -> {
                                // Violet morphing celestial stars/cosmic wave
                                val timeMs = System.currentTimeMillis()
                                for (i in 0 until colCount) {
                                    val phase = kotlin.math.cos(timeMs / 300.0 + i * 0.2).toFloat()
                                    val factor = phase * 0.5f + 0.5f
                                    val barHeight = 4f + factor * 14f
                                    val color = Color(0xFF8A6BFF).copy(alpha = 0.3f + factor * 0.7f)
                                    
                                    drawRoundRect(
                                        color = color,
                                        topLeft = androidx.compose.ui.geometry.Offset(i * (barWidth + spacing), (size.height - barHeight) / 2f),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                                    )
                                }
                            }
                            AudioState.SpeakingNarrator -> {
                                // Flowing emerald speaking dots
                                val timeMs = System.currentTimeMillis()
                                for (i in 0 until colCount) {
                                    val factor = (kotlin.math.sin(timeMs / 120.0 + i * 0.5) * 0.5 + 0.5).toFloat()
                                    val r = 2f + factor * 4f
                                    drawCircle(
                                        color = Color(0xFF45FFB2).copy(alpha = 0.5f + factor * 0.5f),
                                        radius = r,
                                        center = androidx.compose.ui.geometry.Offset(i * (barWidth + spacing) + barWidth/2, size.height / 2f)
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

data class ParsedOption(
    val category: String,
    val icon: ImageVector,
    val color: Color,
    val text: String
)

fun parseOption(option: String): ParsedOption {
    val cleanOption = option.trim()
    
    // Check for explicit prefix e.g. [⚔️ Combate] Atacar o lobo
    val prefixRegex = Regex("^\\[(.*?)\\]\\s*(.*)$")
    val matchResult = prefixRegex.matchEntire(cleanOption)
    
    if (matchResult != null) {
        val requestedPrefix = matchResult.groupValues[1].lowercase()
        val textBody = matchResult.groupValues[2]
        
        when {
            requestedPrefix.contains("combat") || requestedPrefix.contains("combate") || requestedPrefix.contains("⚔") -> {
                return ParsedOption("COMBATE", Icons.Default.Warning, Color(0xFFFF5252), textBody)
            }
            requestedPrefix.contains("magia") || requestedPrefix.contains("magic") || requestedPrefix.contains("feat") || requestedPrefix.contains("🌌") -> {
                return ParsedOption("MAGIA", Icons.Default.Star, Color(0xFFB388FF), textBody)
            }
            requestedPrefix.contains("investigar") || requestedPrefix.contains("buscar") || requestedPrefix.contains("explorar") || requestedPrefix.contains("🔍") -> {
                return ParsedOption("EXPLORAÇÃO", Icons.Default.Search, Color(0xFF40C4FF), textBody)
            }
            requestedPrefix.contains("falar") || requestedPrefix.contains("dialog") || requestedPrefix.contains("diálogo") || requestedPrefix.contains("conversar") || requestedPrefix.contains("🗣") -> {
                return ParsedOption("DIÁLOGO", Icons.Default.Face, Color(0xFFFFD740), textBody)
            }
            requestedPrefix.contains("item") || requestedPrefix.contains("inventario") || requestedPrefix.contains("inventário") || requestedPrefix.contains("mochila") || requestedPrefix.contains("📦") -> {
                return ParsedOption("INVENTÁRIO", Icons.Default.Build, Color(0xFF69F0AE), textBody)
            }
            else -> {
                return ParsedOption(matchResult.groupValues[1].uppercase(), Icons.Default.Info, Color(0xFFFFD43F), textBody)
            }
        }
    }
    
    // Fallback: Keyword based matching if no explicit prefix is present
    val lower = cleanOption.lowercase()
    return when {
        lower.contains("atacar") || lower.contains("combater") || lower.contains("luta") || lower.contains("golpear") || lower.contains("espada") || lower.contains("confrontar") || lower.contains("desafiar") || lower.contains("frente") -> {
            ParsedOption("COMBATE", Icons.Default.Warning, Color(0xFFFF5252), cleanOption)
        }
        lower.contains("esconjurar") || lower.contains("feitiço") || lower.contains("reza") || lower.contains("magia") || lower.contains("lunar") || lower.contains("gravidade") || lower.contains("mental") || lower.contains("invocar") || lower.contains("canalizar") -> {
            ParsedOption("MAGIA", Icons.Default.Star, Color(0xFFB388FF), cleanOption)
        }
        lower.contains("investigar") || lower.contains("pesquisar") || lower.contains("procurar") || lower.contains("olhar") || lower.contains("verificar") || lower.contains("sondar") || lower.contains("explorar") || lower.contains("inspecionar") || lower.contains("analisar") -> {
            ParsedOption("EXPLORAÇÃO", Icons.Default.Search, Color(0xFF40C4FF), cleanOption)
        }
        lower.contains("falar") || lower.contains("conversar") || lower.contains("barganhar") || lower.contains("perguntar") || lower.contains("interrogar") || lower.contains("dialogar") || lower.contains("responder") || lower.contains("revelar") -> {
            ParsedOption("DIÁLOGO", Icons.Default.Face, Color(0xFFFFD740), cleanOption)
        }
        lower.contains("usar poção") || lower.contains("equipar") || lower.contains("inventário") || lower.contains("item") || lower.contains("beber") || lower.contains("abrir baú") || lower.contains("pegar") -> {
            ParsedOption("INVENTÁRIO", Icons.Default.Build, Color(0xFF69F0AE), cleanOption)
        }
        else -> {
            ParsedOption("SOBREVIVÊNCIA", Icons.Default.Info, Color(0xFFFFD43F), cleanOption)
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD43F),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SUGESTÕES DA MENTE DA IA (DIRECIONAIS ▲ / ▼ DO CONTROLE DE TV):",
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD43F),
                letterSpacing = 1.sp,
                fontSize = 9.sp
            )
        }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, rawOption ->
                val parsed = parseOption(rawOption)
                
                var isFocused by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isFocused) 1.03f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )
                
                val borderColor by animateColorAsState(if (isFocused) parsed.color else Color(0xFF1E172B))
                val backgroundFillColor by animateColorAsState(
                    if (isFocused) parsed.color.copy(alpha = 0.12f) else Color(0xFF0C0913)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scale)
                        .background(backgroundFillColor, RoundedCornerShape(12.dp))
                        .border(if (isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
                        .focusable(true)
                        .onFocusChanged { isFocused = it.isFocused }
                        .clickable { onOptionSelect(parsed.text) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("option_${index}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(parsed.color.copy(alpha = 0.2f), CircleShape)
                                    .border(1.dp, parsed.color.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = parsed.icon,
                                    contentDescription = parsed.category,
                                    tint = parsed.color,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Box(
                                modifier = Modifier
                                    .background(parsed.color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = parsed.category,
                                    color = parsed.color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            Text(
                                text = parsed.text,
                                color = if (isFocused) Color.White else Color(0xFFDFDCE5),
                                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Serif,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isFocused) parsed.color.copy(alpha = 0.25f) else Color(0xFF161220), 
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp, 
                                    if (isFocused) parsed.color.copy(alpha = 0.5f) else Color(0xFF261D38), 
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "[Opção ${index + 1}]",
                                color = if (isFocused) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

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
    val borderColor by animateColorAsState(if (isFocused) Color(0xFF8A6BFF) else Color(0xFF16131F))
    val backgroundFillColor by animateColorAsState(if (isFocused) Color(0xFF221633) else Color(0xFF0F0D15))

    Box(
        modifier = modifier
            .scale(scale)
            .background(backgroundFillColor, RoundedCornerShape(10.dp))
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .focusable(true)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
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

@Composable
fun VersionNotificationPrompt(viewModel: GameViewModel, onDismiss: () -> Unit) {
    var isDismissFocused by remember { mutableStateOf(false) }
    val installedVer by viewModel.installedVersion.collectAsStateWithLifecycle()
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1428)),
        border = BorderStroke(2.dp, Color(0xFFFFD43F)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onDismiss() } // Make entire card clickable for effortless dismissal
            .testTag("version_update_notification_card")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFFD43F).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD43F),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "NOVA VERSÃO INSTALADA! (v$installedVer)",
                        color = Color(0xFFFFD43F),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val descText = if (installedVer == 4.2) {
                        "Aprimoramos o som do microfone com ganho digital de 3.5x e filtros inteligentes do Google NLP para reconhecimento instantâneo! Além disso, as opções rápidas agora aparecem em todos os setores do jogo e as notificações se auto-encerram em 10 segundos."
                    } else {
                        "Build v$installedVer carregada com sucesso do repositório GitHub! Todas as novas diretrizes do Mestre de Jogo foram aplicadas no banco de dados local com sucesso total no sincronismo de dados em tempo real."
                    }
                    Text(
                        text = descText,
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            TVButton(
                onClick = onDismiss,
                modifier = Modifier
                    .onFocusChanged { isDismissFocused = it.isFocused }
                    .border(
                        2.dp,
                        if (isDismissFocused) Color.White else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Text("Entendido", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SettingsAndUpdatesDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val owner by viewModel.githubOwner.collectAsStateWithLifecycle()
    val repo by viewModel.githubRepo.collectAsStateWithLifecycle()
    val token by viewModel.githubToken.collectAsStateWithLifecycle()
    val updateState by viewModel.updateCheckState.collectAsStateWithLifecycle()
    val isApplyingUpdate by viewModel.isApplyingUpdate.collectAsStateWithLifecycle()
    val updateProgress by viewModel.updateProgress.collectAsStateWithLifecycle()
    val updateStatusText by viewModel.updateStatusText.collectAsStateWithLifecycle()
    val installedVersion by viewModel.installedVersion.collectAsStateWithLifecycle()

    var ownerText by remember { mutableStateOf(owner) }
    var repoText by remember { mutableStateOf(repo) }
    var tokenText by remember { mutableStateOf(token) }

    var isOwnerFocused by remember { mutableStateOf(false) }
    var isRepoFocused by remember { mutableStateOf(false) }
    var isTokenFocused by remember { mutableStateOf(false) }
    var isCheckFocused by remember { mutableStateOf(false) }
    var isResetSettingsFocused by remember { mutableStateOf(false) }
    var isCloseFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6060408))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16131C)),
            border = BorderStroke(1.dp, Color(0xFF322842)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.95f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Left side: Config inputs (40% width)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.4f)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFFFFD43F), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Configurações do Jogo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text("Personalize os parâmetros de conexão de dados do WhatIsRPG?", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        // GitHub Owner Field
                        Text("GITHUB OWNER / PROPRIETÁRIO:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8A6BFF))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = ownerText,
                            onValueChange = { 
                                ownerText = it 
                                viewModel.saveGithubSettings(it, repoText, tokenText)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF8A6BFF),
                                unfocusedBorderColor = Color(0xFF2C243B),
                                focusedContainerColor = Color(0xFF1C1826)
                            ),
                            placeholder = { Text("Ex: kelvinhx", color = Color.DarkGray, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isOwnerFocused = it.isFocused }
                                .border(1.dp, if (isOwnerFocused) Color(0xFFFFD43F) else Color.Transparent, RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // GitHub Repository Field
                        Text("REPOSITÓRIO GITHUB:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8A6BFF))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = repoText,
                            onValueChange = { 
                                repoText = it 
                                viewModel.saveGithubSettings(ownerText, it, tokenText)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF8A6BFF),
                                unfocusedBorderColor = Color(0xFF2C243B),
                                focusedContainerColor = Color(0xFF1C1826)
                            ),
                            placeholder = { Text("Ex: Projeto-de-RPG-para-TV-Android", color = Color.DarkGray, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isRepoFocused = it.isFocused }
                                .border(1.dp, if (isRepoFocused) Color(0xFFFFD43F) else Color.Transparent, RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // GitHub Private Token Field (PAT)
                        Text("TOKEN DE ACESSO GITHUB (OPCIONAL):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8A6BFF))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tokenText,
                            onValueChange = { 
                                tokenText = it 
                                viewModel.saveGithubSettings(ownerText, repoText, it)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF8A6BFF),
                                unfocusedBorderColor = Color(0xFF2C243B),
                                focusedContainerColor = Color(0xFF1C1826)
                            ),
                            placeholder = { Text("PAT para obter builds privadas", color = Color.DarkGray, fontSize = 11.sp) },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isTokenFocused = it.isFocused }
                                .border(1.dp, if (isTokenFocused) Color(0xFFFFD43F) else Color.Transparent, RoundedCornerShape(8.dp))
                        )
                    }

                    // Lower Left Actions
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TVButton(
                            onClick = { 
                                ownerText = "kelvinhx"
                                repoText = "Projeto-de-RPG-para-TV-Android"
                                tokenText = ""
                                viewModel.saveGithubSettings("kelvinhx", "Projeto-de-RPG-para-TV-Android", "")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isResetSettingsFocused = it.isFocused }
                                .border(2.dp, if (isResetSettingsFocused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restaurar Padrão", color = Color.LightGray, fontSize = 12.sp)
                        }

                        TVButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isCloseFocused = it.isFocused }
                                .border(2.dp, if (isCloseFocused) Color(0xFFFF4E4E) else Color.Transparent, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fechar Configurações", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Divider line separator
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color(0xFF2D253B))
                )

                // Right side: Update Details panel (60% width)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.6f)
                        .padding(start = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD43F), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Central de Atualizações", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            // Check button
                            if (!isApplyingUpdate) {
                                TVButton(
                                    onClick = { viewModel.checkForUpdates() },
                                    modifier = Modifier
                                        .onFocusChanged { isCheckFocused = it.isFocused }
                                        .border(2.dp, if (isCheckFocused) Color(0xFFFFD43F) else Color.Transparent, RoundedCornerShape(12.dp))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buscar Agora", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        Text("Verifique se há novas builds no repositório público do GitHub", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isApplyingUpdate) {
                            // Render nice updater progress
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFF0F0D14), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        progress = { updateProgress },
                                        color = Color(0xFFFFD43F),
                                        trackColor = Color(0xFF2C243B),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("PROCESSO DE ATUALIZAÇÃO...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = updateStatusText, color = Color(0xFF8A6BFF), fontSize = 11.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .height(4.dp)
                                            .background(Color(0xFF241C30), CircleShape)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(updateProgress)
                                                .fillMaxHeight()
                                                .background(Color(0xFFFFD43F), CircleShape)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Render based on check results
                            when (val state = updateState) {
                                is UpdateCheckState.Idle -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .background(Color(0xFF0F0D14), RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Selecione 'Buscar Agora' com o controle remoto para verificar as últimas notas e builds no repositório GitHub.", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("Versão do aplicativo local: $installedVersion", color = Color(0xFF8A6BFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                is UpdateCheckState.Checking -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .background(Color(0xFF0F0D14), RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = Color(0xFF8A6BFF))
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Conectando com o raw.githubusercontent.com...", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Consultando arquivo de notas de $ownerText/$repoText...", color = Color.Gray, fontSize = 10.sp)
                                        }
                                    }
                                }
                                is UpdateCheckState.Success -> {
                                    val info = state.updateInfo
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .background(Color(0xFF0F0D14), RoundedCornerShape(12.dp))
                                            .padding(16.dp)
                                    ) {
                                        // Status Badge row
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(text = "ÚLTIMA BUILD DISPONÍVEL:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            
                                            // Glowing status pill
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (info.isMoreRecent) Color(0xFFFF5252).copy(alpha = 0.2f) else Color(0xFF4CAF50).copy(alpha = 0.2f),
                                                        CircleShape
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (info.isMoreRecent) "⚠️ ATUALIZAÇÃO DISPONÍVEL" else "✅ ESTÁVEL / ATUALIZADO",
                                                    color = if (info.isMoreRecent) Color(0xFFFF5252) else Color(0xFF4CAF50),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = "${info.buildName} (v${info.appVersion})", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                                Text(text = "Envio: ${info.dateAndHour}", color = Color(0xFF8A6BFF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            
                                            if (info.isMoreRecent) {
                                                var isInstallFocused by remember { mutableStateOf(false) }
                                                TVButton(
                                                    onClick = { viewModel.applyHotUpdate(info.appVersion) },
                                                    modifier = Modifier
                                                        .onFocusChanged { isInstallFocused = it.isFocused }
                                                        .border(2.dp, if (isInstallFocused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Green, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Instalar v${info.appVersion}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(text = "NOTAS COMPILADAS DO REPOSITÓRIO:", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Changes list scroll viewport
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .background(Color(0xFF0C0A0F), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFF1E1826), RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                if (info.changesList.isEmpty()) {
                                                    item {
                                                        Text(text = info.rawNotesSection, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Serif)
                                                    }
                                                } else {
                                                    items(info.changesList) { change ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.Top
                                                        ) {
                                                            Text(text = "✦", color = Color(0xFFFFD43F), fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                                                            Text(text = change, color = Color(0xFFE2DFE5), fontSize = 12.sp, lineHeight = 16.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                is UpdateCheckState.Error -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .background(Color(0xFF0F0D14), RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF4E4E), modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(text = "FALHA NA BUSCA DE ATUALIZAÇÕES", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = state.errorMsg, color = Color(0xFFFF8B8B), fontSize = 11.sp, textAlign = TextAlign.Center)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(text = "Dica: Garanta que o repositório possua um arquivo 'NOTAS_DE_ATUALIZACAO.md' na branch 'main'.", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Lower right footer
                    Text(
                        text = "WhatIsRPG? v$installedVersion • Conectado à rede do GitHub em tempo real",
                        color = Color.DarkGray,
                        fontSize = 10.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateOfferDialog(
    updateInfo: com.example.rpg.GitHubUpdateInfo,
    isApplying: Boolean,
    progress: Float,
    statusText: String,
    onApply: () -> Unit,
    onDefer: () -> Unit
) {
    var isUpdateFocused by remember { mutableStateOf(false) }
    var isDeferFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9050407))
            .clickable(enabled = !isApplying) { /* block background clicks */ }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF130F1A)),
            border = BorderStroke(2.dp, Color(0xFF8A6BFF)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFD43F).copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD43F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NOVA ATUALIZAÇÃO DISPONÍVEL",
                            color = Color(0xFFFFD43F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isApplying) {
                    Text(
                        text = "Instalar a versão ${updateInfo.buildName} (v${updateInfo.appVersion})?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lançado em: ${updateInfo.dateAndHour}",
                        fontSize = 11.sp,
                        color = Color(0xFF8A6BFF),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Changes summary section
                    Text(
                        text = "NOTAS DE ATUALIZAÇÃO RECENTES:",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .background(Color(0xFF0C0912), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF231E2D), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (updateInfo.changesList.isEmpty()) {
                                item {
                                    Text(
                                        text = updateInfo.rawNotesSection,
                                        color = Color(0xFFD4D0D9),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Serif
                                    )
                                }
                            } else {
                                items(updateInfo.changesList) { change ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("✦", color = Color(0xFFFF9E00), fontSize = 11.sp, modifier = Modifier.padding(end = 6.dp))
                                        Text(text = change, color = Color(0xFFE2DFE5), fontSize = 12.sp, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Update now button
                        TVButton(
                            onClick = onApply,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { isUpdateFocused = it.isFocused }
                                .border(
                                    2.dp,
                                    if (isUpdateFocused) Color.White else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Green, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Atualizar Agora", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }

                        // Defer / remind me later button
                        TVButton(
                            onClick = onDefer,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { isDeferFocused = it.isFocused }
                                .border(
                                    2.dp,
                                    if (isDeferFocused) Color(0xFFFF5252) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mais Tarde", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                } else {
                    // Update in progress!
                    Spacer(modifier = Modifier.height(14.dp))
                    CircularProgressIndicator(
                        progress = { progress },
                        color = Color(0xFFFFD43F),
                        trackColor = Color(0xFF2C243B),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "EFETUANDO ATUALIZAÇÃO...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = statusText,
                        color = Color(0xFF8A6BFF),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Linear progress bar %
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(6.dp)
                            .background(Color(0xFF241C30), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(Color(0xFFFFD43F), CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(progress * 100).toInt()}% completo",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
