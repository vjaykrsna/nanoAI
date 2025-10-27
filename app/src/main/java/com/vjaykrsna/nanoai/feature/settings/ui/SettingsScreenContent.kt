package com.vjaykrsna.nanoai.feature.settings.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.uiux.ui.components.foundation.NanoSpacing
import kotlinx.coroutines.launch

// NOTE: Tab order designed to match user workflow - frequently changed settings first
private enum class SettingsCategory(val title: String) {
  APPEARANCE(title = "Appearance"),
  BEHAVIOR(title = "Behavior"),
  APIS(title = "APIs"),
  PRIVACY_SECURITY(title = "Privacy & Security"),
  BACKUP_SYNC(title = "Backup & Sync"),
  ABOUT(title = "About"),
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
  state: SettingsContentState,
  snackbarHostState: SnackbarHostState,
  actions: SettingsScreenActions,
  modifier: Modifier = Modifier,
) {
  val categories = SettingsCategory.entries.toList()
  val pagerState = rememberPagerState(initialPage = 0) { categories.size }
  val coroutineScope = rememberCoroutineScope()
  val currentCategory = categories[pagerState.currentPage]

  Scaffold(
    modifier =
      modifier
        .fillMaxSize()
        .semantics {
          contentDescription = "Settings screen organized by tabs with contextual sections"
        }
        .focusProperties {
          // Ensure tab navigation starts from the tabs
          canFocus = false
        },
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
      )
    },
    floatingActionButton = {
      if (currentCategory == SettingsCategory.APIS) {
        FloatingActionButton(
          onClick = actions.onAddProviderClick,
          modifier = Modifier.semantics { contentDescription = "Add API provider" },
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add")
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = NanoSpacing.lg)
    ) {
      SettingsCategoryTabs(
        categories = categories,
        selectedCategory = currentCategory,
        onCategorySelect = { category ->
          val targetIndex = categories.indexOf(category).coerceAtLeast(0)
          coroutineScope.launch { pagerState.animateScrollToPage(targetIndex) }
        },
      )

      Box(modifier = Modifier.weight(1f, fill = true)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
          val category = categories[pageIndex]
          SettingsCategoryContent(
            category = category,
            state = state,
            actions = actions,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}

@Composable
private fun SettingsCategoryTabs(
  categories: List<SettingsCategory>,
  selectedCategory: SettingsCategory,
  onCategorySelect: (SettingsCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectedIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)
  PrimaryScrollableTabRow(
    selectedTabIndex = selectedIndex,
    edgePadding = 0.dp,
    modifier =
      modifier.fillMaxWidth().semantics {
        contentDescription = "Settings categories navigation tabs"
      },
  ) {
    categories.forEachIndexed { index, category ->
      val isSelected = category == selectedCategory
      Tab(
        selected = isSelected,
        onClick = { onCategorySelect(category) },
        text = {
          Text(
            text = category.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
          )
        },
      )
    }
  }
}

@Composable
private fun SettingsCategoryContent(
  category: SettingsCategory,
  state: SettingsContentState,
  actions: SettingsScreenActions,
  modifier: Modifier = Modifier,
) {
  if (state.isLoading) {
    Box(
      modifier = Modifier.fillMaxSize().semantics { contentDescription = "Loading settings" },
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        CircularProgressIndicator()
        Text(
          "Loading settings...",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    return
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(NanoSpacing.lg),
    contentPadding = PaddingValues(top = NanoSpacing.lg, bottom = 112.dp),
  ) {
    when (category) {
      SettingsCategory.APPEARANCE -> {
        item {
          AppearanceThemeCard(
            uiUxState = state.uiUxState,
            onThemeChange = actions.onThemePreferenceChange,
            onHighContrastChange = actions.onHighContrastChange,
          )
        }
        item {
          AppearanceDensityCard(
            uiUxState = state.uiUxState,
            onDensityChange = actions.onVisualDensityChange,
          )
        }
        item { AppearanceTypographyCard() }
        item { AppearanceAnimationPreferencesCard() }
      }
      SettingsCategory.BEHAVIOR -> {
        item { BehaviorStartupCard() }
        item { BehaviorInputPreferencesCard() }
        item { BehaviorAccessibilityCard() }
        item { BehaviorNotificationsCard() }
      }
      SettingsCategory.APIS -> {
        // NOTE: Migration card shows once after credential storage upgrade
        if (state.uiUxState.showMigrationSuccessNotification) {
          item { MigrationSuccessCard(onDismiss = actions.onDismissMigrationSuccess) }
        }

        item { ApiProvidersCard(hasProviders = state.apiProviders.isNotEmpty()) }

        items(
          items = state.apiProviders,
          key = { it.providerId },
          contentType = { "api_provider_card" },
        ) { provider ->
          ApiProviderCard(
            provider = provider,
            onEdit = { actions.onProviderEdit(provider) },
            onDelete = { actions.onProviderDelete(provider) },
          )
        }

        item {
          HuggingFaceAuthCard(
            state = state.huggingFaceState,
            onLoginClick = actions.onHuggingFaceLoginClick,
            onApiKeyClick = actions.onHuggingFaceApiKeyClick,
            onDisconnectClick = actions.onHuggingFaceDisconnectClick,
          )
        }

        item { APIsLoadBalancingCard() }
        item { APIsTestingCard() }
      }
      SettingsCategory.PRIVACY_SECURITY -> {
        item {
          PrivacySection(
            privacyPreferences = state.privacyPreferences,
            onTelemetryToggle = actions.onTelemetryToggle,
            onRetentionPolicyChange = actions.onRetentionPolicyChange,
          )
        }
        item { PrivacyAppLockCard() }
        item { PrivacyDataManagementCard() }
        item { PrivacyEncryptionCard() }
      }
      SettingsCategory.BACKUP_SYNC -> {
        item {
          DataManagementSection(
            onImportBackupClick = actions.onImportBackupClick,
            onExportBackupClick = actions.onExportBackupClick,
          )
        }
        item { BackupAutomatedCard() }
        item { BackupCloudSyncCard() }
        item { BackupDataMigrationCard() }
      }
      SettingsCategory.ABOUT -> {
        item { AboutNanoAICard() }
        item { AboutSupportFeedbackCard() }
        item { AboutDocumentationCard() }
        item { AboutSystemInformationCard() }
        item { AboutAdvancedDiagnosticsCard() }
        item { AboutCacheManagementCard() }
        item { AboutExperimentalFeaturesCard() }
      }
    }
  }
}

@Composable
internal fun MigrationSuccessCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        text = "Migration Successful",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text =
          "Your provider credentials have been migrated to a more secure storage. " +
            "For enhanced security, please rotate your provider credentials.",
        style = MaterialTheme.typography.bodyMedium,
      )
      TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Dismiss") }
    }
  }
}

/**
 * Skeleton loading placeholder for settings sections. Displays animated placeholder while settings
 * load.
 */
@Composable
internal fun SettingsSectionSkeleton(modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
      ),
    shape = RoundedCornerShape(12.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header skeleton
      Box(
        modifier =
          Modifier.fillMaxWidth(0.6f)
            .height(16.dp)
            .background(
              MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
              shape = RoundedCornerShape(4.dp),
            )
      )
      // Content skeletons
      repeat(2) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(12.dp)
              .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(4.dp),
              )
        )
      }
    }
  }
}
