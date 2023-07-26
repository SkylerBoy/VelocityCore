package es.virtualplanet.velocitycore.config;

import space.arim.dazzleconf.AuxiliaryKeys;
import space.arim.dazzleconf.ConfigurationFactory;
import space.arim.dazzleconf.ConfigurationOptions;
import space.arim.dazzleconf.error.ConfigFormatSyntaxException;
import space.arim.dazzleconf.error.IllDefinedConfigException;
import space.arim.dazzleconf.error.InvalidConfigException;
import space.arim.dazzleconf.ext.snakeyaml.CommentMode;
import space.arim.dazzleconf.ext.snakeyaml.SnakeYamlConfigurationFactory;
import space.arim.dazzleconf.ext.snakeyaml.SnakeYamlOptions;
import space.arim.dazzleconf.sorter.AnnotationBasedSorter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ResourceConfigManager<C> {

    private final Path configFile;
    private final String resource;

    private final ConfigurationFactory<C> factory;
    private volatile C configData;

    public ResourceConfigManager(Path configFile, String resource, ConfigurationFactory<C> factory) {
        this.configFile = configFile;
        this.resource = resource;
        this.factory = factory;
    }

    public static <C> ResourceConfigManager<C> create(Path configFolder, String fileName, Class<C> configClass) {
        SnakeYamlOptions yamlOptions = new SnakeYamlOptions.Builder().commentMode(CommentMode.alternativeWriter()).build();
        ConfigurationOptions options = new ConfigurationOptions.Builder().sorter(new AnnotationBasedSorter()).build();
        ConfigurationFactory<C> configFactory = SnakeYamlConfigurationFactory.create(configClass, options, yamlOptions);

        return new ResourceConfigManager<>(configFolder, fileName, configFactory);
    }

    private InputStream obtainDefaultResource() {
        return getClass().getResourceAsStream("/" + resource);
    }

    private C loadDefaultsFromResource() {
        try (InputStream resourceStream = obtainDefaultResource()) {
            return factory.load(resourceStream);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (InvalidConfigException ex) {
            throw new IllDefinedConfigException("Default config resource is broken", ex);
        }
    }

    private C reloadConfigData() throws IOException, InvalidConfigException {
        Path parentDir = configFile.toAbsolutePath().getParent();

        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }

        C defaults = loadDefaultsFromResource();

        if (!Files.exists(configFile)) {
            try (FileChannel fileChannel = FileChannel.open(configFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                 InputStream defaultResource = obtainDefaultResource();
                 ReadableByteChannel defaultResourceChannel = Channels.newChannel(defaultResource)) {

                fileChannel.transferFrom(defaultResourceChannel, 0, Long.MAX_VALUE);
            }

            return defaults;
        }

        C loadedData;

        try (FileChannel fileChannel = FileChannel.open(configFile, StandardOpenOption.READ)) {
            loadedData = factory.load(fileChannel, defaults);
        }

        if (loadedData instanceof AuxiliaryKeys) {
            try (FileChannel fileChannel = FileChannel.open(configFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                factory.write(loadedData, fileChannel);
            }
        }

        return loadedData;
    }

    public void reloadConfig() {
        try {
            configData = reloadConfigData();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);

        } catch (ConfigFormatSyntaxException ex) {
            configData = loadDefaultsFromResource();
            System.err.println("The HOCON syntax in your configuration is invalid.");
            ex.printStackTrace();

        } catch (InvalidConfigException ex) {
            configData = loadDefaultsFromResource();
            System.err.println("One of the values in your configuration is not valid. "
                    + "Check to make sure you have specified the right data types.");
            ex.printStackTrace();
        }
    }

    public C getConfigData() {
        C configData = this.configData;

        if (configData == null) {
            throw new IllegalStateException("Configuration has not been loaded yet");
        }

        return configData;
    }
}
