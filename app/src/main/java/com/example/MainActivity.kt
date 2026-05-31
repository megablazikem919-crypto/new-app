package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.SentHug
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PandaCheerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PandaCheerApp()
            }
        }
    }
}

// Confetti Particle Data model
data class ConfettiParticle(
    var x: Float, // horizontal proportional coordinate (0f .. 1f)
    var y: Float, // vertical proportional coordinate (-0.1f .. 1.1f)
    var color: Color,
    var size: Float,
    var speedY: Float,
    var speedX: Float,
    var emoji: String? = null,
    var alpha: Float = 1.0f,
    var rotation: Float = 0f
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PandaCheerApp(
    viewModel: PandaCheerViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    // Observe State from ViewModel
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val currentQuote by viewModel.currentCheeringQuote.collectAsStateWithLifecycle()
    val sentHugsList by viewModel.sentHugs.collectAsStateWithLifecycle()

    // Dialog state for "Send a Hug" Form
    var showSendHugDialog by remember { mutableStateOf(false) }
    var friendParamName by remember { mutableStateOf("") }
    var friendParamMessage by remember { mutableStateOf("") }
    var friendParamEmoji by remember { mutableStateOf("🐼") }

    // Dialog state for customized name personalization
    var showPersonalizeDialog by remember { mutableStateOf(false) }
    var tempNameInput by remember { mutableStateOf("") }

    // State of active particles for confetti trigger
    val particles = remember { mutableStateListOf<ConfettiParticle>() }

    // Visual State for Panda blowing a kiss with falling loves
    var isKissing by remember { mutableStateOf(false) }
    val pandaScale by animateFloatAsState(
        targetValue = if (isKissing) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "panda_scale"
    )

    // Floating animation offset for Panda image
    val infiniteTransition = rememberInfiniteTransition(label = "panda_float")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "panda_offset"
    )

    // Sparkle badge floating offset
    val badgeOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_offset"
    )

    // Listen to confetti triggers from VM
    LaunchedEffect(Unit) {
        viewModel.confettiTrigger.collect {
            // Spawn 45 custom pastel particles
            val list = List(45) {
                ConfettiParticle(
                    x = kotlin.random.Random.nextInt(10, 91).toFloat() / 100f,
                    y = -0.05f,
                    color = listOf(
                        Color(0xFFFFD1DC), // Soft Pink
                        Color(0xFFACECB5), // Soft Mint
                        Color(0xFFFCD7FF), // Soft Lavender
                        Color(0xFF78555E), // Primary Dark
                        Color(0xFF725477)  // Secondary Lavender
                    ).random(),
                    size = kotlin.random.Random.nextInt(20, 46).toFloat(),
                    speedY = kotlin.random.Random.nextDouble(0.012, 0.026).toFloat(),
                    speedX = kotlin.random.Random.nextInt(-15, 16).toFloat() / 1000f,
                    emoji = if (kotlin.random.Random.nextInt(0, 101) > 40) "❤️" else null,
                    rotation = kotlin.random.Random.nextInt(0, 361).toFloat()
                )
            }
            particles.addAll(list)
        }
    }

    // Interactive Tick update loop for particles
    LaunchedEffect(particles.size) {
        if (particles.isNotEmpty()) {
            while (particles.isNotEmpty()) {
                delay(16) // tick ~60 fps
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.y += p.speedY
                    p.x += p.speedX
                    p.rotation += 2.5f
                    p.alpha -= 0.007f
                    if (p.y > 1.1f || p.alpha <= 0f) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // High-fidelity Floating Header with glassmorphism visual style
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo text
                    Text(
                        text = "PandaCheer",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.clickable {
                            coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                        }
                    )

                    // Navigation Links matching user mockup
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                        }) {
                            Text(
                                "Special",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(onClick = {
                            coroutineScope.launch { lazyListState.animateScrollToItem(3) } // Send hug input field
                        }) {
                            Text(
                                "Hugs",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(onClick = {
                            coroutineScope.launch { lazyListState.animateScrollToItem(1) } // Reasons to smile
                        }) {
                            Text(
                                "Cheer",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TextButton(onClick = {
                            coroutineScope.launch { lazyListState.animateScrollToItem(4) } // Friends logs list
                        }) {
                            Text(
                                "Love",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Top trailing decoration icons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                tempNameInput = if (userName == "cutie ji") "" else userName
                                showPersonalizeDialog = true
                            },
                            modifier = Modifier.testTag("personalize_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mood,
                                contentDescription = "Personalize Name",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.triggerCheer()
                                Toast.makeText(context, "Sending warm panda hugs to your device! 🎉", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Send Love Cheer",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Atmosphere Radial/Gradient Blurs at the background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            radius = 600f
                        )
                    )
            )

            // Main single-screen layout with interactive scroll sections
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
            ) {

                // SECTION 0: HERO CONTAINER
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // A Fresh Hug floating pill badge
                        Button(
                            onClick = { viewModel.triggerCheer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .offset(y = badgeOffset.dp)
                                .testTag("badge_floating_pill"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Awesome sparkle",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "A fresh hug just for you!",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom Hinglish/Personalized Main Header with high graphic weight
                        Text(
                            text = "Get happy $userName,\naapke cheeks acche ni lgte udaas",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 34.sp,
                                lineHeight = 44.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempNameInput = if (userName == "cutie ji") "" else userName
                                    showPersonalizeDialog = true
                                }
                                .testTag("hero_personalized_headline"),
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactive sub-message cycling or showing panda saying
                        Text(
                            text = if (userName == "cutie ji") {
                                "A special message from your panda friend who only wants to see you smile! 🐼✨"
                            } else {
                                "Panda says: $currentQuote 🐾💖"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clickable { viewModel.triggerCheer() }
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Volumetric floating 3D waving panda artwork card
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .offset(y = bounceOffset.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Soft colored background shadow blob
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )

                            // Load web 3D Panda using Coil with dynamic size scaling when blowing glass kisses!
                            AsyncImage(
                                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuC8TfmDn7gx3ESOvZD98TfXPgoUCgSFMp-TR-qRU9KrIrK_KBiGq9agi68ikH-KFzngyLOwZUIWnByE4ImougVc_gPyi_WnpY98JailuhDUo1x7Ws7NthLehF0QyIiTysRjgcXp3ySdIfndMFlsS1ZR7tCnWcvDZGx_BY-rSRG4jYJrR00ScKiObVCtH3RWFZww4568_GgW4wfFoTbRzphVRYOAm3EsM0fUSZhqtBJbugFYNxhRAJ6yD3s97za9IxGUsTtf2GX9BEo",
                                contentDescription = "Cute waving 3D panda friend",
                                modifier = Modifier
                                    .size(220.dp)
                                    .scale(pandaScale)
                                    .clip(CircleShape)
                                    .border(4.dp, Color.White, CircleShape),
                                error = null // Standard default fallback if offline
                            )

                            // Flying kiss interactive speech bubble!
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isKissing,
                                enter = fadeIn(animationSpec = tween(400)) + scaleIn(animationSpec = tween(400)),
                                exit = fadeOut(animationSpec = tween(500)) + scaleOut(animationSpec = tween(500))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset(y = (-65).dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Mwah! 😘💋💕",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Tactile Claymorphic "Cheer Me Up!" Button
                        ClaymorphicButton(
                            onClick = {
                                isKissing = true
                                viewModel.triggerCheer()

                                // Spawn specialized romantic loves/kisses falling from the sky!
                                val customLoves = List(80) {
                                    ConfettiParticle(
                                        x = kotlin.random.Random.nextInt(5, 96).toFloat() / 100f,
                                        y = kotlin.random.Random.nextDouble(-0.35, -0.05).toFloat(),
                                        color = listOf(
                                            Color(0xFFFFB6C1), // Light pink
                                            Color(0xFFFF1493), // Deep pink
                                            Color(0xFFFF69B4), // Hot pink
                                            Color(0xFFFFE4E1), // Misty Rose
                                            Color(0xFFD8BFD8)  // Thistle Lavender
                                        ).random(),
                                        size = kotlin.random.Random.nextInt(26, 60).toFloat(), // larger range of lovely shapes
                                        speedY = kotlin.random.Random.nextDouble(0.008, 0.024).toFloat(),
                                        speedX = kotlin.random.Random.nextInt(-18, 19).toFloat() / 1000f,
                                        emoji = listOf("❤️", "💖", "😘", "💋", "💕", "💘", "🌹", "💝", "🌸").random(),
                                        rotation = kotlin.random.Random.nextInt(0, 361).toFloat()
                                    )
                                }
                                particles.addAll(customLoves)

                                coroutineScope.launch {
                                    delay(2500)
                                    isKissing = false
                                }
                                Toast.makeText(context, "Mwah! 💋 Panda blew you a lovely kiss! 💞", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("cheer_me_up_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Heart Icon",
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "CHEER ME UP!",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 16.sp,
                                    letterSpacing = 0.05.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // SECTION 1: CUTE REASONS TO SMILE
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Cute Reasons to Smile",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Soft pill divider
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(6.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Bento Cards (Scroll list or neat column cards)
                        SmileCard(
                            title = "Radiant Soul",
                            text = "\"You're the brightest part of my day! ☀️\"",
                            icon = Icons.Outlined.WbSunny,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SmileCard(
                            title = "One in a Billion",
                            text = "\"Your smile is more precious than all the bamboo in the world. 🎋❤️\"",
                            icon = Icons.Filled.AutoAwesome,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SmileCard(
                            title = "Cosmic Sparkle",
                            text = "\"Even the stars are jealous of your sparkle. ✨\"",
                            icon = Icons.Outlined.Star,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            iconTint = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SmileCard(
                            title = "Peaceful Mind",
                            text = "\"Take a deep breath, everything is going to be okay, cutie! 🌈\"",
                            icon = Icons.Outlined.Cloud,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SmileCard(
                            title = "Endless Support",
                            text = "\"A big panda hug is waiting for you whenever you need it. 🤗\"",
                            icon = Icons.Outlined.EmojiEmotions,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // SECTION 2: CTA NEED MORE PANDAS
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("cta_card_section"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                        ),
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Need more pandas?",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Sometimes we all need a little reminder that we're doing great. Keep being your amazing self!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Interactive Options
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                            ) {
                                // Send Custom Hug trigger
                                Button(
                                    onClick = {
                                        friendParamName = ""
                                        friendParamMessage = ""
                                        showSendHugDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                                    modifier = Modifier.testTag("cta_send_hug_pill")
                                ) {
                                    Text("Send a Hug", fontWeight = FontWeight.Bold)
                                }

                                // Copy comforting invite message using Android Clipboard manager
                                Button(
                                    onClick = {
                                        val inviteMsg = "Hey sweetie, I'm thinking of you and sending custom virtual panda hugs to brighten your day! Open this: https://ai.studio/build 🐼💖"
                                        clipboardManager.setText(AnnotatedString(inviteMsg))
                                        Toast.makeText(context, "Cheer message copied! Go share the love! 💌", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier.testTag("cta_tell_friend_pill")
                                ) {
                                    Text("Tell a Friend", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // SECTION 3: SEND A HUG BOX (ROOM PERSISTENCE CONTROLLER)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("database_sender_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Spread Love & Support",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Record custom hugs you send to loved ones. They'll be preserved in your local library!",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Local inputs for fast inline record insertion
                            var localFriendName by remember { mutableStateOf("") }
                            var localFriendMsg by remember { mutableStateOf("") }
                            var localEmoji by remember { mutableStateOf("🐼") }

                            OutlinedTextField(
                                value = localFriendName,
                                onValueChange = { localFriendName = it },
                                label = { Text("Friend's Name") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("friend_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = localFriendMsg,
                                onValueChange = { localFriendMsg = it },
                                label = { Text("Sweet Cheering Message") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("friend_message_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Emoji pickers
                            Text(
                                text = "Select Panda Avatar:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("🐼", "🤗", "💖", "🎋", "🌟").forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .border(
                                                width = if (localEmoji == item) 3.dp else 1.dp,
                                                color = if (localEmoji == item) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clip(CircleShape)
                                            .clickable { localEmoji = item }
                                            .background(
                                                if (localEmoji == item) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(item, fontSize = 20.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            ClaymorphicButton(
                                onClick = {
                                    if (localFriendName.isNotBlank()) {
                                        viewModel.sendHugToFriend(localFriendName, localFriendMsg, localEmoji)
                                        localFriendName = ""
                                        localFriendMsg = ""
                                        Toast.makeText(context, "Yay! Hug sent successfully! 🎋🐾", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Please enter a friend's name! ✨", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Send",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SEND COZY HUG", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // SECTION 4: HERO LOG OF SENT HUGS (ROOM REACTIVE CORNER)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hugs Shared Library (${sentHugsList.size})",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            if (sentHugsList.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.clearHugsLog()
                                        Toast.makeText(context, "All logs cleared! Fresh cozy canvas initialized! 🍃", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClearAll,
                                        contentDescription = "Clear Logs",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (sentHugsList.isEmpty()) {
                            // Friendly customized Empty State instruction guidelines
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🐼", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "No hugs logged yet!",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Fill out the Send Hug form above to share your very first smile!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            // Horizontal gallery flow of customized hugs
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                items(sentHugsList) { hug ->
                                    SentHugCard(hug = hug)
                                }
                            }
                        }
                    }
                }

                // SECTION 5: FOOLPROOF COMPREHENSIVE FOOTER
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "PandaCheer",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Privacy",
                                modifier = Modifier.clickable {
                                    Toast.makeText(context, "Your data stays locally inside your device. Private, safe and cozy! 🔒", Toast.LENGTH_SHORT).show()
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("|", color = Color.LightGray)
                            Text(
                                "Help",
                                modifier = Modifier.clickable {
                                    Toast.makeText(context, "Need comfort? Tap the giant 'Cheer Me Up' button! 🎋", Toast.LENGTH_SHORT).show()
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("|", color = Color.LightGray)
                            Text(
                                "Send Love",
                                modifier = Modifier.clickable {
                                    viewModel.triggerCheer()
                                    Toast.makeText(context, "Lots of sparkles sent to you! ✨❤️", Toast.LENGTH_SHORT).show()
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Made with love and panda hugs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // FULL-SCREEN CONFETTI LAYER OVERLAY
            if (particles.isNotEmpty()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {} // Transparent overlay block clicks during explosion
                ) {
                    particles.forEach { p ->
                        val positionX = p.x * maxWidth.value
                        val positionY = p.y * maxHeight.value
                        Box(
                            modifier = Modifier
                                .offset(x = positionX.dp, y = positionY.dp)
                                .rotate(p.rotation)
                                .alpha(p.alpha)
                                .size((p.size / 2).dp)
                        ) {
                            if (p.emoji != null) {
                                Text(p.emoji!!, fontSize = (p.size / 2).sp)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(p.color, shape = CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG 1: PERSONALIZE GREETING (NAME SWAPPER)
    if (showPersonalizeDialog) {
        Dialog(onDismissRequest = { showPersonalizeDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Who are we cheering?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Swap 'cutie ji' for your customized name! Perfect to customize links or share logs with sweethearts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tempNameInput,
                        onValueChange = { tempNameInput = it },
                        placeholder = { Text("e.g. cutie, sweetheart, mom") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("personalize_dialog_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { showPersonalizeDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.labelLarge)
                        }

                        Button(
                            onClick = {
                                viewModel.setUserName(tempNameInput)
                                showPersonalizeDialog = false
                                viewModel.triggerCheer()
                                Toast.makeText(context, "Yay! Customized name initialized! 🎉", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("personalize_dialog_submit")
                        ) {
                            Text("Chevron up!", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // DIALOG 2: COMPREHENSIVE FLOATING FORM "SEND A HUG"
    if (showSendHugDialog) {
        Dialog(onDismissRequest = { showSendHugDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Custom Cheering Card",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Write a direct note to insert a happy card into your gallery ledger.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = friendParamName,
                        onValueChange = { friendParamName = it },
                        label = { Text("To (Friend's name)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_friend_name"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = friendParamMessage,
                        onValueChange = { friendParamMessage = it },
                        label = { Text("Cozy note message") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_friend_message"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("🐼", "🤗", "💖", "🎋").forEach { item ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(
                                        width = if (friendParamEmoji == item) 3.dp else 1.dp,
                                        color = if (friendParamEmoji == item) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clip(CircleShape)
                                    .clickable { friendParamEmoji = item }
                                    .background(
                                        if (friendParamEmoji == item) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item, fontSize = 18.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { showSendHugDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }

                        Button(
                            onClick = {
                                viewModel.sendHugToFriend(friendParamName, friendParamMessage, friendParamEmoji)
                                showSendHugDialog = false
                                Toast.makeText(context, "Hugging stream generated! 🌊🍀", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("dialog_submit_button")
                        ) {
                            Text("Dispatch!", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Sparkle Card Component modeling Bento Boxes in Mock-ups
@Composable
fun SmileCard(
    title: String,
    text: String,
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = iconTint.copy(alpha = 0.35f)
            ),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(containerColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "$title Icon",
                    tint = iconTint,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// Sent Hug horizontal card component representing shared historical entries
@Composable
fun SentHugCard(
    hug: SentHug,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(220.dp)
            .shadow(6.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(hug.pandaEmoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To: ${hug.friendName}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = hug.hugMessage,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                modifier = Modifier.height(52.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Human readable short relative time marker
            Text(
                text = "Panda Hug Shared",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Bouncy interaction helper for tactile button pressing
@Composable
fun ClaymorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    shadowColor: Color = Color(0xFF5E3E47),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonOffsetY by animateDpAsState(
        targetValue = if (isPressed) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "press_offset"
    )

    Box(
        modifier = modifier
            .padding(bottom = 6.dp)
    ) {
        // Physical tactile base shadow layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 6.dp)
                .background(shadowColor, shape = CircleShape)
        )

        // Main action surface layer translating downward on press
        Box(
            modifier = Modifier
                .offset(y = buttonOffsetY)
                .background(containerColor, shape = CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                )
                .padding(horizontal = 32.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}
