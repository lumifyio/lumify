package io.lumify.tools;

import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.User;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.util.Set;

public class UserAdmin extends CommandLineBase {
    private static final String CMD_OPT_USERNAME = "username";
    private static final String CMD_OPT_PRIVILEGES = "privileges";

    public static void main(String[] args) throws Exception {
        int res = new UserAdmin().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options opts = super.getOptions();

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_USERNAME)
                        .withDescription("The username of the user you would like to view or edit")
                        .hasArg()
                        .isRequired()
                        .create("u")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_PRIVILEGES)
                        .withDescription("Comma separated list of privileges " + Privilege.ALL.toString().replaceAll(" ", ""))
                        .hasArg()
                        .create("p")
        );

        return opts;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        String username = cmd.getOptionValue(CMD_OPT_USERNAME);
        String privilegesString = cmd.getOptionValue(CMD_OPT_PRIVILEGES);
        Set<Privilege> privileges = null;
        if (privilegesString != null) {
            privileges = Privilege.stringToPrivileges(privilegesString);
        }

        User user = getUserRepository().findByUsername(username);
        if (user == null) {
            System.err.println("User " + username + " not found");
            return 2;
        }

        if (privileges != null) {
            System.out.println("Assigning privileges " + privileges + " to user " + username);
            getUserRepository().setPrivileges(user, privileges);
            user = getUserRepository().findByUsername(username);
        }

        System.out.println("ID: " + user.getUserId());
        System.out.println("Current Workspace Id: " + user.getCurrentWorkspaceId());
        System.out.println("Display Name: " + user.getDisplayName());
        System.out.println("Status: " + user.getUserStatus());
        System.out.println("Type: " + user.getUserType());
        System.out.println("Privileges: " + getUserRepository().getPrivileges(user).toString().replaceAll(" ", ""));

        return 0;
    }
}
