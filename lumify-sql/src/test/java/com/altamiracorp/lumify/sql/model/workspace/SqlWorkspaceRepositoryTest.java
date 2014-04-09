package com.altamiracorp.lumify.sql.model.workspace;

import com.altamiracorp.lumify.core.exception.LumifyAccessDeniedException;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceAccess;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceUser;
import com.altamiracorp.lumify.sql.model.user.SqlUser;
import com.altamiracorp.lumify.sql.model.user.SqlUserRepository;
import com.altamiracorp.securegraph.util.IterableUtils;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SqlWorkspaceRepositoryTest {
    private final String HIBERNATE_IN_MEM_CFG_XML = "hibernateInMem.cfg.xml";
    private SqlWorkspaceRepository sqlWorkspaceRepository;
    private static org.hibernate.cfg.Configuration configuration;
    private static SessionFactory sessionFactory;
    private SqlUserRepository sqlUserRepository;

    private SqlUser testUser;

    @Before
    public void setUp() throws Exception {
        configuration = new org.hibernate.cfg.Configuration();
        configuration.configure(HIBERNATE_IN_MEM_CFG_XML);
        ServiceRegistry serviceRegistryBuilder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
        sessionFactory = configuration.buildSessionFactory(serviceRegistryBuilder);
        sqlUserRepository = new SqlUserRepository();
        sqlUserRepository.setSessionFactory(sessionFactory);
        sqlWorkspaceRepository = new SqlWorkspaceRepository();
        sqlWorkspaceRepository.setSessionFactory(sessionFactory);
        sqlWorkspaceRepository.setUserRepository(sqlUserRepository);

        testUser = (SqlUser)sqlUserRepository.addUser("123", "user 1", null, new String[0]);
    }

    @Test
    public void testDelete() throws Exception {
        SqlWorkspace workspace = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace 1", testUser);
        sqlWorkspaceRepository.delete(workspace, testUser);

        assertNull(sqlWorkspaceRepository.findById("1", testUser));
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testDeleteWithUserPermissions() {
        SqlWorkspace workspace = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace 1", testUser);

        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(2);
        sqlWorkspaceRepository.delete(workspace, sqlUser);
    }

    @Test
    public void testAdd() throws Exception {
        SqlWorkspace workspace = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace", testUser);
        assertEquals("1", workspace.getCreator().getUserId());
        assertEquals("test workspace", workspace.getDisplayTitle());
        assertEquals(1, workspace.getSqlWorkspaceUser().size());

        SqlWorkspaceUser sqlWorkspaceUser = workspace.getSqlWorkspaceUser().iterator().next();
        assertEquals(WorkspaceAccess.WRITE.toString(), sqlWorkspaceUser.getWorkspaceAccess());
        assertEquals("1", sqlWorkspaceUser.getWorkspace().getId());
    }

    @Test
    public void testFindAll() throws Exception {
        Iterable<Workspace> userIterable = sqlWorkspaceRepository.findAll(testUser);
        assertTrue(IterableUtils.count(userIterable) == 0);

        sqlWorkspaceRepository.add("test workspace 1", testUser);
        sqlWorkspaceRepository.add("test workspace 2", testUser);
        sqlWorkspaceRepository.add("test workspace 3", testUser);
        userIterable = sqlWorkspaceRepository.findAll(testUser);
        assertTrue(IterableUtils.count(userIterable) == 3);
    }

    @Test
    public void testSetTitle() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        assertEquals("test", sqlWorkspace.getDisplayTitle());
        sqlWorkspaceRepository.setTitle(sqlWorkspace, "changed title", testUser);
        SqlWorkspace modifiedWorkspace = (SqlWorkspace) sqlWorkspaceRepository.findById("1", testUser);
        assertEquals("changed title", modifiedWorkspace.getDisplayTitle());
    }

    @Test(expected = LumifyException.class)
    public void testSetTitleWithInvalidWorkspace() {
        sqlWorkspaceRepository.setTitle(new SqlWorkspace(), "test", new SqlUser());
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testSetTitleWithoutUserPermissions() {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        SqlUser sqlUser1 = new SqlUser();
        sqlUser1.setId(2);
        sqlWorkspaceRepository.setTitle(sqlWorkspace, "test", sqlUser1);
    }


    @Test
    public void testFindUsersWithAccess() throws Exception {
        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(2);
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", sqlUser);

        List<WorkspaceUser> workspaceUsers = sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace, sqlUser);
        assertFalse(workspaceUsers.isEmpty());
        assertEquals("2", workspaceUsers.get(0).getUserId());
        assertTrue(workspaceUsers.get(0).isCreator());
    }

    @Test(expected = LumifyException.class)
    public void testFindUsersWithAccessWithInvalidWorkspace() {
        sqlWorkspaceRepository.findUsersWithAccess(new SqlWorkspace(), new SqlUser());
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testFindUsersWithAccessWithoutUserPermissions() {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(2);
        sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace, sqlUser);
    }

    @Test
    public void testUpdateUserOnWorkspace() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "1", WorkspaceAccess.WRITE, testUser);
        sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.findById("1", testUser);
        sqlWorkspace.getSqlWorkspaceUser();
        assertTrue(sqlWorkspaceRepository.doesUserHaveWriteAccess(sqlWorkspace, testUser));

        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "1", WorkspaceAccess.NONE, testUser);
        sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.findById("1", testUser);
        assertFalse(sqlWorkspaceRepository.doesUserHaveWriteAccess(sqlWorkspace, testUser));
        assertFalse(sqlWorkspaceRepository.doesUserHaveReadAccess(sqlWorkspace, testUser));

        sqlUserRepository.addUser("456", "qwe", "", new String[0]);
        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "2", WorkspaceAccess.WRITE, testUser);
        sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.findById("1", testUser);
        assertTrue(sqlWorkspaceRepository.doesUserHaveWriteAccess(sqlWorkspace, testUser));
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testUpdateUserOnWorkspaceWithoutPermissions() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", new SqlUser());

        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(2);
        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "2", WorkspaceAccess.WRITE, sqlUser);
    }
}
