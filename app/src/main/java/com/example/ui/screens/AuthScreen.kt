package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.attemptAutoSignIn
import com.example.core.onGoogleSignInClicked
import com.example.ui.components.StarkButtonLabel
import com.example.ui.components.StarkGlowButton
import com.example.ui.components.StarkGlowText
import com.example.ui.components.StarkHudBackground
import com.example.ui.components.rememberStarkPulse
import com.example.ui.components.starkPulseGlow
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanBright
import com.example.ui.theme.ArcCyanDim
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.StarkGold

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    val coreGlow = rememberStarkPulse(durationMillis = 2400, minValue = 0.25f, maxValue = 0.72f)

    StarkHudBackground(modifier = modifier.fillMaxSize()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .starkPulseGlow(glowColor = ArcCyan, cornerRadius = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(28.dp)
                ) {
                    StarkGlowText(
                        text = "STARK PERSONAL OS • MULTI-LANGUAGE AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArcCyanBright
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(144.dp)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            ArcCyan.copy(alpha = coreGlow * 0.5f),
                                            Color.Transparent
                                        )
                                    ),
                                    radius = this.size.maxDimension * 0.62f
                                )
                            }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_jarvis_core),
                            contentDescription = "JARVIS Core Emblem",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(128.dp)
                                .clip(CircleShape)
                                .border(2.dp, ArcCyan.copy(alpha = 0.85f), CircleShape)
                        )
                    }

                    StarkGlowText(
                        text = stringResource(id = R.string.auth_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        glowColor = ArcCyan,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(id = R.string.auth_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    StarkGlowButton(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading,
                        containerColor = ArcCyanDim,
                        contentColor = ArcCyanBright,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = ArcCyanBright,
                                strokeWidth = 2.dp
                            )
                        } else {
                            StarkButtonLabel(
                                text = stringResource(id = R.string.sign_in_google),
                                color = ArcCyanBright,
                                glowColor = ArcCyan
                            )
                        }
                    }

                    Text(
                        text = "बॉस, तुमचा डेटा पूर्णपणे सुरक्षित आणि खाजगी ठेवला जातो.",
                        style = MaterialTheme.typography.labelSmall,
                        color = StarkGold.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
