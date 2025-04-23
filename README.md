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

## Code Structure

The application follows a layered architecture with clear separation of concerns:

### Main Packages

- `com.example.demo.controller` - REST controllers for handling HTTP requests
- `com.example.demo.service` - Business logic and document processing
- `com.example.demo.dto` - Data Transfer Objects for API communication
- `com.example.demo.exception` - Custom exceptions and error handling

### Key Components

1. **RedlineController** (`controller/RedlineController.java`)
   - Entry point for HTTP requests
   - Input validation and error handling
   - Delegates document processing to RedlineService

2. **RedlineService** (`service/RedlineService.java`)
   - Core business logic
   - Handles document modifications through Aspose API
   - Manages file operations (temporary files, zip creation)

3. **ParagraphFinder** (`ParagraphFinder.java`)
   - Sophisticated paragraph matching using multiple strategies:
     - Exact matching with normalized text
     - Fuzzy matching with Levenshtein distance
     - (Optional, commented out) Embedding-based matching

4. **Normaliser** (`Normaliser.java`)
   - Text normalization for more robust paragraph matching
   - Handles whitespace, quotes, diacritics, etc.

5. **ContractModification** (`dto/ContractModification.java`)
   - DTO for modification requests
   - Includes action type and affected text

6. **RedlineException** (`exception/RedlineException.java`)
   - Custom exception with error codes
   - Enables structured error responses

### Processing Flow

1. User submits a document and modifications
2. Controller validates input
3. Service processes document:
   - Finds paragraphs to modify using ParagraphFinder
   - Creates a clone of the document
   - Applies changes to the clone
   - Uses Aspose comparison to track changes between original and modified
   - Generates output files (redline, clean, PDF)
   - Bundles results in ZIP

### Error Handling

- Custom exceptions with specific error codes
- Consistent logging across all components
- Spring's @ExceptionHandler for API responses

## Dependencies

- Spring Boot for the REST API
- Aspose Words library for document processing
- Jackson for JSON processing
- SLF4J for logging

## Getting Started

1. Clone the repository
2. Build the project (optional):
   ```
   ./mvnw clean package
   ```
3. Run the application:
   ```
   ./mvnw spring-boot:run
   ```
4. The service will be available at http://localhost:8080

You can also build and run the project in one step with:
```
./mvnw clean spring-boot:run
```

## Technical Notes

- The service normalizes text for more robust matching (removing diacritics, standardizing quotes, etc.)
- Temporary files are created during processing and automatically cleaned up
- Comments are attached to the modified paragraphs when provided
- The service uses SLF4J for logging with configurable levels in application.properties