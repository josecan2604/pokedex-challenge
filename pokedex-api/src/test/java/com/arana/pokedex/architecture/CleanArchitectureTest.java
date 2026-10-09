package com.arana.pokedex.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Enforces the dependency rule: source code dependencies only point inwards.
 * <pre>
 *   infrastructure (web, persistence, pokeapi, security, config)
 *        -> application (use cases, ports)
 *             -> domain (models, rules)
 * </pre>
 */
@AnalyzeClasses(packages = "com.arana.pokedex", importOptions = ImportOption.DoNotIncludeTests.class)
class CleanArchitectureTest {

    @ArchTest
    static final ArchRule layersRespectTheDependencyRule = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    @ArchTest
    static final ArchRule domainAndApplicationAreFrameworkFree = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.servlet..",
                    "com.fasterxml.jackson..", "org.hibernate..")
            .because("business rules must not depend on frameworks, the web or the database");

    @ArchTest
    static final ArchRule adaptersDoNotTalkToEachOther = noClasses()
            .that().resideInAPackage("..infrastructure.web..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure.persistence..", "..infrastructure.pokeapi..")
            .because("controllers go through use cases, never straight to a repository or HTTP client");
}
