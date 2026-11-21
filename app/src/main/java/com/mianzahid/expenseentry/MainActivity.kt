package com.mianzahid.expenseentry

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.mianzahid.expenseentry.ui.navigation.HisabooMainScaffold
import com.mianzahid.expenseentry.ui.theme.HisabooTheme

class MainActivity : FragmentActivity() {
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
