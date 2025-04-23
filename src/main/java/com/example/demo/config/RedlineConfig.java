package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the redline service
 */
@Configuration
@ConfigurationProperties(prefix = "redline")
public class RedlineConfig {
    
    private DocumentConfig document = new DocumentConfig();
    private FileConfig file = new FileConfig();
    private ParagraphMatchingConfig paragraphMatching = new ParagraphMatchingConfig();
    
    public static class DocumentConfig {
        private String reviewerName = "AI Reviewer";
        private String reviewerInitials = "AI";

        public String getReviewerName() {
            return reviewerName;
        }

        public void setReviewerName(String reviewerName) {
            this.reviewerName = reviewerName;
        }

        public String getReviewerInitials() {
            return reviewerInitials;
        }

        public void setReviewerInitials(String reviewerInitials) {
            this.reviewerInitials = reviewerInitials;
        }
    }
    
    public static class FileConfig {
        private String originalFilePrefix = "original-";
        private String redlineFilePrefix = "redline-";
        private String cleanFilePrefix = "clean-";
        private String pdfFilePrefix = "redline-";
        private String zipFilePrefix = "redline-output-";
        
        private String redlineFilename = "redline.docx";
        private String cleanFilename = "clean.docx";
        private String pdfFilename = "redline.pdf";
        private String zipFilename = "redline-output.zip";

        public String getOriginalFilePrefix() {
            return originalFilePrefix;
        }

        public void setOriginalFilePrefix(String originalFilePrefix) {
            this.originalFilePrefix = originalFilePrefix;
        }

        public String getRedlineFilePrefix() {
            return redlineFilePrefix;
        }

        public void setRedlineFilePrefix(String redlineFilePrefix) {
            this.redlineFilePrefix = redlineFilePrefix;
        }

        public String getCleanFilePrefix() {
            return cleanFilePrefix;
        }

        public void setCleanFilePrefix(String cleanFilePrefix) {
            this.cleanFilePrefix = cleanFilePrefix;
        }

        public String getPdfFilePrefix() {
            return pdfFilePrefix;
        }

        public void setPdfFilePrefix(String pdfFilePrefix) {
            this.pdfFilePrefix = pdfFilePrefix;
        }

        public String getZipFilePrefix() {
            return zipFilePrefix;
        }

        public void setZipFilePrefix(String zipFilePrefix) {
            this.zipFilePrefix = zipFilePrefix;
        }

        public String getRedlineFilename() {
            return redlineFilename;
        }

        public void setRedlineFilename(String redlineFilename) {
            this.redlineFilename = redlineFilename;
        }

        public String getCleanFilename() {
            return cleanFilename;
        }

        public void setCleanFilename(String cleanFilename) {
            this.cleanFilename = cleanFilename;
        }

        public String getPdfFilename() {
            return pdfFilename;
        }

        public void setPdfFilename(String pdfFilename) {
            this.pdfFilename = pdfFilename;
        }

        public String getZipFilename() {
            return zipFilename;
        }

        public void setZipFilename(String zipFilename) {
            this.zipFilename = zipFilename;
        }
    }
    
    public static class ParagraphMatchingConfig {
        private double levenshteinThreshold = 0.85;
        private double embeddingThreshold = 0.92;
        private boolean useEmbeddingSearch = false;

        public double getLevenshteinThreshold() {
            return levenshteinThreshold;
        }

        public void setLevenshteinThreshold(double levenshteinThreshold) {
            this.levenshteinThreshold = levenshteinThreshold;
        }

        public double getEmbeddingThreshold() {
            return embeddingThreshold;
        }

        public void setEmbeddingThreshold(double embeddingThreshold) {
            this.embeddingThreshold = embeddingThreshold;
        }

        public boolean isUseEmbeddingSearch() {
            return useEmbeddingSearch;
        }

        public void setUseEmbeddingSearch(boolean useEmbeddingSearch) {
            this.useEmbeddingSearch = useEmbeddingSearch;
        }
    }

    public DocumentConfig getDocument() {
        return document;
    }

    public void setDocument(DocumentConfig document) {
        this.document = document;
    }

    public FileConfig getFile() {
        return file;
    }

    public void setFile(FileConfig file) {
        this.file = file;
    }

    public ParagraphMatchingConfig getParagraphMatching() {
        return paragraphMatching;
    }

    public void setParagraphMatching(ParagraphMatchingConfig paragraphMatching) {
        this.paragraphMatching = paragraphMatching;
    }
}