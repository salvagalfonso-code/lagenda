package com.example.agendavalenciana.ios

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The Android + modal: the options are calculated from a fresh server role and owned publications. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IosPublisherMenuSheet(
    policy: IosPublisherMenuState,
    loading: Boolean,
    warning: String?,
    onAction: (IosPublishAction) -> Unit,
    onProfessionalRequest: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(policy.heading, fontSize = 25.sp, fontWeight = FontWeight.Black, color = AgendaBlack)
            Text(policy.subtitle, fontSize = 14.sp, color = Color(0xFF6C6C6C), lineHeight = 23.sp)
            if (policy.suspended) Text("Publicació suspesa: consulta les notificacions de moderació.",
                color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (!warning.isNullOrBlank()) Text(warning, fontSize = 12.sp, color = Color(0xFF8A5B12))
            Spacer(Modifier.height(6.dp))
            policy.actions.forEach { action ->
                PublisherMenuRow(action, enabled = !loading, onClick = { onAction(action) })
            }
            if (policy.reviewActions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("REVISIÓ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                policy.reviewActions.forEach { action ->
                    PublisherMenuRow(action, enabled = !loading, onClick = { onAction(action) })
                }
            }
            policy.claimLabel?.let { label ->
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onProfessionalRequest, enabled = !loading,
                    modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(label, fontSize = 13.sp, color = Color(0xFF707070))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PublisherMenuRow(action: IosPublishAction, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF2F2F2))
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 53.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AgendaGlyph(action.glyph, Modifier.size(23.dp), AgendaGold)
            Spacer(Modifier.width(13.dp))
            Text(action.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f), color = AgendaBlack)
            Text("›", color = Color.Gray, fontSize = 26.sp)
        }
    }
}
