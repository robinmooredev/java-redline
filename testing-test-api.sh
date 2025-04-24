#!/bin/bash

# Sample curl script to test the redline API endpoint

# Path to a sample DOCX file
SAMPLE_DOCX="./sample.docx"

# Sample modifications as JSON
MODIFICATIONS='[
  {
    "action": "INSERT",
    "sectionHeading": "Introduction",
    "oldParagraph": "This is the original paragraph.",
    "newParagraph": "This is a new paragraph to be inserted after the original.",
    "comment": "Added additional context"
  },
  {
    "action": "MODIFY",
    "sectionHeading": "Terms and Conditions",
    "oldParagraph": "The service is provided as-is without warranty.",
    "newParagraph": "The Services are provided as-is without any warranty, express or implied.",
    "comment": "Clarified warranty disclaimer"
  },
  {
    "action": "DELETE",
    "sectionHeading": "Miscellaneous",
    "oldParagraph": "This paragraph should be removed from the document.",
    "comment": "Removed redundant text"
  }
]'

# Check if sample DOCX exists
if [ ! -f "$SAMPLE_DOCX" ]; then
  echo "Error: Sample DOCX file not found at $SAMPLE_DOCX"
  echo "Please create or provide a valid DOCX file"
  exit 1
fi

# Make the API request
echo "Sending request to redline API..."
curl -X POST \
  -H "Content-Type: multipart/form-data" \
  -F "file=@$SAMPLE_DOCX" \
  -F "modifications=$MODIFICATIONS" \
  --output redline-output.zip \
  http://localhost:8080/redline

# Check if the request was successful
if [ $? -eq 0 ] && [ -f "redline-output.zip" ]; then
  echo "Success! Response saved to redline-output.zip"
  echo "The ZIP file contains:"
  echo "- redline.docx (tracked changes version)"
  echo "- clean.docx (all changes accepted)"
  echo "- redline.pdf (PDF with markup visible)"
else
  echo "Request failed or response wasn't saved correctly"
fi