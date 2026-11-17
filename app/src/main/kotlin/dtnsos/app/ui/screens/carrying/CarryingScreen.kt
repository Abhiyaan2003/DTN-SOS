package dtnsos.app.ui.screens.carrying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dtnsos.app.R
import dtnsos.app.ui.components.*
import dtnsos.app.ui.haptics.HapticEvent
import dtnsos.app.ui.haptics.LocalHaptics
import dtnsos.app.ui.model.PacketState
import dtnsos.app.ui.model.PacketUi
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnShapes
import dtnsos.app.ui.theme.DtnSpacing
import dtnsos.app.ui.theme.DtnType

@Composable
fun CarryingScreen(
    packets: List<PacketUi>,
    isListening: Boolean,
    onPacketClick: (PacketUi) -> Unit,
    onStartListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(
        scrollable = false,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.carrying_title),
                style = DtnType.headline,
                color = DtnColors.TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.carrying_helper),
                style = DtnType.label,
                color = DtnColors.TextSecondary
            )
        }

        if (packets.isEmpty()) {
            EmptyCarryingState(
                isListening = isListening,
                onStartListening = onStartListening,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = packets,
                        key = { it.msgIdHex }
                    ) { packet ->
                        PacketRow(
                            packet = packet,
                            onClick = { onPacketClick(packet) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PacketRow(
    packet: PacketUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHaptics.current
    val interactionSource = remember { MutableInteractionSource() }

    val categoryName = packet.category.name.lowercase().replaceFirstChar { it.uppercase() }
    val peopleText = if (packet.people == 1) "1 person" else "${packet.people} people"
    val titleText = "$categoryName · $peopleText"

    val severityName = packet.severity.name.lowercase().replaceFirstChar { it.uppercase() }
    val subtitleParts = buildList {
        add(severityName)
        add(packet.ageText)
        if (!packet.distanceText.isNullOrBlank()) {
            add(packet.distanceText)
        }
    }
    val subtitleText = subtitleParts.joinToString(" · ")

    val (tagText, tagVariant) = when (packet.state) {
        PacketState.SPRAY -> "Spray ${packet.tokens}" to TagVariant.DEFAULT
        PacketState.WAIT -> "Wait" to TagVariant.DEFAULT
        PacketState.DELIVERED -> "Delivered" to TagVariant.DEFAULT
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clip(DtnShapes.radiusMd)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.play(HapticEvent.Tap)
                    onClick()
                }
            )
            .pressScale(interactionSource)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SeverityBadge(severity = packet.severity)

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleText,
                style = DtnType.bodyStrong,
                color = DtnColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitleText,
                style = DtnType.label,
                color = DtnColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Tag(
            text = tagText,
            variant = tagVariant
        )
    }
}

@Composable
private fun EmptyCarryingState(
    isListening: Boolean,
    onStartListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BigIconCircle(
                icon = Icons.AutoMirrored.Filled.List,
                tint = DtnColors.TextSecondary
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.carrying_empty_title),
                style = DtnType.title,
                color = DtnColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isListening) {
                    stringResource(R.string.carrying_empty_listening)
                } else {
                    stringResource(R.string.carrying_empty_paused)
                },
                style = DtnType.body,
                color = DtnColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            if (!isListening) {
                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    text = stringResource(R.string.action_start_listening),
                    onClick = onStartListening
                )
            }
        }
    }
}

