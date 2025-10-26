package com.vjaykrsna.nanoai.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/**
 * DataStore-based storage for privacy preferences.
 *
 * Provides reactive Flow-based access to privacy settings and consent tracking. Uses Preferences
 * DataStore for simple key-value storage.
 */
class PrivacyPreferenceStore @Inject constructor(@ApplicationContext private val context: Context) {
  companion object {
    private val Context.dataStore: DataStore<Preferences> by
      preferencesDataStore(name = "privacy_preferences")

    private val KEY_EXPORT_WARNINGS_DISMISSED = booleanPreferencesKey("export_warnings_dismissed")
    private val KEY_TELEMETRY_OPT_IN = booleanPreferencesKey("telemetry_opt_in")
    private val KEY_CONSENT_ACKNOWLEDGED_AT = longPreferencesKey("consent_acknowledged_at")
    private val KEY_RETENTION_POLICY = stringPreferencesKey("retention_policy")
    private val KEY_DISCLAIMER_SHOWN_COUNT = intPreferencesKey("disclaimer_shown_count")
  }

  /** Flow of current privacy preferences. Emits whenever preferences change. */
  val privacyPreference: Flow<PrivacyPreference> =
    context.dataStore.data.map { preferences ->
      PrivacyPreference(
        exportWarningsDismissed = preferences[KEY_EXPORT_WARNINGS_DISMISSED] ?: false,
        telemetryOptIn = preferences[KEY_TELEMETRY_OPT_IN] ?: false,
        consentAcknowledgedAt =
          preferences[KEY_CONSENT_ACKNOWLEDGED_AT]?.let { Instant.fromEpochMilliseconds(it) },
        disclaimerShownCount = preferences[KEY_DISCLAIMER_SHOWN_COUNT] ?: 0,
        retentionPolicy =
          preferences[KEY_RETENTION_POLICY]?.let { RetentionPolicy.valueOf(it) }
            ?: RetentionPolicy.INDEFINITE,
      )
    }

  /** Flow exposing whether the privacy disclaimer should be shown. */
  val disclaimerExposure: Flow<DisclaimerExposureState> =
    privacyPreference.map { preference ->
      val acknowledgedAt = preference.consentAcknowledgedAt
      val acknowledged = acknowledgedAt != null
      DisclaimerExposureState(
        shouldShowDialog = !acknowledged,
        acknowledged = acknowledged,
        acknowledgedAt = acknowledgedAt,
        shownCount = preference.disclaimerShownCount,
      )
    }

  /** Update export warnings dismissed flag. */
  suspend fun setExportWarningsDismissed(dismissed: Boolean) {
    context.dataStore.edit { preferences -> preferences[KEY_EXPORT_WARNINGS_DISMISSED] = dismissed }
  }

  /** Update telemetry opt-in preference. */
  suspend fun setTelemetryOptIn(optIn: Boolean) {
    context.dataStore.edit { preferences -> preferences[KEY_TELEMETRY_OPT_IN] = optIn }
  }

  /** Update consent acknowledgment timestamp to current time. */
  suspend fun acknowledgeConsent(timestamp: Instant) {
    context.dataStore.edit { preferences ->
      preferences[KEY_CONSENT_ACKNOWLEDGED_AT] = timestamp.toEpochMilliseconds()
    }
  }

  /** Increment the disclaimer shown counter. */
  suspend fun incrementDisclaimerShown() {
    context.dataStore.edit { preferences ->
      val currentCount = preferences[KEY_DISCLAIMER_SHOWN_COUNT] ?: 0
      preferences[KEY_DISCLAIMER_SHOWN_COUNT] = currentCount + 1
    }
  }

  /** Update data retention policy. */
  suspend fun setRetentionPolicy(policy: RetentionPolicy) {
    context.dataStore.edit { preferences -> preferences[KEY_RETENTION_POLICY] = policy.name }
  }

  /** Clears the disclaimer acknowledgement timestamp and shown counter. */
  suspend fun resetDisclaimerExposure() {
    context.dataStore.edit { preferences ->
      preferences.remove(KEY_CONSENT_ACKNOWLEDGED_AT)
      preferences.remove(KEY_DISCLAIMER_SHOWN_COUNT)
    }
  }

  /** Reset all privacy preferences to defaults (for testing). */
  suspend fun reset() {
    context.dataStore.edit { preferences -> preferences.clear() }
  }
}
