import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.architectury.pack200.java.Pack200Adapter
import net.fabricmc.loom.task.RemapJarTask

plugins {
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("gg.essential.loom") version "0.10.0.+"
    idea
    java
    `maven-publish`
}

val modName: String by project
val modID: String by project
val modVersion: String by project

version = modVersion
group = modID

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://repo.sk1er.club/repository/maven-public")
}

val packageLib: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    compileOnly("org.spongepowered:mixin:0.8.5")

    packageLib("gg.essential:loader-launchwrapper:1.1.3")
    implementation("gg.essential:essential-1.8.9-forge:11640+g7f637cfee")
}

sourceSets {
    main {
        output.setResourcesDir("${buildDir}/classes/kotlin/main")
    }
}

loom {
    silentMojangMappingsLicense()
    launchConfigs.getByName("client") {
        property("mixin.debug", "true")
        property("asmhelper.verbose", "true")
        arg("--tweakClass", "gg.essential.loader.stage0.EssentialSetupTweaker")
        arg("--mixin", "mixins.${modID}.json")
    }
    runConfigs {
        getByName("client") {
            isIdeConfigGenerated = true
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(Pack200Adapter())
        mixinConfig("mixins.${modID}.json")
    }
    mixin.defaultRefmapName.set("mixins.${modID}.refmap.json")
}

tasks {
    processResources {
        inputs.property("modname", modName)
        inputs.property("modid", modID)
        inputs.property("version", project.version)
        inputs.property("mcversion", "1.8.9")

        filesMatching(listOf("mcmod.info", "mixins.${modID}.json")) {
            expand(
                mapOf(
                "modname" to modName,
                "modid" to modID,
                "version" to project.version,
                "mcversion" to "1.8.9"
                )
            )
        }
        dependsOn(compileJava)
    }

    named<Jar>("jar") {
        manifest.attributes(
            "FMLCorePluginContainsFMLMod" to true,
            "FMLCorePlugin" to "${modID}.forge.FMLLoadingPlugin",
            "ForceLoadAsMod" to true,
            "MixinConfigs" to "mixins.${modID}.json",
            "ModSide" to "CLIENT",
            "TweakClass" to "gg.essential.loader.stage0.EssentialSetupTweaker",
            "TweakOrder" to "0"
        )
        dependsOn(shadowJar)
        enabled = false
    }
    named<RemapJarTask>("remapJar") {
        archiveBaseName.set(modName)
        input.set(shadowJar.get().archiveFile)
    }
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set(modName)
        archiveClassifier.set("dev")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        configurations = listOf(packageLib)
        mergeServiceFiles()
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    publishing {
        this@tasks.named("jar") {
            enabled = true
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = "FunnyMap"
                artifactId = modID
                version = project.version.toString()
                from(getComponents().getByName("java"))
            }
        }
    }
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))
kotlin.jvmToolchain(8)
