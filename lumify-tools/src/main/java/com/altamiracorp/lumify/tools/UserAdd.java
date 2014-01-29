package com.altamiracorp.lumify.tools;

import com.altamiracorp.lumify.core.cmdline.CommandLineBase;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.user.UserRow;
import com.google.inject.Inject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class UserAdd extends CommandLineBase {
    private UserRepository userRepository;
    private String username;
    private String password;

    public static void main(String[] args) throws Exception {
        int res = new UserAdd().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();

        options.addOption(
                OptionBuilder
                        .withLongOpt("username")
                        .withDescription("The username")
                        .hasArg(true)
                        .withArgName("username")
                        .create("u")
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("password")
                        .withDescription("The password")
                        .hasArg(true)
                        .withArgName("password")
                        .create("p")
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("reset")
                        .withDescription("If the password should be reset")
                        .create("r")
        );

        return options;
    }

    @Override
    protected void processOptions(CommandLine cmd) throws Exception {
        super.processOptions(cmd);
        this.username = cmd.getOptionValue("username");
        this.password = cmd.getOptionValue("password");
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        if (this.username == null || this.username.length() == 0) {
            System.err.println("Invalid username");
            return 1;
        }
        if (this.password == null || this.password.length() == 0) {
            System.err.println("Invalid password");
            return 2;
        }

        System.out.println("Adding user: " + this.username);

        UserRow user = this.userRepository.findByUserName(this.username, getUserProvider().getSystemUser());

        if (cmd.hasOption("reset")) {
            if (user == null) {
                System.err.println("password reset requested but user not found");
                return 4;
            }
            user.setPassword(this.password);
            this.userRepository.save(user, getUserProvider().getSystemUser().getModelUserContext());
            System.out.println("User password reset: " + user.getRowKey());
        } else {
            if (user != null) {
                System.err.println("username already exists");
                return 3;
            }
            user = this.userRepository.addUser(this.username, this.password, getUserProvider().getSystemUser());
            System.out.println("User added: " + user.getRowKey());
        }

        return 0;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
