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
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SqlWorkspaceRepositoryTest {
    private final String HIBERNATE_IN_MEM_CFG_XML = "hibernateInMem.cfg.xml";
    private SqlWorkspaceRepository sqlWorkspaceRepository;
    private static org.hibernate.cfg.Configuration configuration;
    private static SessionFactory sessionFactory;

    @Mock
    private SqlUserRepository sqlUserRepository;

    @Before
    public void setUp() throws Exception {
        configuration = new org.hibernate.cfg.Configuration();
        configuration.configure(HIBERNATE_IN_MEM_CFG_XML);
        ServiceRegistry serviceRegistryBuilder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
        sessionFactory = configuration.buildSessionFactory(serviceRegistryBuilder);
        sqlWorkspaceRepository = new SqlWorkspaceRepository();
        sqlWorkspaceRepository.setSessionFactory(sessionFactory);
        sqlWorkspaceRepository.setUserRepository(sqlUserRepository);
    }

    @Test
    public void testDelete() throws Exception {
        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(1);
        SqlWorkspace workspace = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace 1", sqlUser);
        sqlWorkspaceRepository.delete(workspace, sqlUser);

        assertNull(sqlWorkspaceRepository.findById("1", sqlUser));
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testDeleteWithUserPermissions() {
        SqlWorkspace workspace = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace 1", new SqlUser());

        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(2);
        sqlWorkspaceRepository.delete(workspace, sqlUser);
    }

    @Test
    public void testAdd() throws Exception {
        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(1);
        SqlWorkspace workspace = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace", sqlUser);
        assertEquals("1", sqlWorkspaceRepository.getCreatorUserId(workspace, sqlUser));
        assertEquals("test workspace", workspace.getDisplayTitle());
//        assertFalse(workspace.getUserWithWriteAccess().isEmpty());
//        assertNull(workspace.getUserWithReadAccess());

//        Set<SqlUser> sqlUsers = workspace.getUserWithWriteAccess();
//        assertEquals(sqlUser.getUserId(), sqlUsers.iterator().next().getUserId());

    }

    @Test
    public void testFindAll() throws Exception {
        Iterable<Workspace> userIterable = sqlWorkspaceRepository.findAll(new SqlUser());
        assertTrue(IterableUtils.count(userIterable) == 0);

        SqlUser user = new SqlUser();
        user.setId(1);

        sqlWorkspaceRepository.add("test workspace 1", user);
        sqlWorkspaceRepository.add("test workspace 2", user);
        sqlWorkspaceRepository.add("test workspace 3", user);
        userIterable = sqlWorkspaceRepository.findAll(user);
        assertTrue(IterableUtils.count(userIterable) == 3);
    }

    @Test
    public void testSetTitle() throws Exception {
        SqlUser sqlUser = new SqlUser();
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", sqlUser);

        assertEquals("test", sqlWorkspace.getDisplayTitle());
        sqlWorkspaceRepository.setTitle(sqlWorkspace, "changed title", sqlUser);
        SqlWorkspace modifiedWorkspace = (SqlWorkspace) sqlWorkspaceRepository.findById("1", sqlUser);
        assertEquals("changed title", modifiedWorkspace.getDisplayTitle());
    }

    @Test(expected = LumifyException.class)
    public void testSetTitleWithInvalidWorkspace() {
        sqlWorkspaceRepository.setTitle(new SqlWorkspace(), "test", new SqlUser());
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testSetTitleWithoutUserPermissions() {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", new SqlUser());

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
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", new SqlUser());

        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(2);
        sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace, sqlUser);
    }

    @Test
    public void testUpdateUserOnWorkspace() throws Exception {
        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(1);
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", sqlUser);

        SqlUser sqlUser1 = new SqlUser();
        sqlUser1.setId(2);
        when(sqlUserRepository.findById("1")).thenReturn(sqlUser1);
        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "1", WorkspaceAccess.WRITE, sqlUser);
        sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.findById("1", sqlUser);
        assertTrue(sqlWorkspaceRepository.doesUserHaveWriteAccess(sqlWorkspace, sqlUser1));

        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "1", WorkspaceAccess.NONE, sqlUser);
        sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.findById("1", sqlUser);
        assertFalse(sqlWorkspaceRepository.doesUserHaveWriteAccess(sqlWorkspace, sqlUser1));
        assertFalse(sqlWorkspaceRepository.doesUserHaveReadAccess(sqlWorkspace, sqlUser1));

        SqlUser sqlUser2 = (SqlUser) sqlUserRepository.addUser("456", "qwe", "", new String[0]);
        when(sqlUserRepository.findById("2")).thenReturn(sqlUser1);
        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "2", WorkspaceAccess.WRITE, sqlUser);
        sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.findById("1", sqlUser);
        assertTrue(sqlWorkspaceRepository.doesUserHaveWriteAccess(sqlWorkspace, sqlUser2));
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testUpdateUserOnWorkspaceWithoutPermissions() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", new SqlUser());

        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(2);
        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "2", WorkspaceAccess.WRITE, sqlUser);
    }
}
