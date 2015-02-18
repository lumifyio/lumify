package io.lumify.palantir.mr.mappers;

import io.lumify.core.config.Configuration;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.UserLumifyProperties;
import io.lumify.core.model.user.UserPasswordUtil;
import io.lumify.core.model.user.UserRepository;
import io.lumify.palantir.model.PtUser;
import io.lumify.securegraph.model.user.SecureGraphUserRepository;
import io.lumify.web.clientapi.model.Privilege;
import io.lumify.web.clientapi.model.UserStatus;
import org.apache.hadoop.io.LongWritable;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

public class PtUserMapper extends PalantirMapperBase<PtUser> {
    private String privileges;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        Collection<Privilege> defaultPrivileges = Privilege.stringToPrivileges(context.getConfiguration().get(Configuration.DEFAULT_PRIVILEGES, ""));
        privileges = Privilege.toString(defaultPrivileges);
    }

    @Override
    protected void safeMap(LongWritable key, PtUser ptUser, Context context) throws Exception {
        context.setStatus(key.toString());

        Visibility visibility = SecureGraphUserRepository.VISIBILITY.getVisibility();
        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(UserRepository.createRandomPassword(), salt);

        VertexBuilder m = prepareVertex(getUserVertexId(ptUser), visibility);
        LumifyProperties.CONCEPT_TYPE.setProperty(m, SecureGraphUserRepository.USER_CONCEPT_IRI, visibility);
        UserLumifyProperties.USERNAME.setProperty(m, ptUser.getLogin(), visibility);
        UserLumifyProperties.DISPLAY_NAME.setProperty(m, getDisplayName(ptUser), visibility);
        UserLumifyProperties.CREATE_DATE.setProperty(m, new Date(ptUser.getTimeCreated()), visibility);
        UserLumifyProperties.PASSWORD_SALT.setProperty(m, salt, visibility);
        UserLumifyProperties.PASSWORD_HASH.setProperty(m, passwordHash, visibility);
        UserLumifyProperties.STATUS.setProperty(m, UserStatus.OFFLINE.toString(), visibility);
        UserLumifyProperties.AUTHORIZATIONS.setProperty(m, "", visibility);
        UserLumifyProperties.PRIVILEGES.setProperty(m, privileges, visibility);
        if (ptUser.getEmail() != null) {
            UserLumifyProperties.EMAIL_ADDRESS.setProperty(m, ptUser.getEmail(), visibility);
        }
        m.save(getAuthorizations());
    }

    public static String getUserVertexId(PtUser ptUser) {
        return getUserVertexId(ptUser.getId());
    }

    public static String getUserVertexId(long userId) {
        return ID_PREFIX +"USER_" + userId;
    }

    private String getDisplayName(PtUser ptUser) {
        return ptUser.getFirstName() + " " + ptUser.getLastName();
    }
}
