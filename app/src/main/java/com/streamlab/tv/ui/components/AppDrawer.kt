package com.streamlab.tv.ui.components
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment

import androidx.tv.material3.Surface


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import com.streamlab.tv.ui.theme.AppIcons
import com.streamlab.tv.ui.theme.SurfaceDark

@Composable
fun AppDrawer(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    NavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .background(SurfaceDark)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .width(if (it == DrawerValue.Open) 200.dp else 64.dp),
                verticalArrangement = Arrangement.Center
            ) {
                DrawerItem(
                    icon = AppIcons.Search,
                    label = "Busca",
                    isSelected = currentRoute == "search",
                    isExpanded = it == DrawerValue.Open,
                    onClick = { onNavigate("search") }
                )
                DrawerItem(
                    icon = AppIcons.Home,
                    label = "Canais",
                    isSelected = currentRoute == "main",
                    isExpanded = it == DrawerValue.Open,
                    onClick = { onNavigate("main") }
                )
                DrawerItem(
                    icon = AppIcons.Settings,
                    label = "Ajustes",
                    isSelected = currentRoute == "settings",
                    isExpanded = it == DrawerValue.Open,
                    onClick = { onNavigate("settings") }
                )
            }
        }
    ) {
        content()
    }
}


@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    androidx.tv.material3.Surface(
        onClick = onClick,
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
            if (isExpanded) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = label)
            }
        }
    }
}
