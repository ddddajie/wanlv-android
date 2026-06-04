package com.wanlv.app.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanlv.app.data.MockData
import com.wanlv.app.ui.components.IOSCard
import com.wanlv.app.ui.components.IOSSearchBar
import com.wanlv.app.ui.theme.WanLvBackground
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvGreenLight
import com.wanlv.app.ui.theme.WanLvMint
import com.wanlv.app.ui.theme.WanLvSurface
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary

@Composable
fun MapScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WanLvBackground)
            .statusBarsPadding()
    ) {
        MapPlaceholder()
        IOSSearchBar(
            placeholder = "搜索景区、位置、服务",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        )
        MapTools(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
        )
        SpotBottomSheet(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp, vertical = 18.dp)
        )
    }
}

@Composable
private fun MapPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 74.dp)
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(
                Brush.linearGradient(
                    listOf(WanLvMint, androidx.compose.ui.graphics.Color(0xFFDDF2E6), androidx.compose.ui.graphics.Color(0xFFEAF4DE))
                )
            )
    ) {
        listOf(
            Triple(70.dp, 80.dp, "服务台"),
            Triple(208.dp, 142.dp, "九龙灌浴"),
            Triple(146.dp, 252.dp, "灵山大佛"),
            Triple(92.dp, 360.dp, "莲花广场")
        ).forEach { (x, y, title) ->
            Column(
                modifier = Modifier.offset(x, y),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, color = WanLvTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(WanLvGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("•", color = WanLvSurface, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun MapTools(modifier: Modifier = Modifier) {
    val tools = listOf(
        MapTool(Icons.Rounded.Layers, "图层"),
        MapTool(Icons.Rounded.Wc, "卫生间"),
        MapTool(Icons.Rounded.LocalParking, "停车场"),
        MapTool(Icons.Rounded.MyLocation, "定位")
    )
    IOSCard(
        modifier = modifier,
        padding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
        cornerRadius = 24.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            tools.forEach {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = it.icon, contentDescription = it.title, tint = WanLvTextSecondary, modifier = Modifier.size(18.dp))
                    Text(it.title, color = WanLvTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class MapTool(
    val icon: ImageVector,
    val title: String
)

@Composable
private fun SpotBottomSheet(modifier: Modifier = Modifier) {
    val spot = MockData.mustPlaySpots.first()
    IOSCard(modifier = modifier.fillMaxWidth(), cornerRadius = 26.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(listOf(WanLvMint, WanLvGreenLight.copy(alpha = 0.7f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(spot.imageEmoji, fontSize = 32.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(spot.name, color = WanLvTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(spot.tag, color = WanLvGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(spot.description, color = WanLvTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                    Text("★ ${spot.rating}   ${spot.distance}", color = WanLvGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WanLvGreen)
                ) { Text("路线导航", fontWeight = FontWeight.Bold) }
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WanLvGreenLight)
                ) { Text("语音讲解", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
