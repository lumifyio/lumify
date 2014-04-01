package com.altamiracorp.lumify.tools;

import com.altamiracorp.lumify.core.cmdline.CommandLineBase;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.securegraph.Graph;
import com.google.inject.Inject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class UserAdd extends CommandLineBase {
    private UserRepository userRepository;
    private String username;
    private String password;
    private String[] authorizations;
    private Graph graph;

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
                        .withLongOpt("auths")
                        .withDescription("Comma separated list of authorizations")
                        .hasArg(true)
                        .withArgName("auths")
                        .create("a")
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
        String authorizationsString = cmd.getOptionValue("auths");
        this.authorizations = new String[0];
        if (authorizationsString != null && authorizationsString.length() > 0) {
            this.authorizations = authorizationsString.split(",");
        }
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

        User user = this.userRepository.findByDisplayName(this.username);

        if (cmd.hasOption("reset")) {
            if (user == null) {
                System.err.println("password reset requested but user not found");
                return 4;
            }
            this.userRepository.setPassword(user, this.password);
            System.out.println("User password reset: " + user.getUserId());
        } else {
            if (user != null) {
                System.err.println("username already exists");
                return 3;
            }
            user = this.userRepository.addUser(graph.getIdGenerator().nextId().toString(), this.username, this.password, this.authorizations);
            System.out.println("User added: " + user.getUserId());
        }

        return 0;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setGraph (Graph graph) { this.graph = graph; }
}
