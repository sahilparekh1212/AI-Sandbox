package com.aisandbox.audit.controller;

import com.aisandbox.audit.exception.ResourceNotFoundException;
import com.aisandbox.audit.model.AuditLog;
import com.aisandbox.audit.ratelimit.TransactionalRequestExecutor;
import com.aisandbox.audit.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "Audit log operations (immutable — no update)")
public class AuditLogController {

	private static final Logger log = LoggerFactory.getLogger(AuditLogController.class);

	private final AuditLogRepository auditLogRepository;
	// Wraps mutations so a superseding request rolls the work back transactionally.
	private final TransactionalRequestExecutor txExecutor;

	public AuditLogController(AuditLogRepository auditLogRepository, TransactionalRequestExecutor txExecutor) {
		this.auditLogRepository = auditLogRepository;
		this.txExecutor = txExecutor;
	}

	@GetMapping
	@Operation(summary = "List all audit logs")
	public List<AuditLog> findAll() {
		log.info("Fetching all audit logs");
		return auditLogRepository.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get audit log by ID")
	public AuditLog findById(@PathVariable Long id) {
		log.info("Fetching audit log id={}", id);
		return auditLogRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Audit log not found: " + id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a new audit log entry")
	public AuditLog create(@RequestBody AuditLog auditLog) {
		AuditLog saved = txExecutor.run(() -> auditLogRepository.save(auditLog));
		log.info("Created audit log id={} action={}", saved.getId(), saved.getAction());
		return saved;
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete an audit log entry")
	public void delete(@PathVariable Long id) {
		txExecutor.run(() -> {
			if (!auditLogRepository.existsById(id)) {
				throw new ResourceNotFoundException("Audit log not found: " + id);
			}
			auditLogRepository.deleteById(id);
			return null;
		});
		log.info("Deleted audit log id={}", id);
	}

	@GetMapping("/health")
	@Operation(summary = "Health check")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("audit-service OK");
	}

}
