package com.example.nothingplants.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nothingplants.ui.PlantViewModel
import com.example.nothingplants.ui.theme.NothingFontFamily
import com.example.nothingplants.ui.theme.NothingRed
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

private enum class SettingsMenu {
    MAIN, PERMISSIONS, CLOUD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PlantViewModel,
    onNavigateToVerification: () -> Unit,
    onBack: () -> Unit
) {
    val currentApiKey by viewModel.currentApiKey.collectAsStateWithLifecycle()
    var apiKey by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    val currentApiKey2 by viewModel.currentApiKey2.collectAsStateWithLifecycle()
    var apiKey2 by remember(currentApiKey2) { mutableStateOf(currentApiKey2) }
    var showApiKey by remember { mutableStateOf(false) }
    var showApiKey2 by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var savedMessage by remember { mutableStateOf(false) }
    var currentMenu by remember { mutableStateOf(SettingsMenu.MAIN) }

    // Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                coroutineScope.launch { 
                    snackbarHostState.showSnackbar("Accesso Google effettuato")
                    viewModel.refreshCloudSummary(account)
                }
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Errore accesso Google: ${e.message}") }
        }
    }

    BackHandler(enabled = currentMenu != SettingsMenu.MAIN) {
        currentMenu = SettingsMenu.MAIN
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (currentMenu) {
                            SettingsMenu.MAIN -> "Impostazioni"
                            SettingsMenu.PERMISSIONS -> "Autorizzazioni"
                            SettingsMenu.CLOUD -> "Gestione Backup"
                        }, 
                        fontFamily = NothingFontFamily,
                        fontSize = 34.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentMenu == SettingsMenu.MAIN) {
                            onBack()
                        } else {
                            currentMenu = SettingsMenu.MAIN
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (currentMenu) {
            SettingsMenu.MAIN -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Gemini API Key",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = NothingFontFamily,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Text(
                        "Inserisci qui la tua chiave API per permettere l'analisi fotografica delle piante.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key Primaria", fontFamily = NothingFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (showApiKey) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val description = if (showApiKey) "Nascondi chiave API" else "Mostra chiave API"
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(imageVector = image, contentDescription = description, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            focusedLabelColor = NothingRed,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    OutlinedTextField(
                        value = apiKey2,
                        onValueChange = { apiKey2 = it },
                        label = { Text("API Key Secondaria (opzionale)", fontFamily = NothingFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showApiKey2) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (showApiKey2) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val description = if (showApiKey2) "Nascondi chiave API" else "Mostra chiave API"
                            IconButton(onClick = { showApiKey2 = !showApiKey2 }) {
                                Icon(imageVector = image, contentDescription = description, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            focusedLabelColor = NothingRed,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    Button(
                        onClick = {
                            viewModel.saveApiKey(apiKey)
                            viewModel.saveApiKey2(apiKey2)
                            savedMessage = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(2000)
                                savedMessage = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NothingRed)
                    ) {
                        Text(
                            if (savedMessage) "SALVATO!" else "SALVA",
                            fontFamily = NothingFontFamily,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        "LOCALIZZAZIONE E CLIMA",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = NothingFontFamily,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    
                    val locationEnabled by viewModel.locationEnabled.collectAsStateWithLifecycle()
                    val locationCity by viewModel.locationCity.collectAsStateWithLifecycle()
                    val locationLoadingState by viewModel.locationLoadingState.collectAsStateWithLifecycle()
                    val context = androidx.compose.ui.platform.LocalContext.current

                    val locationPermissionsLauncher = rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
                        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                        if (coarseGranted || fineGranted) {
                            viewModel.setLocationEnabled(true)
                            viewModel.refreshLocation(context)
                        } else {
                            viewModel.setLocationEnabled(false)
                        }
                    }

                    LaunchedEffect(locationLoadingState) {
                        if (locationLoadingState is PlantViewModel.LocationLoadingState.Success) {
                            snackbarHostState.showSnackbar("Posizione aggiornata con successo")
                            viewModel.resetLocationLoadingState()
                        } else if (locationLoadingState is PlantViewModel.LocationLoadingState.Error) {
                            val msg = (locationLoadingState as PlantViewModel.LocationLoadingState.Error).message
                            snackbarHostState.showSnackbar("Errore: $msg")
                            viewModel.resetLocationLoadingState()
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rileva posizione automaticamente",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = NothingFontFamily,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (locationLoadingState is PlantViewModel.LocationLoadingState.Loading) {
                                        "Rilevamento in corso..."
                                    } else if (locationEnabled && locationCity.isNotBlank()) {
                                        "Rilevato: $locationCity"
                                    } else {
                                        "Usa il GPS e la rete per identificare il clima locale"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (locationEnabled && locationLoadingState !is PlantViewModel.LocationLoadingState.Loading) {
                                    IconButton(
                                        onClick = { viewModel.refreshLocation(context) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Aggiorna posizione",
                                            tint = NothingRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                
                                NothingSwitch(
                                    checked = locationEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                                                context, 
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                            
                                            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                                                context, 
                                                android.Manifest.permission.ACCESS_FINE_LOCATION
                                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                            
                                            if (hasCoarse || hasFine) {
                                                viewModel.setLocationEnabled(true)
                                                viewModel.refreshLocation(context)
                                            } else {
                                                locationPermissionsLauncher.launch(
                                                    arrayOf(
                                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            }
                                        } else {
                                            viewModel.setLocationEnabled(false)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        "INFORMAZIONI E SISTEMA",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = NothingFontFamily,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentMenu = SettingsMenu.PERMISSIONS },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Autorizzazioni",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = NothingFontFamily,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Stato dei permessi dell'app",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentMenu = SettingsMenu.CLOUD },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gestione Backup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = NothingFontFamily,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Backup e ripristino via Google Drive",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToVerification() },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Verifica Promemoria",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = NothingFontFamily,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Confronta e allinea i promemoria con i report di cura",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            SettingsMenu.PERMISSIONS -> {
                PermissionsMenuContent(padding = padding)
            }
            SettingsMenu.CLOUD -> {
                CloudSyncMenuContent(
                    viewModel = viewModel,
                    googleSignInLauncher = googleSignInLauncher,
                    snackbarHostState = snackbarHostState,
                    padding = padding
                )
            }
        }
    }
}

@Composable
private fun CloudSyncMenuContent(
    viewModel: PlantViewModel,
    googleSignInLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>,
    snackbarHostState: SnackbarHostState,
    padding: PaddingValues
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var account by remember { mutableStateOf(com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)) }
    val cloudSummary by viewModel.cloudSyncSummary.collectAsStateWithLifecycle()
    val autoBackup by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
    val plants by viewModel.allPlants.collectAsStateWithLifecycle()
    
    var isOperating by remember { mutableStateOf(false) }

    LaunchedEffect(account) {
        account?.let {
            viewModel.refreshCloudSummary(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Google Drive Backup",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = NothingFontFamily,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Esegui il backup delle tue piante, foto del diario, annaffiature e concimazioni sul tuo spazio personale Google Drive (AppData).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        // Login / Logout Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (account == null) {
                            googleSignInLauncher.launch(viewModel.driveService.getGoogleSignInClient().signInIntent)
                        } else {
                            viewModel.driveService.getGoogleSignInClient().signOut().addOnCompleteListener {
                                account = null
                            }
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (account == null) "Accedi con Google" else "Disconnetti",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = NothingFontFamily,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (account == null) "Collega il tuo account per sincronizzare" else "Loggato come: ${account?.email}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        if (account != null) {
            // Auto Backup Toggle Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Backup automatico",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = NothingFontFamily,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Esegui il backup giornaliero in background (solo tramite Wi-Fi)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    NothingSwitch(
                        checked = autoBackup,
                        onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
                    )
                }
            }

            // Sync Comparison Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STATO SINCRONIZZAZIONE",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = NothingFontFamily,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Piante locali:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${plants.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = NothingFontFamily,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Piante nel cloud:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${cloudSummary?.plantCount ?: 0}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = NothingFontFamily,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Foto nel cloud:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${cloudSummary?.photoCount ?: 0}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = NothingFontFamily,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    cloudSummary?.lastSyncTimestamp?.takeIf { it > 0 }?.let { lastSync ->
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(12.dp))
                        val date = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.ITALIAN).format(java.util.Date(lastSync))
                        Text(
                            text = "ULTIMO BACKUP: $date".uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = NothingFontFamily,
                            color = NothingRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isOperating) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NothingRed)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            isOperating = true
                            viewModel.uploadToDrive(account!!) { res ->
                                isOperating = false
                                coroutineScope.launch {
                                    if (res.isSuccess) {
                                        snackbarHostState.showSnackbar("Backup completato con successo")
                                        viewModel.refreshCloudSummary(account!!)
                                    } else {
                                        snackbarHostState.showSnackbar("Errore durante il backup: ${res.exceptionOrNull()?.message}")
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "CARICA",
                            fontFamily = NothingFontFamily,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            isOperating = true
                            viewModel.downloadFromDrive(account!!) { res ->
                                isOperating = false
                                coroutineScope.launch {
                                    if (res.isSuccess) {
                                        snackbarHostState.showSnackbar("Ripristino completato con successo")
                                    } else {
                                        snackbarHostState.showSnackbar("Errore durante il ripristino: ${res.exceptionOrNull()?.message}")
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "SCARICA",
                            fontFamily = NothingFontFamily,
                            color = MaterialTheme.colorScheme.background
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsMenuContent(padding: PaddingValues) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    var isNotificationGranted by remember { mutableStateOf(false) }
    var isLocationGranted by remember { mutableStateOf(false) }

    fun checkPermissions() {
        isNotificationGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        isLocationGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val openAppSettings = {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PermissionItemCard(
            title = "Notifiche",
            description = "Utilizzato per mostrare avvisi di scadenza e promemoria delle innaffiature o concimazioni.",
            status = if (isNotificationGranted) "Autorizzato" else "Non autorizzato",
            onClick = openAppSettings
        )

        PermissionItemCard(
            title = "Posizione",
            description = "Utilizzato per identificare geograficamente la posizione del dispositivo e fornire raccomandazioni meteorologiche e microclimatiche adeguate.",
            status = if (isLocationGranted) "Autorizzato" else "Non autorizzato",
            onClick = openAppSettings
        )

        PermissionItemCard(
            title = "Ripristino all'avvio",
            description = "Consente di riprogrammare i promemoria attivi in background dopo il riavvio del telefono.",
            status = "Autorizzato",
            onClick = openAppSettings
        )
    }
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    status: String,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = NothingFontFamily,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = NothingFontFamily,
                        color = if (status == "Autorizzato") MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else NothingRed
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun NothingSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    val trackColorChecked = if (isDark) Color.White else Color.Black
    val trackColorUnchecked = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
    
    val thumbColorChecked = if (isDark) Color.Black else Color.White
    val thumbColorUnchecked = if (isDark) Color.Black else Color.White

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = thumbColorChecked,
            checkedTrackColor = trackColorChecked,
            checkedBorderColor = Color.Transparent,
            checkedIconColor = Color.Transparent,
            uncheckedThumbColor = thumbColorUnchecked,
            uncheckedTrackColor = trackColorUnchecked,
            uncheckedBorderColor = Color.Transparent,
            uncheckedIconColor = Color.Transparent
        )
    )
}
