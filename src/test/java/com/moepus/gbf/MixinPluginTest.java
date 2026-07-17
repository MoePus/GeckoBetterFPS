package com.moepus.gbf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinPluginTest {
    @Test
    void gatesOnlyConfiguredMixins(@TempDir Path tempDir) throws Exception {
        Path path = tempDir.resolve("gbf.json");
        Files.writeString(path, """
                {
                  "enableMolangCompiler": false,
                  "enableBoneRenderPlan": false,
                  "dumpMolangCompiledClasses": false
                }
                """);
        Supplier<Path> originalPath = ConfigParser.configPath;

        try {
            ConfigParser.configPath = () -> path;
            ConfigParser.loadConfig();
            MixinPlugin plugin = new MixinPlugin();

            assertFalse(plugin.shouldApplyMixin("", "com.moepus.gbf.mixin.MolangParserMixin"));
            assertFalse(plugin.shouldApplyMixin("", "com.moepus.gbf.mixin.GeoArmorRendererMixin"));
            assertFalse(plugin.shouldApplyMixin("", "com.moepus.gbf.mixin.GeoRendererBonePlanMixin"));
            assertTrue(plugin.shouldApplyMixin("", "com.moepus.gbf.mixin.AnimationProcessorMixin"));
        } finally {
            ConfigParser.configPath = originalPath;
            ConfigParser.loadConfig();
        }
    }
}
