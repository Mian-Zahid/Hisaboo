package com.mianzahid.expenseentry.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.mianzahid.expenseentry.ui.components.*
import com.mianzahid.expenseentry.ui.theme.*
import com.mianzahid.expenseentry.viewmodel.HisabooViewModel

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
fun FamilyScreen(
    viewModel: HisabooViewModel,
    onBack: () -> Unit
) {
    var memberName by remember { mutableStateOf("") }
    var memberPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = "Family", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GlassCard(cornerRadius = 22.dp) {
                    Text(
                        text = "Invite code",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = viewModel.inviteCode
                            ?: "No code yet. Generate to invite family members.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (viewModel.inviteCode != null) EmeraldPrimary else TextSecondary,
                        fontWeight = if (viewModel.inviteCode != null) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.generateInviteCode() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = OnEmeraldPrimary
                        )
                    ) {
                        Text("Generate Code", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                GlassCard(cornerRadius = 22.dp) {
                    Text(
                        text = "Add Member",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HisabooTextField(
                        value = memberName,
                        onValueChange = { memberName = it },
                        label = "Name"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = memberPin,
                        onValueChange = { memberPin = it.take(6) },
                        label = "PIN",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.addFamilyMember(memberName, memberPin)
                            memberName = ""
                            memberPin = ""
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = OnEmeraldPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(viewModel.familyMembers, key = { it.id }) { member ->
                GlassCard(cornerRadius = 18.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TintedIconBadge(
                                icon = Icons.Rounded.Person,
                                tint = EmeraldPrimary,
                                size = 38.dp,
                                isCircle = true
                            )
                            Text(member.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "PIN: ••••",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BackupExportScreen(
    viewModel: HisabooViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    viewModel.onGoogleSignInSuccess(account)
                }
            } catch (e: ApiException) {
                statusBanner = "Google Sign-In error (${e.statusCode}). You can also use 'Send Backup to Gmail' below."
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            statusBanner = viewModel.restoreFromUri(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = "Backup & Cloud Sync", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val activeBanner = statusBanner ?: viewModel.cloudStatusBanner
            activeBanner?.let { msg ->
                item {
                    GlassCard(
                        cornerRadius = 16.dp,
                        containerColor = EmeraldGlowTint
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = EmeraldPrimary)
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                GlassCard(cornerRadius = 22.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TintedIconBadge(
                            icon = Icons.Rounded.AccountCircle,
                            tint = BlueAccent,
                            size = 42.dp,
                            isCircle = true
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gmail / Google Cloud Sync",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = viewModel.connectedGoogleEmail
                                    ?: "Not connected — sign in to auto-sync across phones",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (viewModel.connectedGoogleEmail != null) EmeraldPrimary else TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Just like Google Contacts: once signed in with Gmail, every entry auto-saves to your Google Drive AppData and restores automatically when you reinstall or switch phones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (viewModel.connectedGoogleEmail == null) {
                        Button(
                            onClick = {
                                val client = viewModel.cloudManager.getSignInClient()
                                googleSignInLauncher.launch(client.signInIntent)
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = OnEmeraldPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect Gmail Account", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceGlass)
                                .border(1.dp, BorderGlass, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Auto-Sync on Every Entry", style = MaterialTheme.typography.titleMedium)
                                Text("Instant cloud backup", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            }
                            Switch(
                                checked = viewModel.autoCloudSyncEnabled,
                                onCheckedChange = { viewModel.toggleAutoCloudSync(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnEmeraldPrimary,
                                    checkedTrackColor = EmeraldPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.manualCloudBackupNow() },
                                enabled = !viewModel.isCloudSyncing,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = OnEmeraldPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = if (viewModel.isCloudSyncing) "Syncing..." else "Sync Now",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.manualCloudRestoreNow() },
                                enabled = !viewModel.isCloudSyncing,
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SurfaceGlass,
                                    contentColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text("Restore Cloud", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { viewModel.disconnectGoogleAccount() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Disconnect Gmail", color = CashOutRed, fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider(color = BorderGlass, modifier = Modifier.padding(vertical = 12.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                val gmailIntent = viewModel.createGmailShareIntent()
                                context.startActivity(gmailIntent)
                            } catch (_: Exception) {
                                val fallback = viewModel.createGmailShareIntent().apply { setPackage(null) }
                                context.startActivity(android.content.Intent.createChooser(fallback, "Send Hisaboo Backup"))
                            }
                        },
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BlueAccent.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = BlueAccent.copy(alpha = 0.12f),
                            contentColor = BlueAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Backup File to My Gmail", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                GlassCard(cornerRadius = 22.dp) {
                    Text(
                        text = "Device & File Backup",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Last backup: ${viewModel.lastBackupText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { statusBanner = viewModel.backupNow() },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = OnEmeraldPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Backup now", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { statusBanner = viewModel.downloadBackupFile() },
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceGlass,
                                contentColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1.25f)
                                .height(48.dp)
                        ) {
                            Text("Save to Downloads", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Restore from local snapshot, Downloads, or any Gmail backup file:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { statusBanner = viewModel.restoreSavedBackup() },
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceGlass,
                                contentColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Restore local", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = EmeraldGlowTint,
                                contentColor = EmeraldPrimary
                            ),
                            modifier = Modifier
                                .weight(1.15f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Backup File", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                GlassCard(cornerRadius = 22.dp) {
                    Text(
                        text = "Excel / CSV Export",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Export all ${viewModel.transactions.size} entries to Hisaboo_Ledger.csv for Excel or Google Sheets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { statusBanner = viewModel.exportExcelCsv() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = OnEmeraldPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Export CSV", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePinScreen(
    viewModel: HisabooViewModel,
    onBack: () -> Unit
) {
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = "Change PIN", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassCard(cornerRadius = 22.dp) {
                Text(
                    text = "Set 4-digit PIN (default 1234)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                HisabooTextField(
                    value = currentPinInput,
                    onValueChange = { currentPinInput = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = "Current PIN",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                Spacer(modifier = Modifier.height(10.dp))

                HisabooTextField(
                    value = newPinInput,
                    onValueChange = { newPinInput = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = "New PIN",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                Spacer(modifier = Modifier.height(10.dp))

                HisabooTextField(
                    value = confirmPinInput,
                    onValueChange = { confirmPinInput = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = "Confirm PIN",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                feedbackMsg?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isError) CashOutRed else CashInGreen
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        when {
                            currentPinInput != viewModel.currentPin -> {
                                isError = true
                                feedbackMsg = "Current PIN is incorrect."
                            }
                            newPinInput.length != 4 -> {
                                isError = true
                                feedbackMsg = "New PIN must be exactly 4 digits."
                            }
                            newPinInput != confirmPinInput -> {
                                isError = true
                                feedbackMsg = "New PIN and Confirm PIN do not match."
                            }
                            else -> {
                                viewModel.updatePin(newPinInput)
                                isError = false
                                feedbackMsg = "PIN updated successfully!"
                                currentPinInput = ""
                                newPinInput = ""
                                confirmPinInput = ""
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = OnEmeraldPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Save PIN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(
    correctPin: String,
    fingerprintEnabled: Boolean,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var bioStatusMsg by remember { mutableStateOf<String?>(null) }

    fun triggerBiometricAuth() {
        if (!fingerprintEnabled) return
        val fragmentActivity = context.findFragmentActivity()
        if (fragmentActivity == null) {
            bioStatusMsg = "Biometric sensor unavailable — use PIN"
            return
        }

        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            bioStatusMsg = "No fingerprint registered on device — enter PIN"
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onUnlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        bioStatusMsg = errString.toString()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    bioStatusMsg = "Fingerprint not recognized — try again"
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Hisaboo")
            .setSubtitle("Touch the fingerprint sensor to access your ledger")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(fingerprintEnabled) {
        if (fingerprintEnabled) {
            triggerBiometricAuth()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TintedIconBadge(
            icon = Icons.Rounded.Lock,
            tint = EmeraldPrimary,
            size = 64.dp,
            iconSize = 30.dp,
            isCircle = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Hisaboo Locked", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (fingerprintEnabled) {
                "Scan fingerprint or enter 4-digit PIN"
            } else {
                "Enter your 4-digit PIN to unlock"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(22.dp))

        GlassCard(cornerRadius = 24.dp) {
            HisabooTextField(
                value = enteredPin,
                onValueChange = { raw ->
                    val clean = raw.filter { ch -> ch.isDigit() }.take(4)
                    enteredPin = clean
                    errorMsg = null
                    if (clean.length == 4) {
                        if (clean == correctPin) {
                            onUnlock()
                        } else {
                            errorMsg = "Incorrect PIN (Default is 1234)"
                            enteredPin = ""
                        }
                    }
                },
                label = "Enter 4-Digit PIN",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
            errorMsg?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = CashOutRed, style = MaterialTheme.typography.labelMedium)
            }
            if (fingerprintEnabled) {
                bioStatusMsg?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = AmberAccent, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (enteredPin == correctPin) {
                        onUnlock()
                    } else {
                        errorMsg = "Incorrect PIN (Default is 1234)"
                        enteredPin = ""
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = OnEmeraldPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Unlock with PIN", fontWeight = FontWeight.Bold)
            }

            if (fingerprintEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { triggerBiometricAuth() },
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = EmeraldGlowTint,
                        contentColor = EmeraldPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Fingerprint", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
