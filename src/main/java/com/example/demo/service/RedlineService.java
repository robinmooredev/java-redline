package com.example.demo.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aspose.words.Comment;
import com.aspose.words.CommentRangeEnd;
import com.aspose.words.CommentRangeStart;
import com.aspose.words.Document;
import com.aspose.words.Paragraph;
import com.aspose.words.Run;
import com.example.demo.ParagraphFinder;
import com.example.demo.dto.ContractModification;
import com.example.demo.dto.RedlineOutput;
import com.example.demo.exception.RedlineException;
import com.example.demo.exception.RedlineException.ErrorCode;

@Service
public class RedlineService {
    private static final Logger logger = LoggerFactory.getLogger(RedlineService.class);
    private static final String REVIEWER_NAME = "AI Reviewer";
    private static final String REVIEWER_INITIALS = "AI";

    /**
     * Process the original document with the given modifications
     */
    public RedlineOutput generateRedline(MultipartFile originalFile, List<ContractModification> modifications) {
        logger.info("Processing redline request with {} modifications", modifications.size());
        
        if (originalFile == null || originalFile.isEmpty()) {
            logger.error("Original file is null or empty");
            throw new RedlineException(ErrorCode.INPUT_VALIDATION_ERROR, "Original file cannot be null or empty");
        }
        
        if (modifications.isEmpty()) {
            logger.warn("No modifications provided");
        }

        File tmpOriginal = null, tmpRedline = null, tmpClean = null, tmpPdf = null, tmpZip = null;
        Document redlineDoc = null;

        try {
            logger.debug("Creating temporary file for original document");
            tmpOriginal = File.createTempFile("original-", ".docx");
            originalFile.transferTo(tmpOriginal);
            logger.debug("Original document saved to {}", tmpOriginal.getAbsolutePath());

            logger.debug("Loading document with Aspose");
            redlineDoc = new Document(tmpOriginal.getPath());
            
            logger.info("Applying modifications to document clone");
            applyMinimalEdits(redlineDoc, d -> applyUserMods(d, modifications));

            logger.debug("Saving redline document");
            tmpRedline = File.createTempFile("redline-", ".docx");
            redlineDoc.save(tmpRedline.getPath());

            logger.debug("Creating clean document (all changes accepted)");
            tmpClean = File.createTempFile("clean-", ".docx");
            Document cleanDoc = (Document) redlineDoc.deepClone(true);
            cleanDoc.acceptAllRevisions();
            cleanDoc.save(tmpClean.getPath());

            logger.debug("Creating PDF with markup visible");
            tmpPdf = File.createTempFile("redline-", ".pdf");
            redlineDoc.save(tmpPdf.getPath()); // Aspose automatically includes revisions

            logger.info("Creating ZIP archive with all output documents");
            tmpZip = File.createTempFile("redline-output-", ".zip");
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(tmpZip))) {
                addToZip(zip, tmpRedline, "redline.docx");
                addToZip(zip, tmpClean, "clean.docx");
                addToZip(zip, tmpPdf, "redline.pdf");
            }

            byte[] zipContent = Files.readAllBytes(tmpZip.toPath());
            logger.info("ZIP archive created successfully, size: {} bytes", zipContent.length);
            return new RedlineOutput(zipContent);

        } catch (Exception ex) {
            logger.error("Failed to generate redline documents", ex);
            throw new RedlineException(ErrorCode.DOCUMENT_PROCESSING_ERROR, 
                    "Failed to generate redline: " + ex.getMessage(), ex);
        } finally {
            quietlyDelete(tmpOriginal, tmpRedline, tmpClean, tmpPdf, tmpZip);
            if (redlineDoc != null) {
                try {
                    redlineDoc.cleanup();
                } catch (Exception ex) {
                    logger.warn("Failed to cleanup Aspose document", ex);
                }
            }
        }
    }

    /* ---------- Core modification logic ---------- */
    /**
     * Clones <code>original</code>, lets <code>editAction</code> mutate the
     * clone, then asks Aspose to compute the minimal set of tracked changes and
     * write them back into <code>original</code>.
     */
    private static void applyMinimalEdits(Document original, java.util.function.Consumer<Document> editAction) {
        try {
            Document clone = (Document) original.deepClone(true);
            editAction.accept(clone);
            original.compare(clone, REVIEWER_NAME, new Date());
        } catch (Exception e) {
            logger.error("Failed to compare documents for change tracking", e);
            throw new RedlineException(ErrorCode.DOCUMENT_PROCESSING_ERROR, "Failed to compare documents", e);
        }
    }

    /**
     * Apply each user request to <code>doc</code> (the clone).
     */
    private static void applyUserMods(Document doc, List<ContractModification> mods) {
        for (ContractModification m : mods) {
            try {
                logger.debug("Applying {} modification", m.getAction());
                switch (m.getAction()) {
                    case INSERT ->
                        handleInsert(doc, m);
                    case DELETE ->
                        handleDelete(doc, m);
                    case MODIFY ->
                        handleModify(doc, m);
                }
            } catch (RedlineException re) {
                // Pass through custom exceptions
                throw re;
            } catch (Exception e) {
                logger.error("Error processing modification: {}", m.getAction(), e);
                throw new RedlineException(ErrorCode.DOCUMENT_PROCESSING_ERROR, 
                        "Failed to apply " + m.getAction() + " modification: " + e.getMessage(), e);
            }
        }
    }

    /* ---------- individual actions ---------- */
    private static void handleInsert(Document doc, ContractModification m) {
        logger.debug("Handling INSERT modification with section heading: {}", 
                m.getSectionHeading() != null ? m.getSectionHeading() : "none");
        
        Paragraph anchor = findParagraph(doc, m.getSectionHeading(), m.getOldParagraph());
        if (anchor == null) {
            logger.error("Anchor paragraph not found for INSERT operation");
            throw new RedlineException(ErrorCode.ANCHOR_NOT_FOUND, 
                    "Anchor paragraph not found for INSERT operation");
        }

        Paragraph p = new Paragraph(doc);
        p.appendChild(new Run(doc, m.getNewParagraph() == null ? "" : m.getNewParagraph()));
        anchor.getParentNode().insertAfter(p, anchor);

        attachComment(p, m.getComment(), doc);
    }

    private static void handleDelete(Document doc, ContractModification m) {
        logger.debug("Handling DELETE modification with section heading: {}", 
                m.getSectionHeading() != null ? m.getSectionHeading() : "none");
        
        Paragraph target = findParagraph(doc, m.getSectionHeading(), m.getOldParagraph());
        if (target == null) {
            logger.error("Paragraph to delete not found");
            throw new RedlineException(ErrorCode.PARAGRAPH_NOT_FOUND, 
                    "Paragraph to delete not found");
        }
        target.remove();
        // comment is attached to the preceding paragraph (if any)
        attachComment((Paragraph) target.getPreviousSibling(), m.getComment(), doc);
    }

    private static void handleModify(Document doc, ContractModification m) {
        logger.debug("Handling MODIFY modification with section heading: {}", 
                m.getSectionHeading() != null ? m.getSectionHeading() : "none");
        
        Paragraph target = findParagraph(doc, m.getSectionHeading(), m.getOldParagraph());
        if (target == null) {
            logger.error("Paragraph to modify not found");
            throw new RedlineException(ErrorCode.PARAGRAPH_NOT_FOUND, 
                    "Paragraph to modify not found");
        }
        target.removeAllChildren();
        target.appendChild(new Run(doc, m.getNewParagraph()));
        attachComment(target, m.getComment(), doc);
    }

    /* ---------- helpers ---------- */
    private static Paragraph findParagraph(Document doc, String heading, String paraText) {
        if (paraText == null && heading == null) {
            logger.warn("Both paragraph text and heading are null");
            return null;
        }
        
        ParagraphFinder finder = ParagraphFinder.of(doc);
        if (heading != null && !heading.isBlank()) {
            logger.debug("Using heading scope: {}", heading);
            finder = finder.withHeading(heading);
        }
        
        // If we only have a heading, search for that directly
        String searchText = paraText != null ? paraText : heading;
        logger.debug("Searching for paragraph: {}", searchText);
        
        Paragraph result = finder.match(searchText);
        if (result == null) {
            logger.warn("No matching paragraph found for: {}", searchText);
        } else {
            logger.debug("Matching paragraph found");
        }
        return result;
    }

    private static void attachComment(Paragraph anchor, String commentText, Document doc) {
        if (anchor == null) {
            logger.debug("Cannot attach comment: anchor paragraph is null");
            return;
        }
        if (commentText == null || commentText.isBlank()) {
            logger.debug("No comment text provided, skipping comment attachment");
            return;
        }
        
        logger.debug("Attaching comment: {}", commentText);
        try {
            Comment c = new Comment(doc, REVIEWER_NAME, REVIEWER_INITIALS, new Date());
            c.getParagraphs().add(new Paragraph(doc));
            c.getFirstParagraph().getRuns().add(new Run(doc, commentText));
            CommentRangeStart start = new CommentRangeStart(doc, c.getId());
            CommentRangeEnd end = new CommentRangeEnd(doc, c.getId());
            anchor.insertBefore(start, anchor.getFirstChild());
            anchor.appendChild(end);
            anchor.getParentNode().insertAfter(c, anchor);
        } catch (Exception e) {
            logger.warn("Failed to attach comment", e);
            // Non-critical operation, don't throw
        }
    }

    private static void addToZip(ZipOutputStream zip, File f, String entryName) throws IOException {
        logger.debug("Adding {} to ZIP archive", entryName);
        zip.putNextEntry(new ZipEntry(entryName));
        Files.copy(f.toPath(), zip);
        zip.closeEntry();
    }

    private static void quietlyDelete(File... files) {
        for (File f : files) {
            if (f != null && f.exists()) {
                try {
                    logger.debug("Deleting temporary file: {}", f.getAbsolutePath());
                    Files.delete(f.toPath());
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary file: {}", f.getAbsolutePath(), e);
                }
            }
        }
    }
}