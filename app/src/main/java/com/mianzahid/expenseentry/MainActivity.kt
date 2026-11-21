package com.mianzahid.expenseentry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mianzahid.expenseentry.ui.navigation.HisabooMainScaffold
import com.mianzahid.expenseentry.ui.theme.HisabooTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HisabooTheme {
                HisabooMainScaffold()
            }
        }
    }
}
