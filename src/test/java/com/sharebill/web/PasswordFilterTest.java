package com.sharebill.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PasswordFilterTest {

    private static MockFilterChain run(PasswordFilter filter, String method, String path, String password,
                                       MockHttpServletResponse res) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        if (password != null) {
            req.addHeader(PasswordFilter.HEADER, password);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
        return chain;
    }

    @Test
    void blocksApiWithoutCorrectPassword() throws Exception {
        PasswordFilter filter = new PasswordFilter("bí-mật");

        MockHttpServletResponse noHeader = new MockHttpServletResponse();
        assertNull(run(filter, "GET", "/api/groups", null, noHeader).getRequest());
        assertEquals(401, noHeader.getStatus());

        MockHttpServletResponse wrong = new MockHttpServletResponse();
        assertNull(run(filter, "GET", "/api/groups", "sai", wrong).getRequest());
        assertEquals(401, wrong.getStatus());
    }

    @Test
    void allowsCorrectPassword() throws Exception {
        PasswordFilter filter = new PasswordFilter("bí-mật");
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertNotNull(run(filter, "POST", "/api/groups", "bí-mật", res).getRequest());
        assertEquals(200, res.getStatus());
    }

    @Test
    void skipsHealthStaticFilesAndPreflight() throws Exception {
        PasswordFilter filter = new PasswordFilter("bí-mật");
        assertNotNull(run(filter, "GET", "/api/health", null, new MockHttpServletResponse()).getRequest());
        assertNotNull(run(filter, "GET", "/", null, new MockHttpServletResponse()).getRequest());
        assertNotNull(run(filter, "GET", "/group/1", null, new MockHttpServletResponse()).getRequest());
        assertNotNull(run(filter, "OPTIONS", "/api/groups", null, new MockHttpServletResponse()).getRequest());
    }

    @Test
    void disabledWhenNoPasswordConfigured() throws Exception {
        PasswordFilter filter = new PasswordFilter("");
        assertNotNull(run(filter, "GET", "/api/groups", null, new MockHttpServletResponse()).getRequest());
    }
}
