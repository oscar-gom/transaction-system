package com.oscargsedas.transactionsystem.security;

import com.oscargsedas.transactionsystem.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitingFilterTest {
	private RateLimitingFilter filter;

	@BeforeEach
	void setUp() {
		RateLimitProperties properties = new RateLimitProperties();
		properties.setAuthLoginLimit(new RateLimitProperties.Limit(2, 2, 60));
		properties.setAuthRegisterLimit(new RateLimitProperties.Limit(2, 2, 60));
		properties.setAdminLimit(new RateLimitProperties.Limit(1, 1, 60));
		properties.setSearchLimit(new RateLimitProperties.Limit(1, 1, 60));
		properties.setAccountsLimit(new RateLimitProperties.Limit(3, 3, 60));
		properties.setTransactionsLimit(new RateLimitProperties.Limit(3, 3, 60));
		properties.setDefaultLimit(new RateLimitProperties.Limit(3, 3, 60));
		filter = new RateLimitingFilter(properties);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void authEndpointIsRateLimited() throws ServletException, IOException {
		MockHttpServletRequest request = buildRequest("/api/v4/auth/login");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = new CountingFilterChain();

		filter.doFilter(request, response, chain);
		assertEquals(200, response.getStatus());

		response = new MockHttpServletResponse();
		filter.doFilter(request, response, chain);
		assertEquals(200, response.getStatus());

		response = new MockHttpServletResponse();
		filter.doFilter(request, response, chain);
		assertEquals(429, response.getStatus());
	}

	@Test
	void searchEndpointIsRateLimited() throws ServletException, IOException {
		MockHttpServletRequest request = buildRequest("/api/v4/accounts/search");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = new CountingFilterChain();

		filter.doFilter(request, response, chain);
		assertEquals(200, response.getStatus());

		response = new MockHttpServletResponse();
		filter.doFilter(request, response, chain);
		assertEquals(429, response.getStatus());
	}

	@Test
	void adminEndpointIsRateLimited() throws ServletException, IOException {
		MockHttpServletRequest request = buildRequest("/api/v4/admin/promote");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = new CountingFilterChain();

		filter.doFilter(request, response, chain);
		assertEquals(200, response.getStatus());

		response = new MockHttpServletResponse();
		filter.doFilter(request, response, chain);
		assertEquals(429, response.getStatus());
	}

	@Test
	void defaultEndpointIsRateLimited() throws ServletException, IOException {
		MockHttpServletRequest request = buildRequest("/api/v4/transactions/all");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = new CountingFilterChain();

		filter.doFilter(request, response, chain);
		filter.doFilter(request, new MockHttpServletResponse(), chain);
		filter.doFilter(request, new MockHttpServletResponse(), chain);

		response = new MockHttpServletResponse();
		filter.doFilter(request, response, chain);
		assertEquals(429, response.getStatus());
	}

	private MockHttpServletRequest buildRequest(String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(uri);
		request.setRemoteAddr("127.0.0.1");
		return request;
	}

	private static class CountingFilterChain implements FilterChain {
		private final AtomicInteger calls = new AtomicInteger();

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) {
			calls.incrementAndGet();
			if (response instanceof MockHttpServletResponse mockResponse && mockResponse.getStatus() == 0) {
				mockResponse.setStatus(200);
			}
		}

		int getCalls() {
			return calls.get();
		}
	}
}
