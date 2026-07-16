import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class SearchAutocomplete {

    public static void main(String[] args) {
        AutocompleteTrie trie = new AutocompleteTrie();

        String[] pastQueries = {
                "how to make pasta",
                "how to learn java",
                "how to make pizza",
                "how to make pasta",
                "how to make pasta",
                "how to learn python",
                "how to code",
                "where is the nearest cafe",
                "where is the nearest pharmacy",
                "where is the nearest pharmacy",
        };

        for (String q : pastQueries) {
            trie.indexQuery(q);
        }

        String[] prefixes = {
                "how t",
                "how to ma",
                "where is",
        };

        for (String prefix : prefixes) {
            System.out.println("=== prefix: \"" + prefix + "\" ===\n  -- Frequency --");
            printList(trie.complete(prefix, 5, new FrequencyRankingStrategy()));
            System.out.println("  -- Relevance (mock) --");
            printList(trie.complete(prefix, 5, new RelevanceRankingStrategy()));
            System.out.println();
        }
    }

    private static void printList(List<String> c) {
        for (int i = 0; i < c.size(); i++) {
            System.out.println("    " + (i + 1) + ". " + c.get(i));
        }
    }
}

final class Suggestion implements Comparable<Suggestion> {
    private final String text;
    private final int frequency;

    Suggestion(String text, int frequency) {
        this.text = text;
        this.frequency = frequency;
    }

    String getText() { return text; }
    int getFrequency() { return frequency; }

    @Override
    public int compareTo(Suggestion o) {
        int c = Integer.compare(o.frequency, this.frequency);
        if (c != 0) return c;
        return this.text.compareTo(o.text);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Suggestion)) return false;
        Suggestion that = (Suggestion) o;
        return frequency == that.frequency && text.equals(that.text);
    }

    @Override
    public int hashCode() { return Objects.hash(text, frequency); }
}

final class TrieNode {
    private final Map<Character, TrieNode> children = new HashMap<>();
    private boolean endOfWord;
    private String word;
    private int frequency;

    Map<Character, TrieNode> getChildren() { return children; }
    boolean isEndOfWord() { return endOfWord; }
    void setEndOfWord(boolean endOfWord) { this.endOfWord = endOfWord; }
    String getWord() { return word; }
    void setWord(String word) { this.word = word; }
    int getFrequency() { return frequency; }
    void addFrequency(int delta) { this.frequency += delta; }
}

final class TextNormalize {
    private TextNormalize() {}

    static String normalizeForIndex(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String t = raw.toLowerCase(Locale.ROOT).trim();
        t = t.replaceAll("\\s+", " ");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == ' ' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) b.append(c);
        }
        return b.toString();
    }
}

final class AutocompleteTrie {
    private final TrieNode root = new TrieNode();

    void indexQuery(String raw) {
        String n = TextNormalize.normalizeForIndex(raw);
        if (n.isEmpty()) return;
        TrieNode cur = root;
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            cur = cur.getChildren().computeIfAbsent(c, ch -> new TrieNode());
        }
        cur.setEndOfWord(true);
        cur.setWord(n);
        cur.addFrequency(1);
    }

    List<String> complete(String rawPrefix, int maxResults) {
        return complete(rawPrefix, maxResults, new FrequencyRankingStrategy());
    }

    List<String> complete(String rawPrefix, int maxResults, RankingStrategyy ranking) {
        if (maxResults < 1) return List.of();
        if (ranking == null) throw new IllegalArgumentException("ranking");
        String p = TextNormalize.normalizeForIndex(rawPrefix);
        TrieNode node = root;
        for (int i = 0; i < p.length(); i++) {
            node = node.getChildren().get(p.charAt(i));
            if (node == null) return List.of();
        }
        List<Suggestion> acc = new ArrayList<>();
        collect(node, acc);
        if (acc.isEmpty()) return List.of();
        List<Suggestion> ranked = ranking.rank(acc, p);
        List<String> out = new ArrayList<>(Math.min(maxResults, ranked.size()));
        for (int i = 0; i < ranked.size() && i < maxResults; i++) {
            out.add(ranked.get(i).getText());
        }
        return Collections.unmodifiableList(out);
    }

    private void collect(TrieNode node, List<Suggestion> out) {
        if (node.isEndOfWord() && node.getWord() != null) {
            out.add(new Suggestion(node.getWord(), node.getFrequency()));
        }
        for (var e : node.getChildren().entrySet()) {
            collect(e.getValue(), out);
        }
    }
}

interface RankingStrategyy {
    List<Suggestion> rank(List<Suggestion> candidates, String normalizedPrefix);
}

final class FrequencyRankingStrategy implements RankingStrategyy {
    @Override
    public List<Suggestion> rank(List<Suggestion> candidates, String normalizedPrefix) {
        List<Suggestion> out = new ArrayList<>(candidates);
        out.sort(Comparator.comparingInt(Suggestion::getFrequency)
                .reversed()
                .thenComparing(Suggestion::getText, String::compareTo));
        return out;
    }
}

final class RelevanceRankingStrategy implements RankingStrategyy {
    private static final Set<String> MOCK_POPULAR_CONCEPTS =
            Set.of("pasta", "pizza", "pharmacy", "java", "python", "cafe");

    private static int mockScore(Suggestion s, String prefix) {
        int score = s.getFrequency() * 100;
        String t = s.getText();
        for (String term : MOCK_POPULAR_CONCEPTS) {
            if (t.contains(term)) score += 50;
        }
        int extraChars = t.length() - prefix.length();
        score += Math.max(0, 25 - Math.min(25, extraChars));
        return score;
    }

    @Override
    public List<Suggestion> rank(List<Suggestion> candidates, String normalizedPrefix) {
        List<Suggestion> out = new ArrayList<>(candidates);
        out.sort(Comparator.comparingInt((Suggestion s) -> mockScore(s, normalizedPrefix))
                .reversed()
                .thenComparing(Suggestion::getText, String::compareTo));
        return out;
    }
}