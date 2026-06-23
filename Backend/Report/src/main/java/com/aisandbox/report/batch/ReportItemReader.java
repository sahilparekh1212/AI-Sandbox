package com.aisandbox.report.batch;

import com.aisandbox.report.model.Report;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component
public class ReportItemReader implements ItemReader<Report> {

	private final Iterator<Report> iterator;

	public ReportItemReader() {
		// Placeholder: replace with a real data source (DB query, file, etc.)
		iterator = List.of(
			new Report("Monthly Summary", "PENDING"),
			new Report("Annual Overview", "PENDING")
		).iterator();
	}

	@Override
	public Report read() {
		return iterator.hasNext() ? iterator.next() : null;
	}

}
