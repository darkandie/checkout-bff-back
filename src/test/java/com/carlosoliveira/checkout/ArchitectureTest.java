package com.carlosoliveira.checkout;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.carlosoliveira.checkout", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage("..checkout.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta..",
                    "com.fasterxml..",
                    "..checkout.application..",
                    "..checkout.adapter..",
                    "..checkout.config..",
                    "..checkout.mock..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage("..checkout.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..checkout.adapter..",
                    "..checkout.config..",
                    "..checkout.mock..");

    @ArchTest
    static final ArchRule inboundAdaptersDoNotUseOutboundAdapters = noClasses()
            .that().resideInAPackage("..checkout.adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..checkout.adapter.out..");

    @ArchTest
    static final ArchRule bffNeverReadsMockServicesDirectly = noClasses()
            .that().resideOutsideOfPackage("..checkout.mock..")
            .should().dependOnClassesThat().resideInAPackage("..checkout.mock..");

    @ArchTest
    static final ArchRule controllersOnlyTalkToUseCases = noClasses()
            .that().resideInAPackage("..checkout.adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..checkout.application.port..");
}
