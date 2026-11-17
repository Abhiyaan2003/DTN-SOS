package dtnsos.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtnsos.app.R
import dtnsos.app.ui.components.*
import dtnsos.app.ui.model.*
import dtnsos.app.ui.theme.DtnColors
import dtnsos.app.ui.theme.DtnSpacing
import dtnsos.app.ui.theme.DtnType

@Composable
fun HomeScreen(
    state: MeshUiState,
    onStartListening: () -> Unit,
    onRequestStopListening: () -> Unit,
    onNavigateToCarrying: () -> Unit,
    onRequestCancelSos: () -> Unit,
    onDebugLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(modifier = modifier) {
        TopBar(
            role = state.role,
            listening = state.listening,
            hasActiveSos = state.sos != null,
            capability = state.capability,
            onDebugLongClick = onDebugLongClick
        )

        when {
            state.sos != null -> {
                SosActiveContent(
                    sos = state.sos,
                    onCancelClick = onRequestCancelSos
                )
            }
            state.role == Role.GATEWAY -> {
                GatewayContent(gateway = state.gateway)
            }
            state.capability == Capability.CARRY_ONLY -> {
                CarryOnlyContent(
                    state = state,
                    onStopListening = onRequestStopListening,
                    onStartListening = onStartListening,
                    onNavigateToCarrying = onNavigateToCarrying
                )
            }
            !state.listening -> {
                PausedContent(
                    state = state,
                    onStartListening = onStartListening,
                    onNavigateToCarrying = onNavigateToCarrying
                )
            }
            else -> {
                MuleContent(
                    state = state,
                    onStopListening = onRequestStopListening,
                    onNavigateToCarrying = onNavigateToCarrying
                )
            }
        }
    }
}

@Composable
private fun MuleContent(
    state: MeshUiState,
    onStopListening: () -> Unit,
    onNavigateToCarrying: () -> Unit
) {
    HeroCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = stringResource(R.string.home_hero_label_carried),
                style = DtnType.label,
                color = DtnColors.TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${state.carriedCount}",
                style = DtnType.display,
                color = DtnColors.TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_hero_sub_waiting),
                style = DtnType.body,
                color = DtnColors.TextSecondary
            )

            val contactText = when (val s = state.lastContactSecondsAgo) {
                null -> "—"
                in 0..59 -> "${s} s ago"
                in 60..3599 -> "${s / 60} min ago"
                else -> "${s / 3600} h ago"
            }

            val nearbyText = state.nearbyCount?.toString() ?: "—"
            val batteryText = state.batteryDropPerHour?.let { "${"%.1f".format(it)}%/h" } ?: "—"

            StatFooter(
                stats = listOf(
                    StatItem(stringResource(R.string.stat_nearby_phones), nearbyText),
                    StatItem(stringResource(R.string.stat_last_contact), contactText),
                    StatItem(stringResource(R.string.stat_battery_use), batteryText)
                )
            )
        }
    }

    GhostButton(
        text = stringResource(R.string.action_stop_listening),
        onClick = onStopListening
    )

    LinkCard(
        title = stringResource(R.string.link_carrying_title),
        subtitle = stringResource(R.string.link_carrying_sub),
        onClick = onNavigateToCarrying
    )
}

@Composable
private fun PausedContent(
    state: MeshUiState,
    onStartListening: () -> Unit,
    onNavigateToCarrying: () -> Unit
) {
    HeroCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = stringResource(R.string.home_hero_label_carried),
                style = DtnType.label,
                color = DtnColors.TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${state.carriedCount}",
                style = DtnType.display,
                color = DtnColors.TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_hero_sub_paused),
                style = DtnType.body,
                color = DtnColors.TextSecondary
            )

            StatFooter(
                stats = listOf(
                    StatItem(stringResource(R.string.stat_nearby_phones), "—"),
                    StatItem(stringResource(R.string.stat_last_contact), "—"),
                    StatItem(stringResource(R.string.stat_battery_use), "0%/h")
                )
            )
        }
    }

    PrimaryButton(
        text = stringResource(R.string.action_start_listening),
        onClick = onStartListening
    )

    LinkCard(
        title = stringResource(R.string.link_carrying_title),
        subtitle = stringResource(R.string.link_carrying_sub),
        onClick = onNavigateToCarrying
    )
}

@Composable
private fun CarryOnlyContent(
    state: MeshUiState,
    onStopListening: () -> Unit,
    onStartListening: () -> Unit,
    onNavigateToCarrying: () -> Unit
) {
    if (state.listening) {
        MuleContent(
            state = state,
            onStopListening = onStopListening,
            onNavigateToCarrying = onNavigateToCarrying
        )
    } else {
        PausedContent(
            state = state,
            onStartListening = onStartListening,
            onNavigateToCarrying = onNavigateToCarrying
        )
    }

    Text(
        text = stringResource(R.string.home_carry_only_sub),
        style = DtnType.label,
        color = DtnColors.TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GatewayContent(gateway: GatewayStatsUi?) {
    val received = gateway?.received ?: 0
    val lastSyncText = gateway?.lastSyncSecondsAgo?.let { "${it} s ago" } ?: "—"

    val subText = when (gateway?.mqtt) {
        MqttState.CONNECTED -> stringResource(R.string.home_gateway_mqtt_connected, lastSyncText)
        MqttState.CONNECTING -> stringResource(R.string.home_gateway_mqtt_connecting)
        MqttState.OFFLINE -> stringResource(R.string.home_gateway_mqtt_offline)
        null -> "—"
    }

    HeroCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = stringResource(R.string.home_hero_label_received),
                style = DtnType.label,
                color = DtnColors.TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$received",
                style = DtnType.display,
                color = DtnColors.TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subText,
                style = DtnType.body,
                color = if (gateway?.mqtt == MqttState.OFFLINE) DtnColors.Warning else DtnColors.TextSecondary
            )

            StatFooter(
                stats = listOf(
                    StatItem(stringResource(R.string.stat_verified), "${gateway?.verified ?: 0}"),
                    StatItem(stringResource(R.string.stat_unverified), "${gateway?.unverified ?: 0}"),
                    StatItem(stringResource(R.string.stat_duplicates_dropped), "${gateway?.duplicatesDropped ?: 0}")
                )
            )
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.card_queued_title),
                style = DtnType.title,
                color = DtnColors.TextPrimary
            )
            val queued = gateway?.queued ?: 0
            val bodyText = if (queued == 0) {
                stringResource(R.string.card_queued_body_zero)
            } else {
                stringResource(R.string.card_queued_body, queued)
            }
            Text(
                text = bodyText,
                style = DtnType.body,
                color = DtnColors.TextSecondary
            )
        }
    }
}

@Composable
private fun SosActiveContent(
    sos: SosSessionUi,
    onCancelClick: () -> Unit
) {
    HeroCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PulsingRing {
                Text(
                    text = "${sos.carriersSeen}",
                    style = DtnType.displayMd,
                    color = DtnColors.TextPrimary
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.sos_active_title),
                style = DtnType.title,
                color = DtnColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.sos_active_caption),
                style = DtnType.caption,
                color = DtnColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            val minutes = sos.sentSecondsAgo / 60
            val seconds = sos.sentSecondsAgo % 60
            val sentText = "%d:%02d ago".format(minutes, seconds)

            val locationDisplay = if (sos.locationApproximate) {
                sos.locationText + stringResource(R.string.suffix_approximate)
            } else {
                sos.locationText
            }

            val categoryName = sos.category.name.lowercase().replaceFirstChar { it.uppercase() }
            val peopleText = if (sos.people == 1) "1 person" else "${sos.people} people"
            val messageDisplay = "$categoryName · $peopleText"

            KeyValueRow(
                label = stringResource(R.string.key_sent),
                value = sentText,
                showDivider = true
            )
            KeyValueRow(
                label = stringResource(R.string.key_lifetime_left),
                value = "${sos.lifetimeLeftHours} h",
                showDivider = true
            )
            KeyValueRow(
                label = stringResource(R.string.key_location),
                value = locationDisplay,
                showDivider = true
            )
            KeyValueRow(
                label = stringResource(R.string.key_message),
                value = messageDisplay,
                showDivider = false
            )
        }
    }

    Text(
        text = stringResource(R.string.sos_active_helper),
        style = DtnType.label,
        color = DtnColors.TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    GhostButton(
        text = stringResource(R.string.action_cancel_sos),
        onClick = onCancelClick
    )
}

