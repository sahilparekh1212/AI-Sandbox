package com.aisandbox.report.ratelimit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Runs a mutating unit of work inside a transaction that is rolled back if this
 * request is discarded (superseded) before it commits.
 *
 * <p>The {@link DiscardContext#checkpoint()} call runs <em>after</em> the work but
 * <em>inside</em> the same transaction. If a newer request for the same
 * (user, endpoint) key arrived while this one was executing, the checkpoint throws
 * {@link RequestDiscardedException}, and because the throw escapes the
 * {@code @Transactional} boundary the transaction rolls back — so a discarded
 * request leaves no partial writes behind.
 *
 * <p>Report pulls in Spring Batch, which registers its own transaction manager,
 * so we pin this to the JPA {@code transactionManager} bean to stay unambiguous.
 */
@Component
public class TransactionalRequestExecutor {

	@Transactional(transactionManager = "transactionManager", rollbackFor = RequestDiscardedException.class)
	public <T> T run(Supplier<T> work) {
		T result = work.get();
		DiscardContext.checkpoint();
		return result;
	}

}
