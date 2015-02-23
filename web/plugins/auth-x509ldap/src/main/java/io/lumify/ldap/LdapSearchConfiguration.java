package io.lumify.ldap;

import io.lumify.core.config.Configurable;
import io.lumify.core.exception.LumifyException;
import com.unboundid.ldap.sdk.SearchScope;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class LdapSearchConfiguration {
    private String userSearchBase;
    private SearchScope userSearchScope;
    private List<String> userAttributes;
    private String userSearchFilter;
    private String userCertificateAttribute;
    private String groupSearchBase;
    private SearchScope groupSearchScope;
    private String groupNameAttribute;
    private String groupSearchFilter;

    @Configurable(name = "userSearchBase")
    public void setUserSearchBase(String userSearchBase) {
        this.userSearchBase = userSearchBase;
    }

    @Configurable(name = "userSearchScope")
    public void setUserSearchScope(String userSearchScope) {
        this.userSearchScope = toSearchScope(userSearchScope);
    }

    @Configurable(name = "userAttributes")
    public void setUserAttributes(String userAttributes) {
        this.userAttributes = Arrays.asList(userAttributes.split(","));
    }

    @Configurable(name = "userSearchFilter", defaultValue = "(cn=${cn})")
    public void setUserSearchFilter(String userSearchFilter) {
        this.userSearchFilter = userSearchFilter;
    }

    @Configurable(name = "userCertificateAttribute", defaultValue = "userCertificate;binary")
    public void setUserCertificateAttribute(String userCertificateAttribute) {
        this.userCertificateAttribute = userCertificateAttribute;
    }

    @Configurable(name = "groupSearchBase")
    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    @Configurable(name = "groupSearchScope")
    public void setGroupSearchScope(String groupSearchScope) {
        this.groupSearchScope = toSearchScope(groupSearchScope);
    }

    @Configurable(name = "groupNameAttribute", defaultValue = "cn")
    public void setGroupNameAttribute(String groupNameAttribute) {
        this.groupNameAttribute = groupNameAttribute;
    }

    @Configurable(name = "groupSearchFilter", defaultValue = "(uniqueMember=${dn})")
    public void setGroupSearchFilter(String groupSearchFilter) {
        this.groupSearchFilter = groupSearchFilter;
    }

    public String getUserSearchBase() {
        return userSearchBase;
    }

    public SearchScope getUserSearchScope() {
        return userSearchScope;
    }

    public List<String> getUserAttributes() {
        return userAttributes;
    }

    public String getUserCertificateAttribute() {
        return userCertificateAttribute;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public SearchScope getGroupSearchScope() {
        return groupSearchScope;
    }

    public String getUserSearchFilter() {
        return userSearchFilter;
    }

    public String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    public String getGroupSearchFilter() {
        return groupSearchFilter;
    }

    private SearchScope toSearchScope(String searchScope) {
        try {
            Field f = SearchScope.class.getField(searchScope.toUpperCase());
            return (SearchScope) f.get(null);
        } catch (Exception e) {
            throw new LumifyException("unable to configure search scope", e);
        }
    }
}
