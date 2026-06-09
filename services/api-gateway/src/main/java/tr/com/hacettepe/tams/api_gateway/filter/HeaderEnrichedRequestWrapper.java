package tr.com.hacettepe.tams.api_gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Servlet request wrapper that injects X-User-Id and X-User-Role headers so
 * downstream services can trust the caller's identity without re-validating the JWT.
 *
 * <p>All original headers are preserved; the injected headers silently overwrite any
 * client-supplied values with the same names, preventing header-spoofing attacks.
 */
public class HeaderEnrichedRequestWrapper extends HttpServletRequestWrapper {

    static final String HEADER_USER_ID = "X-User-Id";
    static final String HEADER_USER_ROLE = "X-User-Role";

    private final String userId;
    private final String role;

    HeaderEnrichedRequestWrapper(HttpServletRequest request, String userId, String role) {
        super(request);
        this.userId = userId;
        this.role = role;
    }

    @Override
    public String getHeader(String name) {
        if (HEADER_USER_ID.equalsIgnoreCase(name)) {
            return userId;
        }
        if (HEADER_USER_ROLE.equalsIgnoreCase(name)) {
            return role;
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (HEADER_USER_ID.equalsIgnoreCase(name)) {
            return Collections.enumeration(Collections.singletonList(userId));
        }
        if (HEADER_USER_ROLE.equalsIgnoreCase(name)) {
            return Collections.enumeration(Collections.singletonList(role));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>();
        Enumeration<String> original = super.getHeaderNames();
        if (original != null) {
            while (original.hasMoreElements()) {
                names.add(original.nextElement());
            }
        }
        names.add(HEADER_USER_ID);
        names.add(HEADER_USER_ROLE);
        return Collections.enumeration(names);
    }
}
