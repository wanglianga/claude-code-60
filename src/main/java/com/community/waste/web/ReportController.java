package com.community.waste.web;

import com.community.waste.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** 月度小区治理报告。 */
    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('GOVERNANCE','PROPERTY','ADMIN')")
    public Map<String, Object> monthly(@RequestParam(required = false) String month) {
        return reportService.monthly(month == null || month.isBlank() ? YearMonth.now().toString() : month);
    }
}
