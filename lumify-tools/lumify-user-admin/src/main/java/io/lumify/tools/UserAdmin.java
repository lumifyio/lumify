package io.lumify.tools;

import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.User;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class UserAdmin extends CommandLineBase {
    private static final String CMD_OPT_USERNAME = "username";
    private static final String CMD_OPT_USERID = "userid";
    private static final String CMD_OPT_PRIVILEGES = "privileges";
    private static final String CMD_OPT_AS_TABLE = "as-table";
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
                        .withDescription("Comma separated list of privileges " + privilegesAsString(Privilege.ALL))
                        .hasArg()
                        .create("p")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_AS_TABLE)
                        .withDescription("List users in a table")
                        .create("t")
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
        User user = findUser(cmd);
        if (user == null) {
            printUserNotFoundError(cmd);
            return 2;
        }

        getUserRepository().delete(user);
        System.out.println("Deleted user " + user.getUserId());
        return 0;
    }

    private int list(CommandLine cmd) {
        Iterable<User> users = getUserRepository().findAll();
        if (cmd.hasOption(CMD_OPT_AS_TABLE)) {
            printUsers(users);
        } else {
            for (User user : users) {
                printUser(user);
            }
        }
        return 0;
    }

    private int setPrivileges(CommandLine cmd) {
        String privilegesString = cmd.getOptionValue(CMD_OPT_PRIVILEGES);
        Set<Privilege> privileges = null;
        if (privilegesString != null) {
            privileges = Privilege.stringToPrivileges(privilegesString);
        }

        User user = findUser(cmd);
        if (user == null) {
            printUserNotFoundError(cmd);
            return 2;
        }

        if (privileges != null) {
            System.out.println("Assigning privileges " + privileges + " to user " + user.getUserId());
            getUserRepository().setPrivileges(user, privileges);
            user = getUserRepository().findById(user.getUserId());
        }

        printUser(user);
        return 0;
    }

    private User findUser(CommandLine cmd) {
        String username = cmd.getOptionValue(CMD_OPT_USERNAME);
        String userid = cmd.getOptionValue(CMD_OPT_USERID);

        User user = null;
        if (username != null) {
            user = getUserRepository().findByUsername(username);
        } else if (userid != null) {
            user = getUserRepository().findById(userid);
        }
        return user;
    }

    private void printUser(User user) {
        String emailAddress = user.getEmailAddress();
        emailAddress = emailAddress != null ? emailAddress : "";
        Date currentLoginDate = user.getCurrentLoginDate();
        String currentLoginDateString = currentLoginDate != null ? currentLoginDate.toString() : "";
        String currentLoginRemoteAddr = user.getCurrentLoginRemoteAddr();
        currentLoginRemoteAddr = currentLoginRemoteAddr != null ? currentLoginRemoteAddr : "";
        Date previousLoginDate = user.getPreviousLoginDate();
        String previousLoginDateString = previousLoginDate != null ? previousLoginDate.toString() : "";
        String previousLoginRemoteAddr = user.getPreviousLoginRemoteAddr();
        previousLoginRemoteAddr = previousLoginRemoteAddr != null ? previousLoginRemoteAddr : "";

        System.out.println("                        ID: " + user.getUserId());
        System.out.println("                  Username: " + user.getUsername());
        System.out.println("            E-Mail Address: " + emailAddress);
        System.out.println("              Display Name: " + user.getDisplayName());
        System.out.println("        Current Login Date: " + currentLoginDateString);
        System.out.println(" Current Login Remote Addr: " + currentLoginRemoteAddr);
        System.out.println("       Previous Login Date: " + previousLoginDateString);
        System.out.println("Previous Login Remote Addr: " + previousLoginRemoteAddr);
        System.out.println("               Login Count: " + user.getLoginCount());
        System.out.println("                Privileges: " + privilegesAsString(getUserRepository().getPrivileges(user)));
        System.out.println("");
    }

    private void printUsers(Iterable<User> users) {
        if (users != null) {
            int maxIdWidth = 1;
            int maxUsernameWidth = 1;
            int maxEmailAddressWidth = 1;
            int maxDisplayNameWidth = 1;
            int maxLoginCountWidth = 1;
            int maxPrivilegesWith = privilegesAsString(Privilege.ALL).length();
            for (User user : users) {
                String id = user.getUserId();
                int idWidth = id != null ? id.length() : 0;
                maxIdWidth = idWidth > maxIdWidth ? idWidth : maxIdWidth;
                String username = user.getUsername();
                int usernameWidth = username != null ? username.length() : 0;
                maxUsernameWidth = usernameWidth > maxUsernameWidth ? usernameWidth : maxUsernameWidth;
                String emailAddress = user.getEmailAddress();
                int emailAddressWidth = emailAddress != null ? emailAddress.length() : 0;
                maxEmailAddressWidth = emailAddressWidth > maxEmailAddressWidth ? emailAddressWidth : maxEmailAddressWidth;
                String displayName = user.getDisplayName();
                int displayNameWidth = displayName != null ? displayName.length() : 0;
                maxDisplayNameWidth = displayNameWidth > maxDisplayNameWidth ? displayNameWidth : maxDisplayNameWidth;
                int loginCount = user.getLoginCount();
                int loginCountWidth = Integer.toString(loginCount).length();
                maxLoginCountWidth = loginCountWidth > maxLoginCountWidth ? loginCountWidth : maxLoginCountWidth;
            }
            String format = String.format("%%%ds %%%ds %%%ds %%%ds %%%dd %%%ds%%n", -1 * maxIdWidth, -1 * maxUsernameWidth, -1 * maxEmailAddressWidth, -1 * maxDisplayNameWidth, maxLoginCountWidth, -1 * maxPrivilegesWith);
            for (User user : users) {
                String emailAddress = user.getEmailAddress();
                emailAddress = emailAddress != null ? emailAddress : "";
                System.out.printf(format,
                        user.getUserId(),
                        user.getUsername(),
                        emailAddress,
                        user.getDisplayName(),
                        user.getLoginCount(),
                        privilegesAsString(getUserRepository().getPrivileges(user))
                );
            }
        } else {
            System.out.println("No users");
        }
    }

    private String privilegesAsString(Set<Privilege> privileges) {
        return privileges.toString().replaceAll(" ", "");
    }

    private void printUserNotFoundError(CommandLine cmd) {
        String username = cmd.getOptionValue(CMD_OPT_USERNAME);
        if (username != null) {
            System.out.println("No user found with username: " + username);
            return;
        }

        String userid = cmd.getOptionValue(CMD_OPT_USERID);
        if (userid != null) {
            System.out.println("No user found with userid: " + userid);
            return;
        }
    }

}
