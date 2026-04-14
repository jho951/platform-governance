package io.github.jho951.platform.governance.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "platform.governance")
public class PlatformGovernanceProperties {
    private boolean enabled = true;
    private final Audit audit = new Audit();
    private final PolicyConfig policyConfig = new PolicyConfig();
    private final PluginPolicyEngine pluginPolicyEngine = new PluginPolicyEngine();
    private final Engine engine = new Engine();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Audit getAudit() {
        return audit;
    }

    public PolicyConfig getPolicyConfig() {
        return policyConfig;
    }

    public PluginPolicyEngine getPluginPolicyEngine() {
        return pluginPolicyEngine;
    }

    public Engine getEngine() {
        return engine;
    }

    public static class Audit {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class PolicyConfig {
        private Map<String, String> values = new LinkedHashMap<>();

        public Map<String, String> getValues() {
            return values;
        }

        public void setValues(Map<String, String> values) {
            this.values = values == null ? new LinkedHashMap<>() : values;
        }
    }

    public static class PluginPolicyEngine {
        private Store store = Store.MEMORY;
        private String filePath;
        private long cacheTtlMillis = 3000;

        public Store getStore() {
            return store;
        }

        public void setStore(Store store) {
            this.store = store == null ? Store.MEMORY : store;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public long getCacheTtlMillis() {
            return cacheTtlMillis;
        }

        public void setCacheTtlMillis(long cacheTtlMillis) {
            this.cacheTtlMillis = cacheTtlMillis < 0 ? 0 : cacheTtlMillis;
        }

        public enum Store {
            MEMORY,
            FILE
        }
    }

    public static class Engine {
        private boolean strict = false;

        public boolean isStrict() {
            return strict;
        }

        public void setStrict(boolean strict) {
            this.strict = strict;
        }
    }
}
