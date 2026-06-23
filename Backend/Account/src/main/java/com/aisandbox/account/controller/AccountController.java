package com.aisandbox.account.controller;

import com.aisandbox.account.exception.ResourceNotFoundException;
import com.aisandbox.account.model.Account;
import com.aisandbox.account.ratelimit.TransactionalRequestExecutor;
import com.aisandbox.account.repository.AccountRepository;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Account CRUD operations")
public class AccountController {

	private static final Logger log = LoggerFactory.getLogger(AccountController.class);

	private final AccountRepository accountRepository;
	// Wraps mutations so a superseding request rolls the work back transactionally.
	private final TransactionalRequestExecutor txExecutor;

	public AccountController(AccountRepository accountRepository, TransactionalRequestExecutor txExecutor) {
		this.accountRepository = accountRepository;
		this.txExecutor = txExecutor;
	}

	@GetMapping
	@Operation(summary = "List all accounts")
	public List<Account> findAll() {
		log.info("Fetching all accounts");
		return accountRepository.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get account by ID")
	public Account findById(@PathVariable Long id) {
		log.info("Fetching account id={}", id);
		return accountRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a new account")
	public Account create(@RequestBody Account account) {
		Account saved = txExecutor.run(() -> accountRepository.save(account));
		log.info("Created account id={}", saved.getId());
		return saved;
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an existing account")
	public Account update(@PathVariable Long id, @RequestBody Account payload) {
		Account saved = txExecutor.run(() -> {
			Account account = accountRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
			account.setOwnerName(payload.getOwnerName());
			account.setEmail(payload.getEmail());
			return accountRepository.save(account);
		});
		log.info("Updated account id={}", id);
		return saved;
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete an account")
	public void delete(@PathVariable Long id) {
		txExecutor.run(() -> {
			if (!accountRepository.existsById(id)) {
				throw new ResourceNotFoundException("Account not found: " + id);
			}
			accountRepository.deleteById(id);
			return null;
		});
		log.info("Deleted account id={}", id);
	}

	@GetMapping("/health")
	@Operation(summary = "Health check")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok("account-service OK");
	}

}
