package com.wanlv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wanlv.app.R
import com.wanlv.app.model.ScenicSpot
import com.wanlv.app.ui.theme.WanLvGreen
import com.wanlv.app.ui.theme.WanLvTextPrimary
import com.wanlv.app.ui.theme.WanLvTextSecondary

@Composable
fun ScenicSpotCard(
    spot: ScenicSpot,
    modifier: Modifier = Modifier
) {
    IOSCard(
        modifier = modifier.width(154.dp),
        padding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsyncImage(
                model = spot.coverImageUrl,
                contentDescription = spot.name,
                placeholder = painterResource(R.drawable.default_spot),
                error = painterResource(R.drawable.default_spot),
                fallback = painterResource(R.drawable.default_spot),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(WanLvGreen.copy(alpha = 0.08f))
            )
            Text(spot.name, color = WanLvTextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(spot.location, color = WanLvTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("★ ${spot.rating}", color = WanLvGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(2.dp))
        }
    }
}
