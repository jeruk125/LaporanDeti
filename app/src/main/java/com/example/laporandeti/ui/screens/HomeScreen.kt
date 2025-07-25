package com.example.laporandeti.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.laporandeti.AppScreenRoutes
import com.example.laporandeti.ui.theme.LaporanDetiTheme

@Composable
fun HomeScreen(
    navController: NavController, // Parameter NavController
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Welcome to the Home Screen!")
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {
                navController.navigate(AppScreenRoutes.Camera)
            }) {
                Text("Go to Camera")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = {
                navController.navigate(AppScreenRoutes.Gallery)
            }) {
                Text("Go to Gallery")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    LaporanDetiTheme {
        val navController = rememberNavController()
        HomeScreen(navController = navController)
    }
}
