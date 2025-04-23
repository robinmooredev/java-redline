#!/bin/bash

# This script creates a sample DOCX document using pandoc
# It requires pandoc to be installed: https://pandoc.org/installing.html

# Check if pandoc is installed
if ! command -v pandoc &> /dev/null; then
    echo "Error: pandoc is not installed. Please install it first:"
    echo "  macOS: brew install pandoc"
    echo "  Ubuntu/Debian: sudo apt-get install pandoc"
    exit 1
fi

# Create a markdown file with sample content
cat > sample.md << 'EOF'
# Sample Document for Redline Testing

## Introduction

This is the original paragraph. It will be used to test the redline functionality.

Some additional text in the introduction section to provide context.

## Terms and Conditions

The service is provided as-is without warranty.

Users must comply with all applicable laws and regulations.

## Miscellaneous

This paragraph should be removed from the document.

Other miscellaneous information goes here.

## Conclusion

Thank you for testing the redline service.
EOF

# Convert the markdown to DOCX
pandoc -f markdown -t docx -o sample.docx sample.md

# Cleanup the markdown file
rm sample.md

echo "Sample DOCX document created as 'sample.docx'"
echo "You can now run ./test-api.sh to test the API"