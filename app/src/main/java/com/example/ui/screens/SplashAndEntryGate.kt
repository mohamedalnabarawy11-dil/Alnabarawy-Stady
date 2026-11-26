package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.SoundEngine
import com.example.i18n.AppStrings
import com.example.ui.components.GoldHairlineDivider
import com.example.ui.components.LuxuryGoldButton
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.IvoryWhite
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianPure
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.SoftGoldGlow
import com.example.ui.theme.WarmSilverText
import kotlinx.coroutines.delay

@Composable
fun SplashScreenOverlay(
    onFinished: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1250L)
        visible = false
        delay(650L)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(650))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianPure),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, ChampagneGold.copy(alpha = 0.8f), CircleShape)
                        .background(ObsidianPure),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nabraawy_logo),
                        contentDescription = "النبراوي للدراسة",
                        modifier = Modifier
                            .size(102.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(26.dp))
                Text(
                    text = "النبراوي للدراسة",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ChampagneGold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "NABRAAWY STUDY",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlayfairDisplayFamily,
                        letterSpacing = 3.sp
                    ),
                    color = SoftGoldGlow.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EntryGateScreen(
    strings: AppStrings,
    soundEngine: SoundEngine,
    onInstallEnter: () -> Unit,
    onExitApp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianPure, ObsidianBlack, ObsidianCard)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ObsidianCard.copy(alpha = 0.94f))
                .border(1.2.dp, ChampagneGold.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, ChampagneGold.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_nabraawy_crest),
                    contentDescription = strings.appTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = strings.appTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = ChampagneGold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Nabraawy Study",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = PlayfairDisplayFamily,
                    letterSpacing = 2.sp
                ),
                color = SoftGoldGlow.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            GoldHairlineDivider(color = ChampagneGold)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = strings.entrySubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = IvoryWhite.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ONLY TWO BUTTONS on the Entry Gate as requested:
            LuxuryGoldButton(
                text = strings.installAppBtn,
                icon = Icons.Filled.DownloadDone,
                onClick = onInstallEnter,
                soundEngine = soundEngine,
                isPrimary = true,
                testTagName = "install_app_button",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            LuxuryGoldButton(
                text = strings.exitSiteBtn,
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = onExitApp,
                soundEngine = soundEngine,
                isPrimary = false,
                testTagName = "exit_app_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
