package cool.klass.dropwizard.bundle.reladomo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.auto.service.AutoService;
import com.gs.fw.common.mithra.MithraManager;
import com.gs.fw.common.mithra.MithraManagerProvider;
import cool.klass.dropwizard.bundle.prioritized.PrioritizedBundle;
import cool.klass.dropwizard.configuration.reladomo.ReladomoFactory;
import cool.klass.dropwizard.configuration.reladomo.ReladomoFactoryProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(PrioritizedBundle.class)
public class ReladomoBundle implements PrioritizedBundle<ReladomoFactoryProvider>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ReladomoBundle.class);

    @Override
    public int getPriority()
    {
        return -3;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap)
    {
    }

    @Override
    public void run(@Nonnull ReladomoFactoryProvider configuration, @Nonnull Environment environment)
    {
        LOGGER.info("Running {}.", ReladomoBundle.class.getSimpleName());

        ReladomoFactory reladomoFactory = configuration.getReladomoFactory();

        Duration     transactionTimeout         = reladomoFactory.getTransactionTimeout();
        int          transactionTimeoutSeconds  = Math.toIntExact(transactionTimeout.toSeconds());
        List<String> runtimeConfigurationPaths  = reladomoFactory.getRuntimeConfigurationPaths();
        boolean      enableRetrieveCountMetrics = reladomoFactory.isEnableRetrieveCountMetrics();

        boolean captureTransactionLevelPerformanceData =
                reladomoFactory.isCaptureTransactionLevelPerformanceData();

        setTransactionTimeout(transactionTimeoutSeconds);
        // Notification should be configured here. Refer to notification/Notification.html under reladomo-javadoc.jar.
        loadRuntimeConfigurations(runtimeConfigurationPaths);

        setCaptureTransactionLevelPerformanceData(captureTransactionLevelPerformanceData);

        if (enableRetrieveCountMetrics)
        {
            ReladomoBundle.registerRetrieveCountMetrics(environment);
        }

        LOGGER.info("Completing {}.", ReladomoBundle.class.getSimpleName());
    }

    private static void registerRetrieveCountMetrics(@Nonnull Environment environment)
    {
        environment.metrics().gauge(
                "Reladomo database retrieve count",
                () -> () -> MithraManagerProvider.getMithraManager().getDatabaseRetrieveCount());
        environment.metrics().gauge(
                "Reladomo remote retrieve count",
                () -> () -> MithraManagerProvider.getMithraManager().getRemoteRetrieveCount());
    }

    private static void setTransactionTimeout(int transactionTimeoutSeconds)
    {
        MithraManager mithraManager = MithraManagerProvider.getMithraManager();
        mithraManager.setTransactionTimeout(transactionTimeoutSeconds);
    }

    private static void setCaptureTransactionLevelPerformanceData(boolean captureTransactionLevelPerformanceData)
    {
        MithraManager mithraManager = MithraManagerProvider.getMithraManager();
        mithraManager.setCaptureTransactionLevelPerformanceData(captureTransactionLevelPerformanceData);
    }

    private static void loadRuntimeConfigurations(@Nonnull List<String> runtimeConfigurationPaths)
    {
        runtimeConfigurationPaths.forEach(ReladomoBundle::loadRuntimeConfiguration);
    }

    private static void loadRuntimeConfiguration(String runtimeConfigurationPath)
    {
        LOGGER.info("Loading Reladomo configuration XML: {}", runtimeConfigurationPath);
        try (
                InputStream inputStream = ReladomoBundle.class.getClassLoader()
                        .getResourceAsStream(runtimeConfigurationPath))
        {
            MithraManagerProvider.getMithraManager().readConfiguration(inputStream);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}