package com.aisandbox.report.batch;

import com.aisandbox.report.model.Report;
import com.aisandbox.report.repository.ReportRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class ReportItemWriter implements ItemWriter<Report> {

	private final ReportRepository reportRepository;

	public ReportItemWriter(ReportRepository reportRepository) {
		this.reportRepository = reportRepository;
	}

	@Override
	public void write(Chunk<? extends Report> chunk) {
		// Placeholder: persist processed reports
		reportRepository.saveAll(chunk.getItems());
	}

}
