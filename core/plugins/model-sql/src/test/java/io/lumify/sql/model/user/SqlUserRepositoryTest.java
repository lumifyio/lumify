package io.lumify.sql.model.user;

import io.lumify.core.config.Configuration;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.model.user.UserListenerUtil;
import io.lumify.core.model.user.UserPasswordUtil;
import io.lumify.web.clientapi.model.UserStatus;
import io.lumify.core.user.User;
import io.lumify.sql.model.HibernateSessionManager;
import io.lumify.sql.model.workspace.SqlWorkspace;
import org.hibernate.Session;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.securegraph.inmemory.InMemoryGraph;
import org.securegraph.util.IterableUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class SqlUserRepositoryTest {
    private final String HIBERNATE_IN_MEM_CFG_XML = "hibernateInMem.cfg.xml";
    private SqlUserRepository sqlUserRepository;
    private static org.hibernate.cfg.Configuration configuration;

    @Mock
    private AuthorizationRepository authorizationRepository;
    private InMemoryGraph graph;
    private HibernateSessionManager sessionManager;

    @Before
    public void setup() {
        graph = InMemoryGraph.create();
        configuration = new org.hibernate.cfg.Configuration();
        configuration.configure(HIBERNATE_IN_MEM_CFG_XML);
        ServiceRegistry serviceRegistryBuilder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
        sessionManager = new HibernateSessionManager(configuration.buildSessionFactory(serviceRegistryBuilder));
        Map<?, ?> configMap = new HashMap<Object, Object>();
        Configuration lumifyConfiguration = new HashMapConfigurationLoader(configMap).createConfiguration();
        UserListenerUtil userListenerUtil = new UserListenerUtil();
        sqlUserRepository = new SqlUserRepository(lumifyConfiguration, sessionManager, authorizationRepository, userListenerUtil, graph);
    }

    @After
    public void teardown() {
        sessionManager.clearSession();
    }

    @Test
    public void testAddUser() throws Exception {
        SqlUser sqlUser1 = (SqlUser) sqlUserRepository.addUser("abc", "test user1", null, "", new String[0]);
        assertEquals("abc", sqlUser1.getUsername());
        assertEquals("test user1", sqlUser1.getDisplayName());
        assertEquals(UserStatus.OFFLINE, sqlUser1.getUserStatus());

        SqlUser sqlUser2 = (SqlUser) sqlUserRepository.addUser("def", "test user2", null, null, new String[0]);
        assertNull(sqlUser2.getPasswordHash());
        assertNull(sqlUser2.getPasswordSalt());
        assertEquals("def", sqlUser2.getUsername());
        assertEquals("test user2", sqlUser2.getDisplayName());
        assertEquals(UserStatus.OFFLINE, sqlUser2.getUserStatus());

        SqlUser sqlUser3 = (SqlUser) sqlUserRepository.addUser("ghi", "test user3", null, "&gdja81", new String[0]);
        byte[] salt = sqlUser3.getPasswordSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword("&gdja81", salt);
        assertTrue(UserPasswordUtil.validatePassword("&gdja81", salt, passwordHash));
        assertEquals("ghi", sqlUser3.getUsername());
        assertEquals("test user3", sqlUser3.getDisplayName());
        assertEquals(UserStatus.OFFLINE, sqlUser3.getUserStatus());
    }

    @Test(expected = LumifyException.class)
    public void testAddUserWithExisitingUsername() {
        sqlUserRepository.addUser("123", "test user1", null, "&gdja81", new String[0]);
        sqlUserRepository.addUser("123", "test user1", null, null, new String[0]);
    }

    @Test
    public void testFindById() throws Exception {
        SqlUser addedUser = (SqlUser) sqlUserRepository.addUser("12345", "test user", null, "&gdja81", new String[0]);
        SqlUser user = (SqlUser) sqlUserRepository.findById(addedUser.getUserId());
        byte[] salt = user.getPasswordSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword("&gdja81", salt);
        assertTrue(UserPasswordUtil.validatePassword("&gdja81", salt, passwordHash));
        assertEquals("12345", user.getUsername());
        assertEquals("test user", user.getDisplayName());
        assertEquals(user.getUserId(), addedUser.getUserId());
        assertEquals(UserStatus.OFFLINE, user.getUserStatus());

        assertNull(sqlUserRepository.findById("2"));
    }


    @Test
    public void testFindSkipLimit() throws Exception {
        Iterable<User> userIterable = sqlUserRepository.find(0, 100);
        assertTrue(IterableUtils.count(userIterable) == 0);

        sqlUserRepository.addUser("123", "test user1", null, "&gdja81", new String[0]);
        sqlUserRepository.addUser("456", "test user2", null, null, new String[0]);
        sqlUserRepository.addUser("789", "test user3", null, null, new String[0]);
        userIterable = sqlUserRepository.find(0, 100);
        assertTrue(IterableUtils.count(userIterable) == 3);
        userIterable = sqlUserRepository.find(1, 100);
        assertTrue(IterableUtils.count(userIterable) == 2);
    }

    @Test
    public void testSetPassword() throws Exception {
        SqlUser testUser = (SqlUser) sqlUserRepository.addUser("abcd", "test user", null, "1234", new String[0]);

        assertTrue(sqlUserRepository.findByUsername("abcd") != null);
        assertTrue(UserPasswordUtil.validatePassword("1234", testUser.getPasswordSalt(), testUser.getPasswordHash()));

        sqlUserRepository.setPassword(testUser, "ijk");
        assertTrue(UserPasswordUtil.validatePassword("ijk", testUser.getPasswordSalt(), testUser.getPasswordHash()));
    }

    @Test(expected = LumifyException.class)
    public void testSetPasswordWithNullUser() {
        sqlUserRepository.setPassword(null, "1u201");
    }

    @Test(expected = NullPointerException.class)
    public void testSetPasswordWithNullPassword() {
        sqlUserRepository.setPassword(new SqlUser(), null);
    }

    @Test(expected = LumifyException.class)
    public void testSetPasswordWithNoUserId() {
        sqlUserRepository.setPassword(new SqlUser(), "123");
    }

    @Test(expected = LumifyException.class)
    public void testSetPasswordWithNonExistingUser() {
        SqlUser sqlUser = new SqlUser();
        sqlUser.setUserId("1");
        sqlUserRepository.setPassword(sqlUser, "123");
    }

    @Test
    public void testIsPasswordValid() throws Exception {
        SqlUser testUser = (SqlUser) sqlUserRepository.addUser("1234", "test user", null, null, new String[0]);
        assertFalse(sqlUserRepository.isPasswordValid(testUser, ""));

        sqlUserRepository.setPassword(testUser, "abc");
        assertTrue(sqlUserRepository.isPasswordValid(testUser, "abc"));
    }

    @Test(expected = LumifyException.class)
    public void testIsPasswordValidWithNullUser() {
        sqlUserRepository.isPasswordValid(null, "1234");
    }

    @Test(expected = NullPointerException.class)
    public void testIsPasswordValidWithNullPassword() {
        sqlUserRepository.isPasswordValid(new SqlUser(), null);
    }

    @Test(expected = LumifyException.class)
    public void testIsPasswordValidWithNonExisitingUser() {
        sqlUserRepository.isPasswordValid(null, "123");
    }

    @Test
    public void testSetCurrentWorkspace() throws Exception {
        Session session = sessionManager.getSession();
        SqlWorkspace sqlWorkspace = new SqlWorkspace();
        sqlWorkspace.setWorkspaceId("WORKSPACE_1");
        sqlWorkspace.setDisplayTitle("workspace1");
        session.save(sqlWorkspace);

        User user = sqlUserRepository.addUser("123", "abc", null, null, new String[0]);
        sqlUserRepository.setCurrentWorkspace(user.getUserId(), sqlWorkspace.getWorkspaceId());
        SqlUser testUser = (SqlUser) sqlUserRepository.findById(user.getUserId());
        assertEquals("workspace1", testUser.getCurrentWorkspace().getDisplayTitle());
    }

    @Test(expected = LumifyException.class)
    public void testSetCurrentWorkspaceWithNonExisitingUser() {
        sqlUserRepository.setCurrentWorkspace("1", new SqlWorkspace().getWorkspaceId());
    }

    @Test
    public void testSetStatus() throws Exception {
        SqlUser user = (SqlUser)sqlUserRepository.addUser("123", "abc", null, null, new String[0]);
        sqlUserRepository.setStatus(user.getUserId(), UserStatus.ACTIVE);

        SqlUser testUser = (SqlUser) sqlUserRepository.findById(user.getUserId());
        assertEquals(UserStatus.ACTIVE, testUser.getUserStatus());
    }

    @Test(expected = LumifyException.class)
    public void testSetStatusWithNonExisitingUser() {
        sqlUserRepository.setStatus("1", UserStatus.OFFLINE);
    }
}
