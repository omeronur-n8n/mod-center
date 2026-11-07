package com.omeronur.gelisio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omeronur.gelisio.ui.components.GlassCard
import com.omeronur.gelisio.ui.icons.GelisioIconFont
import com.omeronur.gelisio.ui.theme.G

/** Henüz veri bağlanmamış modüllerin iskeleti: her kart bir sonraki sürümün işini tanımlar. */
@Composable
fun RoadmapScreen(title: String, subtitle: String, icon: Char, items: List<Pair<String, String>>) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = G.text)
        Text(subtitle, color = G.textSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        items.forEach { (name, desc) ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        icon.toString(),
                        fontFamily = GelisioIconFont,
                        fontSize = 22.sp,
                        color = G.mint,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.fillMaxWidth()) {
                        Text(name, color = G.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(desc, color = G.textSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
