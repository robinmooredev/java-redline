package com.example.demo;

import com.aspose.words.Paragraph;
import com.aspose.words.SaveFormat;

public final class Normaliser {

    static String clean(Paragraph p) throws Exception {
        return clean(p.toString(SaveFormat.TEXT));
    }

    static String clean(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "") // strip diacritics
                .replace('’', '\'').replace('"', '\"').replace('"', '\"')
                .replaceAll("[\\s\\u00A0]+", " ") // collapse all whitespace
                .trim().toLowerCase();
    }
}
