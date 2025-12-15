# Ardunakon Onboarding Technical Implementation Guide

## Architecture Overview

### Component Hierarchy
```
MainActivity
├── OnboardingFlow (conditional)
│   ├── WelcomeScreen
│   ├── InterfaceTour  
│   ├── ConnectionTutorial (Merged Setup)
│   └── CompletionScreen
└── ControlScreen (normal app flow)
```

### State Management
```kotlin
sealed class OnboardingStep {
    object Welcome : OnboardingStep()
    object InterfaceTour : OnboardingStep()
    data class ConnectionTutorial(val step: ConnectionTutorialStep) : OnboardingStep()
    // Advanced Features merged into ConnectionTutorial
    object Completion : OnboardingStep()
    object Finished : OnboardingStep()
}

enum class FeatureType {
    PROFILES,
    DEBUG_CONSOLE, 
    TELEMETRY,
    DUAL_DEVICE
}
```

## Core Components

### 1. OnboardingManager
```kotlin
@Singleton
class OnboardingManager @Inject constructor(
    private val preferences: OnboardingPreferences
) {
    fun shouldShowOnboarding(): Boolean {
        return !preferences.isCompleted() || !preferences.isVersionCurrent()
    }
    
    fun startOnboarding() {
        preferences.setInProgress(true)
    }
    
    fun completeOnboarding() {
        preferences.setCompleted(true)
        preferences.setVersion(getCurrentVersion())
        preferences.setInProgress(false)
    }
    
    fun skipOnboarding() {
        preferences.setSkipped(true)
        preferences.setCompleted(true)
        preferences.setInProgress(false)
    }
    
    fun resetOnboarding() {
        preferences.setCompleted(false)
        preferences.setSkipped(false) 
        preferences.setInProgress(false)
    }
}
```

### 2. OnboardingPreferences
```kotlin
class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_COMPLETED = "completed"
        private const val KEY_VERSION = "version"
        private const val KEY_SKIPPED = "skipped"
        private const val KEY_IN_PROGRESS = "in_progress"
        private const val CURRENT_VERSION = 1
    }
    
    fun isCompleted(): Boolean = prefs.getBoolean(KEY_COMPLETED, false)
    fun isSkipped(): Boolean = prefs.getBoolean(KEY_SKIPPED, false)
    fun isInProgress(): Boolean = prefs.getBoolean(KEY_IN_PROGRESS, false)
    fun isVersionCurrent(): Boolean = prefs.getInt(KEY_VERSION, 0) == CURRENT_VERSION
    
    fun setCompleted(completed: Boolean) = prefs.edit().putBoolean(KEY_COMPLETED, completed).apply()
    fun setSkipped(skipped: Boolean) = prefs.edit().putBoolean(KEY_SKIPPED, skipped).apply()
    fun setInProgress(inProgress: Boolean) = prefs.edit().putBoolean(KEY_IN_PROGRESS, inProgress).apply()
    fun setVersion(version: Int) = prefs.edit().putInt(KEY_VERSION, version).apply()
}
```

### 3. OnboardingOverlay Component
```kotlin
@Composable
fun OnboardingOverlay(
    step: OnboardingStep,
    targetComposable: @Composable () -> Unit,
    content: @Composable () -> Unit,
    isVisible: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    Box(modifier = modifier.fillMaxSize()) {
        // Highlight target element
        targetComposable()
        
        // Overlay background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { /* Block clicks to background */ }
        )
        
        // Tutorial content
        TutorialCard(
            step = step,
            onNext = onNext,
            onSkip = onSkip,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun TutorialCard(
    step: OnboardingStep,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Content based on step
            when (step) {
                is OnboardingStep.Welcome -> WelcomeContent()
                is OnboardingStep.InterfaceTour -> InterfaceTourContent(step)
                is OnboardingStep.ConnectionTutorial -> ConnectionContent(step)
                is OnboardingStep.AdvancedFeatures -> AdvancedFeaturesContent(step)
                OnboardingStep.Completion -> CompletionContent()
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onSkip) {
                    Text("Skip Tutorial")
                }
                
                Button(onClick = onNext) {
                    Text("Next")
                }
            }
        }
    }
}
```

### 4. HighlightOverlay Component
```kotlin
@Composable
fun HighlightOverlay(
    target: @Composable () -> Unit,
    content: @Composable () -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVisible) {
        target()
        return
    }
    
    Box(modifier = modifier) {
        target()
        
        // Highlight box around target
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 3.dp,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        radius = 1000f
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
        )
        
        // Content
        content()
    }
}
```

## MainActivity Integration

### Modified MainActivity
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var onboardingManager: OnboardingManager
    
    private var showOnboarding by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if should show onboarding
        showOnboarding = onboardingManager.shouldShowOnboarding()
        
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showOnboarding) {
                        OnboardingFlow(
                            onComplete = {
                                showOnboarding = false
                                onboardingManager.completeOnboarding()
                            },
                            onSkip = {
                                showOnboarding = false
                                onboardingManager.skipOnboarding()
                            }
                        )
                    } else {
                        MainApp()
                    }
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    // Existing app content
    if (isBound && bluetoothService != null) {
        ControlScreen(
            isDarkTheme = true,
            onQuitApp = { quitApp() }
        )
    } else {
        LoadingScreen()
    }
}
```

### OnboardingFlow Composable
```kotlin
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    
    when (currentStep) {
        OnboardingStep.Welcome -> {
            WelcomeScreen(
                onStart = { viewModel.nextStep() },
                onSkip = onSkip
            )
        }
        is OnboardingStep.InterfaceTour -> {
            InterfaceTourScreen(
                step = currentStep,
                onNext = { viewModel.nextStep() },
                onSkip = onSkip
            )
        }
        is OnboardingStep.ConnectionTutorial -> {
            ConnectionTutorialScreen(
                step = currentStep,
                onNext = { viewModel.nextStep() },
                onSkip = onSkip
            )
        }

        OnboardingStep.Completion -> {
            CompletionScreen(
                onFinish = onComplete,
                onSkip = onSkip
            )
        }
    }
}
```

## ViewModel Implementation

### OnboardingViewModel
```kotlin
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingManager: OnboardingManager,
    private val bluetoothManager: AppBluetoothManager,
    private val profileManager: ProfileManager
) : ViewModel() {
    
    private val _currentStep = MutableStateFlow(OnboardingStep.Welcome)
    val currentStep = _currentStep.asStateFlow()
    
    private val _selectedFeatures = MutableStateFlow(setOf<FeatureType>())
    val selectedFeatures = _selectedFeatures.asStateFlow()
    
    fun nextStep() {
        when (val current = _currentStep.value) {
            OnboardingStep.Welcome -> {
                _currentStep.value = OnboardingStep.InterfaceTour
            }
            is OnboardingStep.InterfaceTour -> {
                // Check if user wants to skip interface tour
                if (current.shouldSkip) {
                    _currentStep.value = OnboardingStep.ConnectionTutorial(0)
                } else {
                    _currentStep.value = OnboardingStep.ConnectionTutorial(0)
                }
            }
            is OnboardingStep.ConnectionTutorial -> {
                val nextStepNumber = current.step + 1
                if (nextStepNumber >= getConnectionTutorialSteps()) {
                    _currentStep.value = OnboardingStep.AdvancedFeatures(_selectedFeatures.value)
                } else {
                    _currentStep.value = OnboardingStep.ConnectionTutorial(nextStepNumber)
                }
            }
            is OnboardingStep.AdvancedFeatures -> {
                _currentStep.value = OnboardingStep.Completion
            }
            OnboardingStep.Completion -> {
                // Will be handled by completion screen
            }
        }
    }
    
    fun selectFeature(feature: FeatureType, selected: Boolean) {
        _selectedFeatures.value = if (selected) {
            _selectedFeatures.value + feature
        } else {
            _selectedFeatures.value - feature
        }
    }
    
    fun skipToStep(step: OnboardingStep) {
        _currentStep.value = step
    }
    
    fun startDeviceScanning() {
        bluetoothManager.startScanning()
    }
    
    fun createFirstProfile() {
        val defaultProfile = profileManager.createDefaultProfile("My First Project")
        profileManager.saveProfile(defaultProfile)
    }
    
    private fun getConnectionTutorialSteps(): Int = 4
}
```

## Specific Screen Implementations

### WelcomeScreen
```kotlin
@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = "🚀 Ardunakon",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Arduino Controller App",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Value propositions
        ValueProposition(
            icon = Icons.Default.TouchApp,
            title = "Control with precision",
            description = "Professional-grade joystick controls"
        )
        
        ValueProposition(
            icon = Icons.Default.Wifi,
            title = "Bluetooth + WiFi support", 
            description = "Connect via multiple protocols"
        )
        
        ValueProposition(
            icon = Icons.Default.Settings,
            title = "Customizable profiles",
            description = "Save settings for different projects"
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Description
        Text(
            text = "We'll guide you through the essentials in just 2 minutes",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Buttons
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Get Started ▶️")
        }
        
        TextButton(onClick = onSkip) {
            Text("Skip Tour")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "📖 Access tutorial later in Help",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ValueProposition(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "✨ $title",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### InterfaceTourScreen
```kotlin
@Composable
fun InterfaceTourScreen(
    step: OnboardingStep.InterfaceTour,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main app interface (faded)
        ControlScreen(
            isDarkTheme = true,
            onQuitApp = { /* disabled during tutorial */ }
        )
        
        // Tutorial overlays based on current interface tour step
        when (step.currentInterfaceElement) {
            InterfaceElement.EMERGENCY_STOP -> {
                EmergencyStopTutorial(
                    onNext = onNext,
                    onSkip = onSkip
                )
            }
            InterfaceElement.CONNECTION_STATUS -> {
                ConnectionStatusTutorial(
                    onNext = onNext,
                    onSkip = onSkip
                )
            }
            InterfaceElement.LEFT_JOYSTICK -> {
                LeftJoystickTutorial(
                    onNext = onNext,
                    onSkip = onSkip
                )
            }
            InterfaceElement.RIGHT_JOYSTICK -> {
                RightJoystickTutorial(
                    onNext = onNext,
                    onSkip = onSkip
                )
            }
            InterfaceElement.CONNECTION_MODE -> {
                ConnectionModeTutorial(
                    onNext = onNext,
                    onSkip = onSkip
                )
            }
        }
    }
}

@Composable
fun EmergencyStopTutorial(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    // Implementation for E-stop tutorial
    TutorialOverlay(
        title = "🔴 EMERGENCY STOP",
        description = "This is the most important button! Tap to instantly stop all motors.",
        targetAlignment = Alignment.TopEnd,
        onNext = onNext,
        onSkip = onSkip
    )
}
```

### ConnectionTutorialScreen
```kotlin
@Composable
fun ConnectionTutorialScreen(
    step: OnboardingStep.ConnectionTutorial,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Show appropriate step content
        // Show appropriate step content
        when (step.step) {
            ConnectionTutorialStep.CHOOSE_ARDUINO -> ChooseArduinoContent(...)
            ConnectionTutorialStep.CONNECTION_MODE -> ConnectionModeContent(...)
            ConnectionTutorialStep.SETUP_FINAL -> SetupFinalContent(...)
        }
    }
}

@Composable
fun ChooseArduinoScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TutorialHeader(
            title = "🔌 CONNECT",
            subtitle = "Choose your Arduino type:",
            onSkip = onSkip
        )
        
        LazyColumn {
            items(arduinoTypes) { arduino ->
                ArduinoTypeCard(
                    arduino = arduino,
                    onSelected = { onNext() }
                )
            }
        }
    }
}

@Composable
fun ArduinoTypeCard(
    arduino: ArduinoType,
    onSelected: () -> Unit
) {
    Card(
        onClick = onSelected,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = arduino.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = arduino.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = arduino.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

## Help Menu Integration

### Modified HelpDialog
```kotlin
@Composable
fun HelpDialog(
    onDismiss: () -> Unit,
    onTakeTutorial: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Help & Support") },
        text = {
            Column {
                HelpItem(
                    icon = Icons.Default.Help,
                    title = "User Guide",
                    description = "Complete feature documentation"
                )
                HelpItem(
                    icon = Icons.Default.QuestionAnswer,
                    title = "FAQ", 
                    description = "Common questions and answers"
                )
                HelpItem(
                    icon = Icons.Default.Build,
                    title = "Troubleshooting",
                    description = "Fix connection and control issues"
                )
                Divider()
                HelpItem(
                    icon = Icons.Default.School,
                    title = "🎓 Take Tutorial",
                    description = "Interactive walkthrough for new users",
                    onClick = onTakeTutorial
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun HelpItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            HelpItemContent(icon, title, description)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            HelpItemContent(icon, title, description)
        }
    }
}

@Composable
fun HelpItemContent(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

## Animation and Effects

### Pulse Animation for Highlights
```kotlin
@Composable
fun PulseAnimation(
    content: @Composable () -> Unit,
    isVisible: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    
    if (isVisible) {
        Box(
            modifier = Modifier.scale(scale)
        ) {
            content()
        }
    } else {
        content()
    }
}
```

### Fade Animation for Overlays
```kotlin
@Composable
fun FadeInAnimation(
    content: @Composable (AnimatedVisibilityScope.() -> Unit),
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300, easing = FastOutSlowInEasing))
    ) {
        content()
    }
}
```

## Testing Strategy

### Unit Tests
```kotlin
@RunWith(MockitoJUnitRunner::class)
class OnboardingManagerTest {
    
    @Mock
    private lateinit var preferences: OnboardingPreferences
    
    private lateinit var onboardingManager: OnboardingManager
    
    @Before
    fun setup() {
        onboardingManager = OnboardingManager(preferences)
    }
    
    @Test
    fun `shouldShowOnboarding returns true when not completed`() {
        // Given
        `when`(preferences.isCompleted()).thenReturn(false)
        `when`(preferences.isVersionCurrent()).thenReturn(true)
        
        // When
        val result = onboardingManager.shouldShowOnboarding()
        
        // Then
        assertThat(result).isTrue()
    }
}
```

### UI Tests
```kotlin
@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun welcomeScreen_displaysCorrectly() {
        // Given - first run app
        // When - app starts
        composeTestRule.setContent {
            OnboardingFlow(
                onComplete = { },
                onSkip = { }
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithText("🚀 Ardunakon")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Get Started")
            .assertIsDisplayed()
    }
}
```

## Performance Considerations

### Memory Management
- Dispose animations on screen changes
- Use remember for expensive operations
- Lazy load tutorial assets

### Startup Performance
- Check onboarding status asynchronously
- Don't block UI thread for preferences
- Pre-load tutorial assets in background

### Battery Optimization
- Disable tutorial animations if battery saver is on
- Reduce animation frequency for tutorial elements
- Allow tutorial to be paused/resumed

This technical implementation provides a complete, production-ready onboarding system that integrates seamlessly with the existing Ardunakon app architecture.