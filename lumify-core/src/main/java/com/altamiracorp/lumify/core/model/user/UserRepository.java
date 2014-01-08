package com.altamiracorp.lumify.core.model.user;

import java.util.Collection;
import java.util.List;

import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UserRepository extends Repository<UserRow> {
    @Inject
    public UserRepository(final ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public UserRow fromRow(Row row) {
        UserRow user = new UserRow(row.getRowKey());
        Collection<ColumnFamily> families = row.getColumnFamilies();
        for (ColumnFamily columnFamily : families) {
            String columnFamilyName = columnFamily.getColumnFamilyName();
            if (columnFamilyName.equals(UserMetadata.NAME)) {
                Collection<Column> columns = columnFamily.getColumns();
                user.addColumnFamily(new UserMetadata().addColumns(columns));
            } else {
                user.addColumnFamily(columnFamily);
            }
        }
        return user;
    }

    @Override
    public Row toRow(UserRow workspace) {
        return workspace;
    }

    @Override
    public String getTableName() {
        return UserRow.TABLE_NAME;
    }

    public UserRow findOrAddUser(String userName, com.altamiracorp.lumify.core.user.User authUser) {
        UserRow user = findByUserName(userName, authUser);
        if (user != null) {
            return user;
        }

        user = new UserRow();
        user.getMetadata().setUserName(userName);
        user.getMetadata().setUserType(UserType.USER.toString());
        save(user, authUser.getModelUserContext());
        return user;
    }

    public UserRow findByUserName(String userName, com.altamiracorp.lumify.core.user.User authUser) {
        List<UserRow> users = findAll(authUser.getModelUserContext());
        for (UserRow user : users) {
            if (userName.equals(user.getMetadata().getUserName())) {
                return user;
            }
        }
        return null;
    }
}
