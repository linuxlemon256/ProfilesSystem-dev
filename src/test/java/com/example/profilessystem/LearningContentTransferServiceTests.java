package com.example.profilessystem;

import com.example.profilessystem.entity.LearningContentTransfer;
import com.example.profilessystem.server.LearningContentTransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Transactional
class LearningContentTransferServiceTests {
    @Autowired
    private LearningContentTransferService service;

    @Test
    void exportedLearningContentCanBeMergedBackWithoutChangingCounts() {
        Map<String, Integer> before = service.overview();
        LearningContentTransfer content = service.exportContent();

        assertEquals(LearningContentTransfer.FORMAT, content.format());
        assertFalse(content.professionalCategories().isEmpty());

        LearningContentTransfer.ImportSummary summary = service.importContent(content);

        assertEquals(content.professionalCategories().size(), summary.professionalCategories());
        assertEquals(content.specificMajors().size(), summary.specificMajors());
        assertEquals(content.learningRoutes().size(), summary.learningRoutes());
        assertEquals(content.learningResources().size(), summary.learningResources());
        assertEquals(before, service.overview());
    }
}
