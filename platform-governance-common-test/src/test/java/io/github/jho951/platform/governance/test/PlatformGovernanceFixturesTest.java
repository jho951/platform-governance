package io.github.jho951.platform.governance.test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformGovernanceFixturesTest {
    @Test
    void sampleRequestUsesStableFixtureValues() {
        assertThat(PlatformGovernanceFixtures.sampleRequest().action()).isEqualTo("review");
        assertThat(PlatformGovernanceFixtures.sampleContext().environment()).isEqualTo("prod");
    }
}
