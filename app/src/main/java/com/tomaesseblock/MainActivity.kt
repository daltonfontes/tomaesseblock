package com.tomaesseblock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tomaesseblock.ui.TomaEsseBlockRoot
import com.tomaesseblock.ui.theme.TomaEsseBlockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TomaEsseBlockTheme {
                TomaEsseBlockRoot()
            }
        }
    }
}
