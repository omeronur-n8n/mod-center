package com.omeronur.gelisio.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.omeronur.gelisio.data.Backend
import com.omeronur.gelisio.ui.theme.G

@Composable
fun SignInScreen() {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val client = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Backend.webClientId(context).orEmpty())
                .requestEmail()
                .build(),
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken.orEmpty(), null)
            Backend.auth.signInWithCredential(credential).addOnFailureListener { error = it.message ?: "Giriş başarısız" }
        } catch (e: ApiException) {
            error = "Google girişi başarısız (kod ${e.statusCode})"
        }
    }
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Gelisio", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = G.text)
        Spacer(Modifier.height(8.dp))
        Text("Alışkanlıklar, projeler, hobiler — tek yerde.", color = G.textSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { launcher.launch(client.signInIntent) },
            colors = ButtonDefaults.buttonColors(containerColor = G.mint, contentColor = G.bgBottom),
        ) { Text("Google ile giriş yap", fontWeight = FontWeight.SemiBold) }
        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = G.critical, fontSize = 13.sp)
        }
    }
}
