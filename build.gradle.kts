import groovy.util.Node

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.9.10"
    id("net.minecrell.licenser") version "0.3"
}

group = "eu.mikroskeem"
version = "0.0.1-SNAPSHOT"
description = "A Gradle plugin to remap Java source code like one would remap Java bytecode"
val url = "https://github.com/mikroskeem/SourceTransformer"

val inriaSpoonCoreVersion = "6.2.0"

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("fr.inria.gforge.spoon:spoon-core:$inriaSpoonCoreVersion")
}

license {
    header = rootProject.file("etc/HEADER")
    filter.include("**/*.kt")
}

gradlePlugin {
    (plugins) {
        "sourcetransformer" {
            id = "eu.mikroskeem.sourcetransformer"
            implementationClass = "eu.mikroskeem.sourcetransformer.SourceTransformer"
        }
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(java.sourceSets["main"].allJava)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)

            pom.withXml {
                builder {
                    "repositories" {
                        "repository" {
                            "id"("mikroskeem-repo")
                            "url"("https://repo.wut.ee/repository/mikroskeem-repo")
                        }
                    }
                }
            }
        }
    }

    repositories {
        mavenLocal()


        if(rootProject.hasProperty("wutRepoUsername") && rootProject.hasProperty("wutRepoPassword")) {
            maven {
                credentials {
                    username = rootProject.properties["wutRepoUsername"]!! as String
                    password = rootProject.properties["wutRepoPassword"]!! as String
                }

                name = "mikroskeem-repo"
                setUrl("https://repo.wut.ee/repository/mikroskeem-repo")
            }
        }
    }
}

val wrapper = tasks.creating(Wrapper::class) {
    gradleVersion = "4.6"
}

fun XmlProvider.builder(builder: GroovyBuilderScope.() -> Unit) {
    (asNode().children().last() as Node).plus(delegateClosureOf<Any> {
        withGroovyBuilder(builder)
    })
}