package io.lumify.tools;

import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.web.clientapi.model.Privilege;
import io.lumify.core.user.User;
import io.lumify.web.clientapi.model.UserStatus;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;
import org.securegraph.Authorizations;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.securegraph.util.IterableUtils.toList;

public class UserAdmin extends CommandLineBase {
    private static final String CMD_ACTION_CREATE = "create";
    private static final String CMD_ACTION_LIST = "list";
    private static final String CMD_ACTION_ACTIVE = "active";
    private static final String CMD_ACTION_UPDATE_PASSWORD = "update-password";
    private static final String CMD_ACTION_DELETE = "delete";
    private static final String CMD_ACTION_SET_PRIVILEGES = "set-privileges";
    private static final String CMD_ACTION_SET_AUTHORIZATIONS = "set-authorizations";
    private static final String CMD_ACTION_SET_DISPLAYNAME_EMAIL = "set-displayname-and-or-email";

    private static final String CMD_OPT_USERID = "userid";
    private static final String CMD_OPT_USERNAME = "username";
    private static final String CMD_OPT_PASSWORD = "password";
    private static final String CMD_OPT_PRIVILEGES = "privileges";
    private static final String CMD_OPT_AUTHORIZATIONS = "authorizations";
    private static final String CMD_OPT_DISPLAYNAME = "displayname";
    private static final String CMD_OPT_EMAIL = "email";
    private static final String CMD_OPT_IDLE = "idle";

    private static final String CMD_OPT_AS_TABLE = "as-table";

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
                        .withDescription("The id of the user to view or edit")
                        .hasArg()
                        .create("i")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_USERNAME)
                        .withDescription("The username of the user to view or edit")
                        .hasArg()
                        .create("u")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_PASSWORD)
                        .withDescription("The password value to set")
                        .hasArg()
                        .create()
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_PRIVILEGES)
                        .withDescription("Comma separated list of privileges to set, one or more of: " + privilegesAsString(Privilege.ALL) + " or NONE")
                        .hasArg()
                        .create("p")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_AUTHORIZATIONS)
                        .withDescription("Comma separated list of authorizations to set, or none")
                        .hasOptionalArg()
                        .create("a")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_DISPLAYNAME)
                        .withDescription("Display name to set")
                        .hasOptionalArg()
                        .create("d")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_EMAIL)
                        .withDescription("E-mail address to set")
                        .hasOptionalArg()
                        .create("e")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_AS_TABLE)
                        .withDescription("List users in a table")
                        .create("t")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_IDLE)
                        .withDescription("Include idle users")
                        .create()
        );

        return opts;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        List args = cmd.getArgList();

        if (args.contains(CMD_ACTION_CREATE)) {
            return create(cmd);
        }
        if (args.contains(CMD_ACTION_LIST)) {
            return list(cmd);
        }
        if (args.contains(CMD_ACTION_ACTIVE)) {
            return active(cmd);
        }
        if (args.contains(CMD_ACTION_UPDATE_PASSWORD)) {
            return updatePassword(cmd);
        }
        if (args.contains(CMD_ACTION_DELETE)) {
            return delete(cmd);
        }
        if (args.contains(CMD_ACTION_SET_PRIVILEGES)) {
            return setPrivileges(cmd);
        }
        if (args.contains(CMD_ACTION_SET_AUTHORIZATIONS)) {
            return setAuthorizations(cmd);
        }
        if (args.contains(CMD_ACTION_SET_DISPLAYNAME_EMAIL)) {
            return setDisplayNameAndOrEmail(cmd);
        }

        String actions = StringUtils.join(getActions(), " | ");
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(UserAdmin.class.getSimpleName() + " < " + actions + " >", getOptions());
        return -1;
    }

    private int create(CommandLine cmd) {
        String username = cmd.getOptionValue(CMD_OPT_USERNAME);
        String password = cmd.getOptionValue(CMD_OPT_PASSWORD);
        String displayName = cmd.getOptionValue(CMD_OPT_DISPLAYNAME);
        String emailAddress = cmd.getOptionValue(CMD_OPT_EMAIL);
        String[] authorizations = new String[]{};

        getUserRepository().addUser(username, displayName, emailAddress, password, authorizations);
        //format: addUser(username, displayName, emailAddress, password, authorizations);

        User user = getUserRepository().findByUsername(username);

        String privilegesString = cmd.getOptionValue(CMD_OPT_PRIVILEGES);
        Set<Privilege> privileges;
        if (privilegesString != null) {
            privileges = Privilege.stringToPrivileges(privilegesString);
        } else {
            privileges = new HashSet<Privilege>();
            privileges.add(Privilege.READ);
        }
        getUserRepository().setPrivileges(user, privileges);

        if (displayName != null) {
            getUserRepository().setDisplayName(user, displayName);
        }
        else {
            System.out.println("no display name provided");
        }
        if (emailAddress != null) {
            getUserRepository().setEmailAddress(user, emailAddress);
        }
        else {
            System.out.println("no email address provided");
        }

        if (username == null) {
            System.out.println("no username provided");
            //return -2;
        }
        if (password == null) {
            System.out.println("no password provided");
            //return -2;
        }
        if (authorizations == null) {
            System.out.println("no authorizations provided");
            //return -2;
        }

        printUser(user);
        return 0;
    }

    private int list(CommandLine cmd) {
        int skip = 0;
        int limit = 100;
        List<User> sortedUsers = new ArrayList<User>();
        while (true) {
            List<User> users = toList(getUserRepository().find(skip, limit));
            if (users.size() == 0) {
                break;
            }
            sortedUsers.addAll(users);
            skip += limit;
        }
        Collections.sort(sortedUsers, new Comparator<User>() {
            @Override
            public int compare(User u1, User u2) {
                Date d1 = u1.getCreateDate();
                Date d2 = u2.getCreateDate();
                if (d1 != null && d2 != null) {
                    return d1.compareTo(d2);
                }
                return 0;
            }
        });
        if (cmd.hasOption(CMD_OPT_AS_TABLE)) {
            printUsers(sortedUsers);
        } else {
            for (User user : sortedUsers) {
                printUser(user);
            }
        }
        return 0;
    }

    private int active(CommandLine cmd) {
        int skip = 0;
        int limit = 100;
        List<User> activeUsers = new ArrayList<User>();
        while (true) {
            List<User> users = toList(getUserRepository().findByStatus(skip, limit, UserStatus.ACTIVE));
            if (users.size() == 0) {
                break;
            }
            activeUsers.addAll(users);
            skip += limit;
        }
        System.out.println(activeUsers.size() + " " + UserStatus.ACTIVE + " user" + (activeUsers.size() == 1 ? "" : "s"));
        printUsers(activeUsers);

        if (cmd.hasOption(CMD_OPT_IDLE)) {
            skip = 0;
            limit = 100;
            List<User> idleUsers = new ArrayList<User>();
            while (true) {
                List<User> users = toList(getUserRepository().findByStatus(skip, limit, UserStatus.IDLE));
                if (users.size() == 0) {
                    break;
                }
                idleUsers.addAll(users);
                skip += limit;
            }
            System.out.println(idleUsers.size() + " " + UserStatus.IDLE + " user" + (idleUsers.size() == 1 ? "" : "s"));
            printUsers(idleUsers);
        }

        return 0;
    }

    private int updatePassword(CommandLine cmd) {
        User user = findUser(cmd);

        if (user == null) {
            printUserNotFoundError(cmd);
            return 2;
        }

        String password = cmd.getOptionValue(CMD_OPT_PASSWORD);
        getUserRepository().setPassword(user, password);
        printUser(user);
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

    private int setAuthorizations(CommandLine cmd) {
        String authorizationsString = cmd.getOptionValue(CMD_OPT_AUTHORIZATIONS);
        List<String> authorizations = new ArrayList<String>();
        if (authorizationsString != null && authorizationsString.length() > 0) {
            authorizations.addAll(Arrays.asList(StringUtils.split(authorizationsString, ',')));
        }

        User user = findUser(cmd);
        if (user == null) {
            printUserNotFoundError(cmd);
            return 2;
        }

        for (String auth : getUserRepository().getAuthorizations(user).getAuthorizations()) {
            if (authorizations.contains(auth)) {
                System.out.println("Keeping authorization:  " + auth);
                authorizations.remove(auth); // so we don't add it later
            } else {
                System.out.println("Removing authorization: " + auth);
                getUserRepository().removeAuthorization(user, auth);
            }
        }
        for (String auth : authorizations) {
            System.out.println("Adding authorization:   " + auth);
            getUserRepository().addAuthorization(user, auth);
        }
        System.out.println("");

        printUser(user);
        return 0;
    }

    private int setDisplayNameAndOrEmail(CommandLine cmd) {
        String displayName = cmd.getOptionValue(CMD_OPT_DISPLAYNAME);
        String emailAddress = cmd.getOptionValue(CMD_OPT_EMAIL);

        if (displayName == null && emailAddress == null) {
            System.out.println("no display name or e-mail address provided");
            return -2;
        }

        User user = findUser(cmd);
        if (user == null) {
            printUserNotFoundError(cmd);
            return 2;
        }

        if (displayName != null) {
            getUserRepository().setDisplayName(user, displayName);
        }
        if (emailAddress != null) {
            getUserRepository().setEmailAddress(user, emailAddress);
        }

        user = findUser(cmd); // reload the user so we have our new value(s)
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
        System.out.println("                        ID: " + user.getUserId());
        System.out.println("                  Username: " + user.getUsername());
        System.out.println("            E-Mail Address: " + valueOrBlank(user.getEmailAddress()));
        System.out.println("              Display Name: " + user.getDisplayName());
        System.out.println("               Create Date: " + valueOrBlank(user.getCreateDate()));
        System.out.println("        Current Login Date: " + valueOrBlank(user.getCurrentLoginDate()));
        System.out.println(" Current Login Remote Addr: " + valueOrBlank(user.getCurrentLoginRemoteAddr()));
        System.out.println("       Previous Login Date: " + valueOrBlank(user.getPreviousLoginDate()));
        System.out.println("Previous Login Remote Addr: " + valueOrBlank(user.getPreviousLoginRemoteAddr()));
        System.out.println("               Login Count: " + user.getLoginCount());
        System.out.println("                Privileges: " + privilegesAsString(getUserRepository().getPrivileges(user)));
        System.out.println("            Authorizations: " + authorizationsAsString(getUserRepository().getAuthorizations(user)));
        System.out.println("");
    }

    private void printUsers(Iterable<User> users) {
        if (users != null) {
            int maxCreateDateWidth = 1;
            int maxIdWidth = 1;
            int maxUsernameWidth = 1;
            int maxEmailAddressWidth = 1;
            int maxDisplayNameWidth = 1;
            int maxLoginCountWidth = 1;
            int maxPrivilegesWidth = privilegesAsString(Privilege.ALL).length();
            for (User user : users) {
                maxCreateDateWidth = maxWidth(user.getCreateDate(), maxCreateDateWidth);
                maxIdWidth = maxWidth(user.getUserId(), maxIdWidth);
                maxUsernameWidth = maxWidth(user.getUsername(), maxUsernameWidth);
                maxEmailAddressWidth = maxWidth(user.getEmailAddress(), maxEmailAddressWidth);
                maxDisplayNameWidth = maxWidth(user.getDisplayName(), maxDisplayNameWidth);
                maxLoginCountWidth = maxWidth(Integer.toString(user.getLoginCount()), maxLoginCountWidth);
            }
            String format = String.format("%%%ds %%%ds %%%ds %%%ds %%%ds %%%dd %%%ds%%n", -1 * maxCreateDateWidth,
                    -1 * maxIdWidth,
                    -1 * maxUsernameWidth,
                    -1 * maxEmailAddressWidth,
                    -1 * maxDisplayNameWidth,
                    maxLoginCountWidth,
                    -1 * maxPrivilegesWidth);
            for (User user : users) {
                System.out.printf(format,
                        valueOrBlank(user.getCreateDate()),
                        user.getUserId(),
                        user.getUsername(),
                        valueOrBlank(user.getEmailAddress()),
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
        SortedSet<Privilege> sortedPrivileges = new TreeSet<Privilege>(new Comparator<Privilege>() {
            @Override
            public int compare(Privilege p1, Privilege p2) {
                return p1.ordinal() - p2.ordinal();
            }
        });
        sortedPrivileges.addAll(privileges);
        return sortedPrivileges.toString().replaceAll(" ", "");
    }

    private String authorizationsAsString(Authorizations authorizations) {
        List<String> list = Arrays.asList(authorizations.getAuthorizations());
        if (list.size() > 0) {
            Collections.sort(list);
            return "[" + StringUtils.join(list, ',') + "]";
        } else {
            return "";
        }
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

    private String valueOrBlank(Object o) {
        if (o == null) {
            return "";
        } else if (o instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            return sdf.format(o);
        } else {
            return o.toString();
        }
    }

    private int maxWidth(Object o, int max) {
        int width = valueOrBlank(o).length();
        return width > max ? width : max;
    }

    private List<String> getActions() {
        List<String> actions = new ArrayList<String>();
        for (Field field : UserAdmin.class.getDeclaredFields()) {
            if (field.getName().startsWith("CMD_ACTION_")) {
                try {
                    actions.add(field.get(new UserAdmin()).toString());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return actions;
    }
}
