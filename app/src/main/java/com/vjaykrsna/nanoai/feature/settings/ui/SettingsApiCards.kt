package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// API Settings Cards
@Composable
internal fun APIsLoadBalancingCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "Load Balancing",
    infoText =
      "Distribute API calls across multiple providers with intelligent load balancing. Configure automatic failover when providers become unavailable, set rate limits to prevent quota exhaustion, and prioritize preferred providers for specific tasks.\n\nMulti-provider orchestration will enable seamless switching.",
    modifier = modifier,
  )
}

@Composable
internal fun APIsTestingCard(modifier: Modifier = Modifier) {
  SettingsInfoCard(
    title = "API Testing",
    infoText =
      "Monitor the health and performance of all configured AI providers. Test connectivity, track quota usage, analyze response times, and get alerts when providers experience issues or quota limits are approached.\n\nReal-time health checks and usage analytics for all configured APIs.",
    modifier = modifier,
  )
}
