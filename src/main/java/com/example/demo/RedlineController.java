package com.example.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.NodeCollection;
import com.aspose.words.NodeType;
import com.aspose.words.Paragraph;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.regex.Pattern;
import com.aspose.words.IReplacingCallback;
import com.aspose.words.ReplacingArgs;
import com.aspose.words.ReplaceAction;
import com.aspose.words.Run;
import com.aspose.words.FindReplaceOptions;
import com.aspose.words.Comment;
import com.aspose.words.CommentRangeStart;
import com.aspose.words.CommentRangeEnd;
import com.aspose.words.Node;

@RestController
public class RedlineController {

    @PostMapping("/redline")

    /**
     * Generates the *minimal* tracked‑changes diff by 1. Deep‑cloning the
     * original document, 2. Performing edits on that clone only, 3. Letting
     * Aspose.Words' built‑in compare engine write the differences back to the
     * original document.
     *
     * @param originalDoc the document that will eventually hold the redline
     * @param editAction lambda with all edits to be applied to the clone
     */
    private static void applyMinimalEdits(
            Document originalDoc,
            java.util.function.Consumer<Document> editAction) throws Exception {

        // 1. Work on a deep clone so the original stays untouched until we diff
        Document modifiedDoc = (Document) originalDoc.deepClone(true);

        // 2. Caller performs all edits on the clone
        editAction.accept(modifiedDoc);

        // 3. Ask Aspose to compute the minimal revision set (LCS diff internally)
        originalDoc.compare(modifiedDoc, "AI Reviewer", new java.util.Date());
    }

    // (Optional utility, not used in this version but handy to keep around)
    private static List<String> splitIntoWords(String text) {
        return Arrays.asList(text.trim().split("\\s+"));
    }

    // Holds one requested contract change
    static class ContractModification {
        private String originalText;
        private String revisedText;
        private String comment;

        public String getOriginalText()       { return originalText; }
        public void   setOriginalText(String v){ this.originalText = v; }

        public String getRevisedText()        { return revisedText; }
        public void   setRevisedText(String v){ this.revisedText = v; }

        public String getComment()            { return comment; }
        public void   setComment(String v)    { this.comment = v; }
    }

    public ResponseEntity<?> redline(
            @RequestParam("original") MultipartFile original,
            @RequestParam("modifications") String modificationsJson
    ) {
        File tempOriginal = null;
        File outputRedline = null;
        File outputPdf = null;
        File outputZip = null;
        Document doc = null;

        try {
            // Convert MultipartFile to temporary File
            tempOriginal = File.createTempFile("original-", ".docx");
            original.transferTo(tempOriginal);

            // Create output files
            outputRedline = File.createTempFile("redline-", ".docx");
            outputPdf = File.createTempFile("redline-", ".pdf");
            outputZip = File.createTempFile("redline-output-", ".zip");

            // Load the document
            doc = new Document(tempOriginal.getPath());

            // Parse the JSON array of requested modifications
            ObjectMapper mapper = new ObjectMapper();
            List<ContractModification> modifications = mapper.readValue(
                    modificationsJson,
                    new TypeReference<List<ContractModification>>() {}
            );

            // Use helper to generate a minimal redline
            applyMinimalEdits(doc, modifiedDoc -> {
                for (ContractModification m : modifications) {
                    Pattern pattern = Pattern.compile(Pattern.quote(m.getOriginalText()), Pattern.CASE_INSENSITIVE);

                    try {
                        FindReplaceOptions options = new FindReplaceOptions();
                        options.setReplacingCallback(new IReplacingCallback() {
                            @Override
                            public int replacing(ReplacingArgs args) throws Exception {
                                // Create a comment for the matched text
                                Comment comment = new Comment(modifiedDoc, "AI Reviewer", "AI", new java.util.Date());
                                comment.getParagraphs().add(new Paragraph(modifiedDoc));
                                comment.getFirstParagraph().getRuns().add(new Run(modifiedDoc, m.getComment() == null ? "" : m.getComment()));

                                // Create comment range markers
                                CommentRangeStart commentStart = new CommentRangeStart(modifiedDoc, comment.getId());
                                CommentRangeEnd commentEnd = new CommentRangeEnd(modifiedDoc, comment.getId());

                                // Get the node containing the text to be replaced
                                Node currentNode = args.getMatchNode();
                                
                                // Insert the comment range markers and comment
                                currentNode.getParentNode().insertBefore(commentStart, currentNode);
                                currentNode.getParentNode().insertAfter(commentEnd, currentNode);
                                currentNode.getParentNode().insertAfter(comment, commentEnd);

                                // Replace the text
                                args.setReplacement(m.getRevisedText());
                                return ReplaceAction.REPLACE;
                            }
                        });

                        modifiedDoc.getRange().replace(pattern, m.getRevisedText(), options);
                    } catch (Exception e) {
                        throw new RuntimeException("Error during text replacement: " + e.getMessage(), e);
                    }
                }
            });

            // Save with tracked changes visible
            doc.save(outputRedline.getPath());
            doc.save(outputPdf.getPath());

            // Create ZIP file containing both outputs
            try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputZip))) {
                // Add DOCX file
                zipOut.putNextEntry(new ZipEntry("redline.docx"));
                Files.copy(outputRedline.toPath(), zipOut);
                zipOut.closeEntry();

                // Add PDF file
                zipOut.putNextEntry(new ZipEntry("redline.pdf"));
                Files.copy(outputPdf.toPath(), zipOut);
                zipOut.closeEntry();
            }

            // Prepare response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "redline-output.zip");

            // Return the ZIP file
            byte[] zipContent = Files.readAllBytes(outputZip.toPath());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipContent);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error processing uploaded file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing document: " + e.getMessage());
        } finally {
            // Clean up resources
            if (doc != null) {
                try {
                    doc.cleanup();
                } catch (Exception e) {
                    System.err.println("Error cleaning up document: " + e.getMessage());
                }
            }
            
            // Clean up temporary files
            deleteQuietly(tempOriginal);
            deleteQuietly(outputRedline);
            deleteQuietly(outputPdf);
            deleteQuietly(outputZip);
        }
    }

    private void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                System.err.println("Failed to delete temporary file: " + file.getPath());
            }
        }
    }
}

// Response class to hold both file types
class RedlineResponse {
    private final byte[] redlineDoc;
    private final byte[] pdfDoc;

    public RedlineResponse(byte[] redlineDoc, byte[] pdfDoc) {
        this.redlineDoc = redlineDoc;
        this.pdfDoc = pdfDoc;
    }

    // Add getters
    public byte[] getRedlineDoc() { return redlineDoc; }
    public byte[] getPdfDoc() { return pdfDoc; }
}
