import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellij)
    alias(libs.plugins.spotless)
    alias(libs.plugins.idea.ext)
}

group = "com.japaTestRunner"
version = System.getenv("VERSION") ?: "0.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure Java toolchain to automatically use Java 21
// Gradle will auto-download if not available
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

intellij {
    version.set("2024.1")
    type.set("IU") // IntelliJ IDEA Ultimate for full JavaScript support
    plugins.set(listOf("JavaScript", "NodeJS"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("253.*")
    }

    // Disable searchable options building as plugin doesn't expose custom settings
    buildSearchableOptions {
        enabled = false
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

// Spotless configuration for Kotlin formatting
spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.0.1")
            .editorConfigOverride(
                mapOf(
                    "indent_size" to "4",
                    "max_line_length" to "120",
                ),
            )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.0.1")
    }
}

// Git hooks installation task
val installGitHooks by tasks.registering {
    group = "git"
    description = "Installs git commit-msg hook for conventional commits validation"

    doLast {
        val hooksDir = file(".git/hooks")
        if (!hooksDir.exists()) {
            println("No .git/hooks directory found. Skipping git hooks installation.")
            return@doLast
        }

        // Create commit-msg hook for conventional commits
        val commitMsgHook = file(".git/hooks/commit-msg")
        commitMsgHook.writeText(
            """
            #!/bin/sh
            # Conventional Commits validation

            commit_msg_file=${'$'}1
            commit_msg=${'$'}(cat "${'$'}commit_msg_file")

            # Skip merge commits
            if echo "${'$'}commit_msg" | grep -qE "^Merge branch"; then
                exit 0
            fi

            # Check conventional commit format
            if ! echo "${'$'}commit_msg" | grep -qE "^(feat|fix|docs|style|refactor|test|chore|perf|ci|build|revert)(\(.+\))?!?: .+"; then
                echo "❌ Commit message does not follow Conventional Commits format!"
                echo ""
                echo "Format: <type>(<scope>): <subject>"
                echo ""
                echo "Types: feat, fix, docs, style, refactor, test, chore, perf, ci, build, revert"
                echo "Example: feat(parser): add support for arrays"
                exit 1
            fi
            """.trimIndent(),
        )
        commitMsgHook.setExecutable(true)
        println("✓ Installed commit-msg hook for conventional commits")
    }
}

// Automatically install git hooks after project setup
tasks.named("prepareKotlinBuildScriptModel") {
    dependsOn(installGitHooks)
}

// IDE integration: Automatically applies Spotless formatting after Gradle sync
idea {
    project.settings.taskTriggers.afterSync("spotlessApply", "installGitHooks")
}
