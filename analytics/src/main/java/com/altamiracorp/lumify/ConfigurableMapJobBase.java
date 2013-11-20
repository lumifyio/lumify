package com.altamiracorp.lumify;

import java.util.Properties;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.util.Tool;

import com.altamiracorp.lumify.cmdline.CommandLineBase;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.bigtable.model.accumulo.AccumuloModelOutputFormat;
import com.altamiracorp.lumify.core.model.artifact.Artifact;

public abstract class ConfigurableMapJobBase extends CommandLineBase implements Tool {
    public static final String FAIL_FIRST_ERROR = "failOnFirstError";

    private Class pluginClass;
    private String[] config;
    private boolean failOnFirstError = false;

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();

        if (hasConfigurableClassname()) {
            options.addOption(
                    OptionBuilder
                            .withLongOpt("classname")
                            .withDescription("The class to run")
                            .withArgName("name")
                            .isRequired()
                            .hasArg()
                            .create()
            );
        }

        options.addOption(
                OptionBuilder
                        .withLongOpt("config")
                        .withDescription("Configuration for the class")
                        .withArgName("name=value")
                        .hasArg()
                        .create('D')
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(FAIL_FIRST_ERROR)
                        .withDescription("Enables failing on the first error that occurs")
                        .create()
        );

        return options;
    }

    @Override
    protected void processOptions(CommandLine cmd) throws Exception {
        super.processOptions(cmd);

        if (hasConfigurableClassname()) {
            String pluginClassName = cmd.getOptionValue("classname");
            if (pluginClassName == null) {
                throw new RuntimeException("'class' parameter is required");
            }
            pluginClass = loadClass(pluginClassName);
        }

        config = cmd.getOptionValues("config");
        if (cmd.hasOption(FAIL_FIRST_ERROR)) {
            failOnFirstError = true;
        }

        disableFrameworkInitialization();
    }

    protected boolean hasConfigurableClassname() {
        return true;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        Job job = new Job(getConf(), this.getClass().getSimpleName());
        Configuration configuration = getConfiguration();
        configureJob(job, configuration);

        job.setJarByClass(this.getClass());

        if (config != null) {
            for (String config : this.config) {
                String[] parts = config.split("=", 2);
                job.getConfiguration().set(parts[0], parts[1]);
            }
        }

        job.setInputFormatClass(getInputFormatClassAndInit(job));

        job.setMapOutputKeyClass(Key.class);
        job.setMapOutputValueClass(Value.class);
        job.setMapperClass(getMapperClass(job, pluginClass));

        job.setNumReduceTasks(0);

        Class<? extends OutputFormat> outputFormatClass = getOutputFormatClass();
        if (outputFormatClass != null) {
            job.setOutputFormatClass(outputFormatClass);
        }
        AccumuloModelOutputFormat.init(
                job,
                configuration.get("model.accumulo.instanceName"),
                configuration.get("model.accumulo.zookeeperServerNames"),
                configuration.get("model.accumulo.user"),
                configuration.get("model.accumulo.password"),
                Artifact.TABLE_NAME);

        job.waitForCompletion(true);
        return job.isSuccessful() ? 0 : 1;
    }

    protected abstract Class<? extends InputFormat> getInputFormatClassAndInit(Job job);

    protected Class<? extends OutputFormat> getOutputFormatClass() {
        return AccumuloModelOutputFormat.class;
    }

    protected abstract Class<? extends Mapper> getMapperClass(Job job, Class pluginClass);

    protected String[] getConfig() {
        return config;
    }

    private void configureJob(final Job job, final Configuration config) {
        for (final String key : config.getKeys()) {
            job.getConfiguration().set(key, config.get(key));
        }

        job.getConfiguration().setBoolean(FAIL_FIRST_ERROR, failOnFirstError);
    }

    private void disableFrameworkInitialization() {
        initFramework = false;
    }
}
