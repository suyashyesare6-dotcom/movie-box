package com.example.ui.screens

import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AiResponseState
import com.example.model.SecurityCheck
import com.example.model.UiState
import com.example.ui.components.StarkButtonLabel
import com.example.ui.components.StarkGlowButton
import com.example.ui.components.StarkGlowText
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanBright
import com.example.ui.theme.ArcCyanDim
import com.example.ui.theme.HudOutline
import com.example.ui.theme.SecureEmerald
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.StarkGold
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

@Composable
fun SecurityScreen(
    checksState: UiState<List<SecurityCheck>>,
    analysisState: AiResponseState,
    analysisText: String,
    onAnalyzeSuspiciousText: (String) -> Unit,
    onToggleCheck: (SecurityCheck) -> Unit,
    onSpeakText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var suspiciousInput by remember { mutableStateOf("") }
    var demoPlaintext by remember { mutableStateOf("Stark Lab Sample Note") }
    var encryptedPreview by remember { mutableStateOf("") }
    var decryptedPreview by remember { mutableStateOf("") }

    val checks = (checksState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Shield Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SecureEmerald.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.img_security_shield),
                        contentDescription = "JARVIS Defensive Security Shield",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        StarkGlowText(
                            text = "DEFENSIVE SECURITY & ENCRYPTION LAB",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecureEmerald
                        )
                        Text(
                            text = "तुमच्या अकाउंट्स, डिव्हाइस आणि डेटाचे संरक्षण",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "कधीही खरे पासवर्ड, OTP किंवा प्रायव्हेट कीज चॅटमध्ये टाकू नका, बॉस.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StarkGold
                        )
                    }
                }
            }
        }

        // Phishing / Scam Analyzer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HudOutline, RoundedCornerShape(18.dp))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ArcCyan)
                        Text("फिशिंग व स्कॅम तपासणी", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "संशयास्पद SMS, लिंक किंवा मेसेज इथे पेस्ट करा (वैयक्तिक OTP/पासवर्ड वगळून).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = suspiciousInput,
                        onValueChange = { suspiciousInput = it },
                        label = { Text("संशयास्पद मेसेज किंवा लिंक") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phishing_input")
                    )
                    StarkGlowButton(
                        onClick = { onAnalyzeSuspiciousText(suspiciousInput) },
                        enabled = suspiciousInput.isNotBlank() && analysisState !is AiResponseState.Loading,
                        containerColor = ArcCyanDim,
                        contentColor = ArcCyanBright,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_scam_button")
                    ) {
                        if (analysisState is AiResponseState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ArcCyanBright,
                                strokeWidth = 2.dp
                            )
                        } else {
                            StarkButtonLabel(
                                text = "धोका तपासा",
                                color = ArcCyanBright,
                                glowColor = ArcCyan
                            )
                        }
                    }

                    if (analysisText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlateSurface)
                                .border(1.dp, HudOutline, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("JARVIS सुरक्षा विश्लेषण", style = MaterialTheme.typography.labelSmall, color = StarkGold)
                                    IconButton(onClick = { onSpeakText(analysisText) }) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak analysis", tint = ArcCyan)
                                    }
                                }
                                Text(analysisText, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else if (analysisState is AiResponseState.Error) {
                        Text(analysisState.message, color = AlertCrimson)
                    }
                }
            }
        }

        // Interactive Educational AES-256 Demo
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HudOutline, RoundedCornerShape(18.dp))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = StarkGold)
                        Text("AES-256 एन्क्रिप्शन डेमो (शिक्षणासाठी)", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "स्वतःचा अल्गोरिदम कधीच बनवू नका; नेहमी AES-256-GCM सारखे स्टँडर्ड वापरा. खाली नमुना मजकूर एन्क्रिप्ट करून पहा (खरे पासवर्ड टाकू नका).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = demoPlaintext,
                        onValueChange = { demoPlaintext = it },
                        label = { Text("नमुना मजकूर (Plaintext)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StarkGlowButton(
                        onClick = {
                            runCatching {
                                val keyGen = KeyGenerator.getInstance("AES")
                                keyGen.init(256)
                                val secretKey = keyGen.generateKey()
                                val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
                                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                                cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                                val cipherBytes = cipher.doFinal(demoPlaintext.toByteArray(Charsets.UTF_8))
                                val encoded = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)

                                val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
                                decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                                val restored = String(decryptCipher.doFinal(cipherBytes), Charsets.UTF_8)

                                encryptedPreview = encoded
                                decryptedPreview = restored
                            }
                        },
                        enabled = demoPlaintext.isNotBlank(),
                        accentColor = SecureEmerald,
                        containerColor = SecureEmerald.copy(alpha = 0.16f),
                        contentColor = SecureEmerald,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SecureEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        StarkButtonLabel(
                            text = "AES-256 ने लॉक व अनलॉक करून पहा",
                            color = SecureEmerald
                        )
                    }
                    if (encryptedPreview.isNotBlank()) {
                        Text(
                            text = "Ciphertext (Base64): $encryptedPreview",
                            style = MaterialTheme.typography.labelMedium,
                            color = ArcCyan
                        )
                        Text(
                            text = "Decrypted परत मिळालेला मजकूर: $decryptedPreview",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecureEmerald
                        )
                    }
                }
            }
        }

        // Security Hygiene Checklist
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SecureEmerald)
                Text(
                    text = "वैयक्तिक सुरक्षा चेकलिस्ट (${checks.count { it.completed }}/${checks.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        when (checksState) {
            is UiState.Loading -> item { CircularProgressIndicator(color = ArcCyan) }
            is UiState.Error -> item { Text(checksState.message, color = AlertCrimson) }
            is UiState.Success -> {
                items(checks, key = { it.id }) { check ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, HudOutline, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Checkbox(
                                checked = check.completed,
                                onCheckedChange = { onToggleCheck(check) },
                                colors = CheckboxDefaults.colors(checkedColor = SecureEmerald, uncheckedColor = ArcCyan)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(check.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = check.guidance,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
