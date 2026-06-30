package com.aisandbox.audit.controller;

import com.aisandbox.audit.dto.AuditLogFilter;
import com.aisandbox.audit.dto.PagedResponse;
import com.aisandbox.audit.model.AuditLog;
import com.aisandbox.audit.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogV2ControllerTest {

	private AuditLogService auditLogService;
	private AuditLogV2Controller controller;

	@BeforeEach
	void setUp() {
		auditLogService = mock(AuditLogService.class);
		controller = new AuditLogV2Controller(auditLogService);
	}

	@Test
	void findAll_returnsTheServicePageAsAPagedResponse() {
		AuditLog row = new AuditLog("User", "CREATE", "details");
		Pageable pageable = PageRequest.of(0, 20);
		when(auditLogService.search(any(AuditLogFilter.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(row), pageable, 1));

		PagedResponse<AuditLog> response = controller.findAll(false, pageable);

		assertThat(response.content()).containsExactly(row);
		assertThat(response.totalElements()).isEqualTo(1);
		assertThat(response.last()).isTrue();
	}

	@Test
	void findAll_forwardsIncludeDeletedToTheServiceFilter() {
		Pageable pageable = PageRequest.of(0, 20);
		when(auditLogService.search(any(AuditLogFilter.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		controller.findAll(true, pageable);

		verify(auditLogService).search(eq(new AuditLogFilter(null, null, null, null, true)), eq(pageable));
	}
}
