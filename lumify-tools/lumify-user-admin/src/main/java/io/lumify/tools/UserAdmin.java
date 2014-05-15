package io.lumify.tools;

import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.User;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.util.List;
import java.util.Set;

public class UserAdmin extends CommandLineBase {
    private static final String CMD_OPT_USERNAME = "username";
    private static final String CMD_OPT_USERID = "userid";
    private static final String CMD_OPT_PRIVILEGES = "privileges";
    private static final String CMD_ACTION_LIST = "list";
    private static final String CMD_ACTION_DELETE = "delete";
    private static final String CMD_ACTION_SET_PRIVILEGES = "set-privileges";

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
                        .withLongOpt(CMD_OPT_USERID)
                        .withDescription("The user id of the user you would like to view or edit")
                        .hasArg()
                        .create("i")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_USERNAME)
                        .withDescription("The username of the user you would like to view or edit")
                        .hasArg()
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
        List args = cmd.getArgList();
        if (args.contains(CMD_ACTION_LIST)) {
            return list(cmd);
        }

        if (args.contains(CMD_ACTION_DELETE)) {
            return delete(cmd);
        }

        if (args.contains(CMD_ACTION_SET_PRIVILEGES)) {
            return setPrivileges(cmd);
        }

        System.out.println(cmd.toString());
        return 0;
    }

    private int delete(CommandLine cmd) {
        String username = cmd.getOptionValue(CMD_OPT_USERNAME);
        String userid = cmd.getOptionValue(CMD_OPT_USERID);

        User user = null;
        if (username != null) {
            user = getUserRepository().findByUsername(username);
        } else if (userid != null) {
            user = getUserRepository().findById(userid);
        }

        if (user == null) {
            System.err.println("User " + username + " not found");
            return 2;
        }

        getUserRepository().delete(user);
        System.out.println("Deleted user " + user.getUserId());
        return 0;
    }

    private int list(CommandLine cmd) {
        Iterable<User> users = getUserRepository().findAll();
        for (User user : users) {
            printUser(user);
        }
        return 0;
    }

    private int setPrivileges(CommandLine cmd) {
        String username = cmd.getOptionValue(CMD_OPT_USERNAME);
        String userid = cmd.getOptionValue(CMD_OPT_USERID);

        String privilegesString = cmd.getOptionValue(CMD_OPT_PRIVILEGES);
        Set<Privilege> privileges = null;
        if (privilegesString != null) {
            privileges = Privilege.stringToPrivileges(privilegesString);
        }

        User user = null;
        if (username != null) {
            user = getUserRepository().findByUsername(username);
        } else if (userid != null) {
            user = getUserRepository().findById(userid);
        }

        if (user == null) {
            System.err.println("User " + username + " not found");
            return 2;
        }

        if (privileges != null) {
            System.out.println("Assigning privileges " + privileges + " to user " + username);
            getUserRepository().setPrivileges(user, privileges);
            user = getUserRepository().findByUsername(username);
        }

        printUser(user);

        return 0;
    }

    private void printUser(User user) {
        System.out.println("ID: " + user.getUserId());
        System.out.println("Username: " + user.getUsername());
        System.out.println("Current Workspace Id: " + user.getCurrentWorkspaceId());
        System.out.println("Display Name: " + user.getDisplayName());
        System.out.println("Status: " + user.getUserStatus());
        System.out.println("Type: " + user.getUserType());
        System.out.println("Privileges: " + getUserRepository().getPrivileges(user).toString().replaceAll(" ", ""));
        System.out.println("");
    }
}
