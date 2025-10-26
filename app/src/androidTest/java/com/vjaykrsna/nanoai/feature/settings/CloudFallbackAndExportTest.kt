package com.vjaykrsna.nanoai.feature.settings

import com.vjaykrsna.nanoai.testing.TestEnvironmentRule
import org.junit.Ignore
import org.junit.jupiter.api.Test

@org.junit.jupiter.api.extension.ExtendWith(TestEnvironmentRule::class)
class CloudFallbackAndExportTest {
  @Ignore("Pending cloud fallback + export instrumentation after backend sync in Phase 2.")
  @Test
  fun cloudFallbackPending() {
    // TODO: Reintroduce scenario tests once real network + datastore plumbing is
    // finalized.
  }
}
