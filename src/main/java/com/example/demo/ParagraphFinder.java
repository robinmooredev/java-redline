package com.example.demo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aspose.words.Document;
import com.aspose.words.Paragraph;
import com.example.demo.config.RedlineConfig;

@Component
public final class ParagraphFinder {

    private static RedlineConfig config;
    
    @Autowired
    public ParagraphFinder(RedlineConfig config) {
        ParagraphFinder.config = config;
    }

    private Document doc;
    private String headingFilter;

    private ParagraphFinder(Document doc) {
        this.doc = doc;
    }

    public static ParagraphFinder of(Document doc) {
        return new ParagraphFinder(doc);
    }

    /**
     * Optional heading scope (makes fuzzy matches safer)
     */
    public ParagraphFinder withHeading(String heading) {
        this.headingFilter = Normaliser.clean(heading);
        return this;
    }

    public Paragraph match(String query) {
        String needle = Normaliser.clean(query);

        /* ---------- PASS 1 : exact-ish ---------- */
        for (Paragraph p : candidates()) {
            try {
                if (Normaliser.clean(p).equals(needle)) {
                    return p;
                }
            } catch (Exception e) {
                // Skip paragraphs that can't be processed
            }
        }

        /* ---------- PASS 2 : fuzzy distance ---------- */
        Paragraph best = null;
        double bestScore = 0;
        for (Paragraph p : candidates()) {
            try {
                double sim = calculateSimilarity(needle, Normaliser.clean(p));
                if (sim > bestScore) {
                    bestScore = sim;
                    best = p;
                }
            } catch (Exception e) {
                // Skip paragraphs that can't be processed
            }
        }
        if (bestScore >= config.getParagraphMatching().getLevenshteinThreshold()) {
            return best;
        }

        /* ---------- PASS 3 : embedding search (optional) ---------- */
        if (config.getParagraphMatching().isUseEmbeddingSearch()) {
            // List<String> paras = candidates().stream().map(Normaliser::clean).toList();
            // List<double[]> embeddings = Embeddings.get(paras);     // your caching layer
            // double[] queryVec        = Embeddings.get(needle);
            // int idx = Cosine.bestMatch(queryVec, embeddings);
            // if (Cosine.score(queryVec, embeddings.get(idx)) >= config.getParagraphMatching().getEmbeddingThreshold())
            //     return candidates().get(idx);
        }
        return null;  // or throw
    }

    private List<Paragraph> candidates() {
        List<Paragraph> result = new ArrayList<>();
        var nodes = doc.getFirstSection().getBody().getChildNodes();

        if (headingFilter == null) {
            for (int i = 0; i < nodes.getCount(); i++) {
                var node = nodes.get(i);
                if (node instanceof Paragraph p) {
                    result.add(p);
                }
            }
            return result;
        }

        // Find the heading paragraph, then collect all paragraphs under it
        // until the next heading of the same or higher level
        boolean inSection = false;
        for (int i = 0; i < nodes.getCount(); i++) {
            var node = nodes.get(i);
            if (!(node instanceof Paragraph p)) continue;
            try {
                String cleaned = Normaliser.clean(p);
                if (!inSection) {
                    if (cleaned.contains(headingFilter)) {
                        inSection = true;
                        result.add(p);
                    }
                } else {
                    // Stop at the next section heading (lines like "3. ...", "4. ...")
                    if (cleaned.matches("^\\d+\\.\\s.*") && !cleaned.contains(headingFilter)) {
                        break;
                    }
                    result.add(p);
                }
            } catch (Exception e) {
                // Skip paragraphs that can't be processed
            }
        }
        return result;
    }

    private static double calculateSimilarity(String s1, String s2) {
        // Simple Levenshtein distance implementation
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        
        int maxLength = Math.max(s1.length(), s2.length());
        return maxLength == 0 ? 1.0 : 1.0 - (double)dp[s1.length()][s2.length()] / maxLength;
    }
}