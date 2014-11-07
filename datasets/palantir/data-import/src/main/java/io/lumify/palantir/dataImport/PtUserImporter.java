package io.lumify.palantir.dataImport;

import com.google.inject.Inject;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.User;
import io.lumify.palantir.dataImport.model.PtUser;

public class PtUserImporter extends PtRowImporterBase<PtUser> {
    private UserRepository userRepository;

    protected PtUserImporter(DataImporter dataImporter) {
        super(dataImporter, PtUser.class);
    }

    @Override
    protected void processRow(PtUser row) throws Exception {
        User user = userRepository.findOrAddUser(row.getLogin(), getDisplayName(row), row.getEmail(), getPassword(row), getUserAuthorizations(row));
        getDataImporter().getUsers().put(row.getId(), user);
    }

    private String[] getUserAuthorizations(PtUser row) {
        return new String[0];
    }

    private String getPassword(PtUser row) {
        return UserRepository.createRandomPassword();
    }

    private String getDisplayName(PtUser row) {
        return row.getFirstName() + " " + row.getLastName();
    }

    @Override
    protected String getSql() {
        return "select * FROM {namespace}.PT_USER";
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
