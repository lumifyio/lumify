package com.altamiracorp.lumify.web;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.X509Certificate;

public class DevX509AuthenticationProvider extends X509AuthenticationProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevX509AuthenticationProvider.class);

    @Inject
    public DevX509AuthenticationProvider(UserRepository userRepository) {
        super(userRepository);
    }

    @Override
    protected String getUsername(X509Certificate cert) {
        String dn = cert.getSubjectX500Principal().getName();
        try {
            return getCn(dn);
        } catch (InvalidNameException e) {
            LOGGER.error("Unable to parse CN from X509 certificate DN: " + dn);
            return null;
        }
    }

    private String getCn(String dn) throws InvalidNameException {
        LdapName ldapDN = new LdapName(dn);
        for (Rdn rdn : ldapDN.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("CN")) {
                return rdn.getValue().toString();
            }
        }
        return null;
    }
}
