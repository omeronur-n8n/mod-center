package com.omeronur.gelisio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.omeronur.gelisio.data.Backend
import com.omeronur.gelisio.ui.components.GlassCard
import com.omeronur.gelisio.ui.theme.G

@Composable
fun MeScreen(user: FirebaseUser) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Ben", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = G.text)
        GlassCard {
            Text(user.displayName.orEmpty(), color = G.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(user.email.orEmpty(), color = G.textSecondary, fontSize = 13.sp)
        }
        GlassCard {
            Text("Sürüm 0.1.0", color = G.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text("İkonlar: Uicons by Flaticon", color = G.textSecondary, fontSize = 13.sp)
        }
        Button(
            onClick = { Backend.auth.signOut() },
            colors = ButtonDefaults.buttonColors(containerColor = G.critical, contentColor = G.bgBottom),
        ) { Text("Çıkış yap", fontWeight = FontWeight.SemiBold) }
    }
}
