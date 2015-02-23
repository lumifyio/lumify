package io.lumify.ldap;

import com.unboundid.ldap.sdk.SearchResultEntry;

import java.security.cert.X509Certificate;
import java.util.Set;

public interface LdapSearchService {
    SearchResultEntry searchPeople(X509Certificate certificate);
    Set<String> searchGroups(SearchResultEntry personEntry);
}
