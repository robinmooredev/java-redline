# Java Redline Document Service

A Spring Boot REST service that generates "redline" contract documents with tracked changes from an original document and a list of modifications.

## Overview

This service takes an original DOCX file and a JSON array of requested modifications, then applies those changes as tracked revisions in Microsoft Word format. It returns a ZIP file containing:

1. **redline.docx** - The document with tracked changes visible
2. **clean.docx** - All revisions accepted (final version)
3. **redline.pdf** - PDF version with markup visible

## API Specification

### Endpoint

```
POST /redline
```

### Request

The request must be a multipart form with the following parameters:

- `file`: The original DOCX document (MultipartFile)
- `modifications`: A JSON array of modification requests

### Modification JSON Format

Each modification in the array has the following structure:

```json
{
  "action": "INSERT|DELETE|MODIFY",
  "sectionHeading": "Optional heading text that scopes the change",
  "oldParagraph": "Text of paragraph being replaced/deleted (optional for INSERT)",
  "newParagraph": "Text being inserted/used as replacement (optional for DELETE)",
  "comment": "Optional reviewer comment to attach"
}
```

### Response

A ZIP file containing three documents:
- redline.docx
- clean.docx
- redline.pdf

## Implementation Details

The service uses:
- Spring Boot for the REST API
- Aspose Words library for document processing
- Paragraph fuzzy matching to locate text even with minor differences:
  - Exact matching with normalized text
  - Levenshtein distance for fuzzy matching
  - Optional embedding-based semantic matching (commented out in code)

## Dependencies

- Spring Boot
- Aspose Words
- Jackson for JSON processing

## Technical Notes

- The service normalizes text for more robust matching (removing diacritics, standardizing quotes, etc.)
- Temporary files are created during processing and automatically cleaned up
- Comments are attached to the modified paragraphs when provided