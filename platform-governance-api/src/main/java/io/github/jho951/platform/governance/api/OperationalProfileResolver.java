package io.github.jho951.platform.governance.api;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves whether the current runtime profiles should be treated as production.
 */
@FunctionalInterface
public interface OperationalProfileResolver {
    /**
     * @param activeProfiles currently active runtime profiles
     * @param productionProfiles profiles configured as production
     * @return true when any active profile matches the configured production profile set
     */
    boolean isProduction(Collection<String> activeProfiles, Collection<String> productionProfiles);

    default boolean hasActiveProfiles(Collection<String> activeProfiles) {
        return !normalize(activeProfiles).isEmpty();
    }

    default boolean isProductionName(String value, Collection<String> productionProfiles) {
        return normalize(productionProfiles).contains(normalizeValue(value));
    }

    default List<String> activeProfiles(Collection<String> activeProfiles) {
        if (activeProfiles == null) {
            return List.of();
        }
        return activeProfiles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    static OperationalProfileResolver standard() {
        return (activeProfiles, productionProfiles) -> {
            Set<String> normalizedProductionProfiles = normalize(productionProfiles);
            if (normalizedProductionProfiles.isEmpty()) {
                return false;
            }
            return normalize(activeProfiles).stream().anyMatch(normalizedProductionProfiles::contains);
        };
    }

    private static Set<String> normalize(Collection<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(OperationalProfileResolver::normalizeValue)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalizeValue(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
