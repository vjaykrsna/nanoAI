package com.vjaykrsna.nanoai.feature.library.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vjaykrsna.nanoai.feature.library.domain.ProviderType
import com.vjaykrsna.nanoai.feature.library.presentation.ModelLibraryTab
import com.vjaykrsna.nanoai.feature.library.presentation.model.HuggingFaceSortOption
import com.vjaykrsna.nanoai.feature.library.presentation.model.ModelSort
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.FILTER_PANEL_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.FILTER_TOGGLE_TAG
import com.vjaykrsna.nanoai.feature.library.ui.ModelLibraryUiConstants.SEARCH_FIELD_TAG

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ModelLibraryToolbar(
  tab: ModelLibraryTab,
  searchQuery: String,
  pipelineOptions: List<String>,
  selectedPipeline: String?,
  localSort: ModelSort,
  huggingFaceSort: HuggingFaceSortOption,
  localLibraryOptions: List<ProviderType>,
  selectedLocalLibrary: ProviderType?,
  huggingFaceLibraryOptions: List<String>,
  selectedHuggingFaceLibrary: String?,
  capabilityOptions: List<String>,
  selectedCapabilities: Set<String>,
  activeFilterCount: Int,
  onSearchChange: (String) -> Unit,
  onPipelineSelect: (String?) -> Unit,
  onSelectLocalSort: (ModelSort) -> Unit,
  onSelectHuggingFaceSort: (HuggingFaceSortOption) -> Unit,
  onSelectLocalLibrary: (ProviderType?) -> Unit,
  onSelectHuggingFaceLibrary: (String?) -> Unit,
  onToggleCapability: (String) -> Unit,
) {
  var filtersExpanded by rememberSaveable { mutableStateOf(false) }
  val hasFilterPanel =
    pipelineOptions.isNotEmpty() ||
      localLibraryOptions.isNotEmpty() ||
      huggingFaceLibraryOptions.isNotEmpty() ||
      capabilityOptions.isNotEmpty()

  if (!hasFilterPanel) filtersExpanded = false

  val badgeLabel =
    when {
      !hasFilterPanel || activeFilterCount <= 0 -> null
      activeFilterCount > 9 -> "9+"
      else -> activeFilterCount.toString()
    }

  Surface(shape = MaterialTheme.shapes.large, tonalElevation = 4.dp) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchChange,
          modifier = Modifier.weight(1f).testTag(SEARCH_FIELD_TAG),
          singleLine = true,
          shape = MaterialTheme.shapes.large,
          placeholder = { Text("Search models") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          trailingIcon =
            if (searchQuery.isNotBlank()) {
              {
                IconButton(onClick = { onSearchChange("") }) {
                  Icon(Icons.Filled.Close, contentDescription = "Clear search query")
                }
              }
            } else {
              null
            },
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              cursorColor = MaterialTheme.colorScheme.primary,
              focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
              unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        if (hasFilterPanel) {
          IconButton(
            modifier = Modifier.testTag(FILTER_TOGGLE_TAG),
            onClick = { filtersExpanded = !filtersExpanded },
          ) {
            BadgedBox(badge = { badgeLabel?.let { label -> Badge { Text(label) } } }) {
              Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = if (filtersExpanded) "Collapse filters" else "Expand filters",
              )
            }
          }
        }
      }

      AnimatedVisibility(visible = hasFilterPanel && filtersExpanded) {
        Surface(
          modifier = Modifier.fillMaxWidth().testTag(FILTER_PANEL_TAG),
          shape = MaterialTheme.shapes.large,
          tonalElevation = 1.dp,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            FilterSection(title = "Sort order") {
              val sortScroll = rememberScrollState()
              Row(
                modifier = Modifier.horizontalScroll(sortScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                when (tab) {
                  ModelLibraryTab.HUGGING_FACE -> {
                    HuggingFaceSortOption.entries.forEach { option ->
                      val selected = option == huggingFaceSort
                      FilterChip(
                        selected = selected,
                        label = { Text(option.label()) },
                        onClick = { if (!selected) onSelectHuggingFaceSort(option) },
                      )
                    }
                  }
                  else -> {
                    ModelSort.entries.forEach { option ->
                      val selected = option == localSort
                      FilterChip(
                        selected = selected,
                        label = { Text(option.label()) },
                        onClick = { if (!selected) onSelectLocalSort(option) },
                      )
                    }
                  }
                }
              }
            }

            if (pipelineOptions.isNotEmpty()) {
              FilterSection(title = "Pipeline") {
                val pipelineScroll = rememberScrollState()
                Row(
                  modifier = Modifier.horizontalScroll(pipelineScroll),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  FilterChip(
                    selected = selectedPipeline == null,
                    onClick = { onPipelineSelect(null) },
                    label = { Text("All pipelines") },
                  )

                  pipelineOptions.forEach { pipeline ->
                    val display = pipeline.trim()
                    val selected = selectedPipeline?.equals(display, ignoreCase = true) == true
                    FilterChip(
                      selected = selected,
                      onClick = { onPipelineSelect(if (selected) null else display) },
                      label = { Text(display) },
                    )
                  }
                }
              }
            }

            if (capabilityOptions.isNotEmpty() && tab != ModelLibraryTab.HUGGING_FACE) {
              FilterSection(title = "Capabilities") {
                val capabilityScroll = rememberScrollState()
                Row(
                  modifier = Modifier.horizontalScroll(capabilityScroll),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  capabilityOptions.forEach { capability ->
                    val display = capability.trim().replaceFirstChar { it.uppercase() }
                    val selected = selectedCapabilities.contains(capability)
                    FilterChip(
                      selected = selected,
                      onClick = { onToggleCapability(capability) },
                      label = { Text(display) },
                    )
                  }
                }
              }
            }

            val showLocalLibrary =
              tab != ModelLibraryTab.HUGGING_FACE && localLibraryOptions.isNotEmpty()
            val showHuggingFaceLibrary =
              tab == ModelLibraryTab.HUGGING_FACE && huggingFaceLibraryOptions.isNotEmpty()

            if (showLocalLibrary || showHuggingFaceLibrary) {
              FilterSection(title = "Library") {
                val libraryScroll = rememberScrollState()
                Row(
                  modifier = Modifier.horizontalScroll(libraryScroll),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  when (tab) {
                    ModelLibraryTab.HUGGING_FACE -> {
                      FilterChip(
                        selected = selectedHuggingFaceLibrary == null,
                        onClick = { onSelectHuggingFaceLibrary(null) },
                        label = { Text("All libraries") },
                      )
                      huggingFaceLibraryOptions.forEach { library ->
                        val selected = selectedHuggingFaceLibrary == library
                        FilterChip(
                          selected = selected,
                          onClick = { onSelectHuggingFaceLibrary(if (selected) null else library) },
                          label = { Text(library) },
                        )
                      }
                    }
                    else -> {
                      FilterChip(
                        selected = selectedLocalLibrary == null,
                        onClick = { onSelectLocalLibrary(null) },
                        label = { Text("All providers") },
                      )
                      localLibraryOptions.forEach { provider ->
                        val selected = selectedLocalLibrary == provider
                        FilterChip(
                          selected = selected,
                          onClick = { onSelectLocalLibrary(if (selected) null else provider) },
                          label = { Text(provider.displayName()) },
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun ModelLibraryTabs(
  selectedTab: ModelLibraryTab,
  onTabSelect: (ModelLibraryTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val tabs = ModelLibraryTab.entries
  val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
  TabRow(
    selectedTabIndex = selectedIndex,
    modifier =
      modifier.fillMaxWidth().semantics { contentDescription = "Model library navigation tabs" },
  ) {
    tabs.forEach { tab ->
      Tab(selected = tab == selectedTab, onClick = { onTabSelect(tab) }, text = { Text(tab.label) })
    }
  }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    content()
  }
}
