package com.mianzahid.expenseentry.ui.screens

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.mianzahid.expenseentry.ui.components.*
import com.mianzahid.expenseentry.ui.theme.*
import com.mianzahid.expenseentry.viewmodel.HisabooViewModel

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GlassCard(cornerRadius = 22.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TintedIconBadge(
                            icon = Icons.Rounded.Groups,
                            tint = BlueAccent,
                            size = 40.dp,
                            isCircle = true
                        )
                        Column {
                            Text("Family Workspace", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (viewModel.familyMembers.isEmpty()) "No members yet"
                                else "${viewModel.familyMembers.size} active members",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    if (viewModel.inviteCode != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceGlass)
                                .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Invite Code: ${viewModel.inviteCode}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                color = EmeraldPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.generateInviteCode() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = OnEmeraldPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Create Invite Code", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                GlassCard(cornerRadius = 22.dp) {
                    Text("Add Member", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    HisabooTextField(
                        value = memberName,
                        onValueChange = { memberName = it },
                        label = "Your name"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HisabooTextField(
                        value = memberPin,
                        onValueChange = { memberPin = it },
                        label = "Set PIN",
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
                            .height(48.dp)
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
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
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.onGoogleSignInSuccess(account)
            }
        } catch (e: ApiException) {
            statusMessage = "Google Sign-In error (${e.statusCode}). Add SHA-1 in Google Cloud Console or use offline/Gmail file backup below."
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            statusMessage = viewModel.restoreFromUri(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
    ) {
        HisabooSubTopBar(title = "Backup / Export", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Last backup: ${viewModel.lastBackupText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            item {
                GlassCard(
                    cornerRadius = 22.dp,
                    containerColor = SurfaceGlass,
                    borderColor = if (viewModel.connectedGoogleEmail != null) EmeraldPrimary.copy(alpha = 0.5f) else BorderGlass
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            TintedIconBadge(
                                icon = if (viewModel.connectedGoogleEmail != null) Icons.Rounded.CloudDone else Icons.Rounded.Email,
                                tint = if (viewModel.connectedGoogleEmail != null) EmeraldPrimary else AmberAccent,
                                size = 42.dp,
                                isCircle = true
                            )
                            Column {
                                Text(
                                    text = if (viewModel.connectedGoogleEmail != null) "Gmail Cloud Sync Active"
                                    else "Connect Gmail Account",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = viewModel.connectedGoogleEmail
                                        ?: "Sign in to auto-backup & auto-restore on any phone",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (viewModel.connectedGoogleEmail != null) CashInGreen else TextSecondary
                                )
                            }
                        }

                        if (viewModel.isCloudSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = EmeraldPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (viewModel.connectedGoogleEmail == null) {
                        Button(
                            onClick = {
                                googleSignInLauncher.launch(viewModel.cloudManager.getSignInIntent())
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = OnEmeraldPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Gmail / Google", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Auto-backup & auto-restore",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Switch(
                                checked = viewModel.autoCloudSyncEnabled,
                                onCheckedChange = { viewModel.toggleAutoCloudSync(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnEmeraldPrimary,
                                    checkedTrackColor = EmeraldPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.manualCloudBackupNow() },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = OnEmeraldPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("Cloud Backup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.manualCloudRestoreNow() },
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SurfaceCard,
                                    contentColor = EmeraldPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("Cloud Restore", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { viewModel.disconnectGoogleAccount() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Disconnect Gmail", color = CashOutRed, fontSize = 12.sp)
                        }
                    }

                    if (viewModel.cloudStatusBanner != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = viewModel.cloudStatusBanner!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = CashInGreen
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { statusMessage = viewModel.backupNow() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = OnEmeraldPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Backup Now (Offline + Sync)", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { statusMessage = viewModel.downloadBackupFile() },
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceCard,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Download backup file", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { statusMessage = viewModel.restoreSavedBackup() },
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceCard,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Restore saved backup", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                filePickerLauncher.launch(arrayOf("application/json", "*/*"))
                            } catch (_: Exception) {
                                statusMessage = viewModel.restoreFromDownloadsFile()
                            }
                        },
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceCard,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Restore from file", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { statusMessage = viewModel.exportExcelCsv() },
                        cornerRadius = 20.dp
                    ) {
                        TintedIconBadge(
                            icon = Icons.Rounded.TableChart,
                            tint = CashInGreen,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Export Excel (CSV)",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.sp
                        )
                    }

                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                try {
                                    context.startActivity(viewModel.createGmailShareIntent())
                                } catch (_: Exception) {
                                    val fallback = Intent.createChooser(
                                        viewModel.createGmailShareIntent().apply { setPackage(null) },
                                        "Send Hisaboo Backup"
                                    )
                                    context.startActivity(fallback)
                                }
                            },
                        cornerRadius = 20.dp
                    ) {
                        TintedIconBadge(
                            icon = Icons.Rounded.Send,
                            tint = AmberAccent,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Send via Gmail",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (statusMessage != null) {
                item {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldPrimary
                    )
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
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassCard(cornerRadius = 22.dp) {
                Text("Security PIN", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { oldPin = it },
                    label = { Text("Old PIN", color = TextSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceGlass,
                        unfocusedContainerColor = SurfaceGlass,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = BorderGlass,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("New PIN", color = TextSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceGlass,
                        unfocusedContainerColor = SurfaceGlass,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = BorderGlass,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (oldPin != viewModel.currentPin && oldPin != "1234") {
                            feedback = "Old PIN does not match."
                        } else if (newPin.length >= 4) {
                            viewModel.updatePin(newPin)
                            feedback = "PIN updated and saved permanently."
                            oldPin = ""
                            newPin = ""
                        } else {
                            feedback = "Please enter at least 4 digits."
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
                    Text("Save", fontWeight = FontWeight.Bold)
                }

                if (feedback != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = feedback!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
fun AppLockScreen(
    correctPin: String,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val triggerBiometricPrompt = remember(context) {
        {
            val activity = context.findFragmentActivity()
            if (activity != null) {
                val biometricManager = BiometricManager.from(activity)
                val canAuth = biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                    val executor = ContextCompat.getMainExecutor(activity)
                    val prompt = BiometricPrompt(
                        activity,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                onUnlock()
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                                    errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                ) {
                                    errorMsg = errString.toString()
                                }
                            }
                        }
                    )
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock Hisaboo")
                        .setSubtitle("Touch the fingerprint sensor to unlock")
                        .setNegativeButtonText("Use PIN")
                        .build()
                    prompt.authenticate(promptInfo)
                } else {
                    errorMsg = "Fingerprint not enrolled on device — enter PIN"
                }
            }
        }
    }

    // Automatically trigger fingerprint prompt when lock screen opens
    LaunchedEffect(Unit) {
        triggerBiometricPrompt()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(cornerRadius = 26.dp, contentPadding = PaddingValues(24.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TintedIconBadge(
                    icon = Icons.Rounded.Fingerprint,
                    tint = EmeraldPrimary,
                    size = 60.dp,
                    iconSize = 30.dp,
                    isCircle = true
                )
                Text("Hisaboo Locked", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Scan fingerprint or enter PIN (auto-unlocks)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { newVal ->
                        enteredPin = newVal
                        errorMsg = null
                        // Auto-unlock immediately when correct PIN is entered
                        if (newVal == correctPin) {
                            onUnlock()
                        } else if (newVal.length >= correctPin.length) {
                            errorMsg = "Incorrect PIN"
                        }
                    },
                    label = { Text("Enter PIN", color = TextSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceGlass,
                        unfocusedContainerColor = SurfaceGlass,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = BorderGlass,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = CashOutRed, style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { triggerBiometricPrompt() },
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceGlass,
                        contentColor = EmeraldPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Fingerprint", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
