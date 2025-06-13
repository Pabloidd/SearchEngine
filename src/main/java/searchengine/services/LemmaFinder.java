package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class LemmaFinder {
    private final LuceneMorphology luceneMorphology;
    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public LemmaFinder() throws IOException {
        this.luceneMorphology = new RussianLuceneMorphology();
    }

    private LemmaFinder(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    /**
     * Собирает леммы из текста с подсчетом их частоты.
     * @param text исходный текст
     * @return Map<лемма, частота>
     */
    public Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsRussianWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank()) continue;

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) continue;

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) continue;

            String normalWord = normalForms.get(0);
            lemmas.put(normalWord, lemmas.getOrDefault(normalWord, 0) + 1);
        }

        return lemmas;
    }

    /**
     * Получает уникальные леммы из текста.
     * @param text исходный текст
     * @return Set уникальных лемм
     */
    public Set<String> getLemmaSet(String text) {
        String[] textArray = arrayContainsRussianWords(text);
        Set<String> lemmaSet = new HashSet<>();
        for (String word : textArray) {
            if (!word.isEmpty() && isCorrectWordForm(word)) {
                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) continue;
                lemmaSet.addAll(luceneMorphology.getNormalForms(word));
            }
        }
        return lemmaSet;
    }

    /**
     * Проверяет, содержит ли текст указанные леммы.
     * @param text текст для проверки
     * @param lemmas набор лемм для поиска
     * @return true если хотя бы одна лемма найдена
     */
    public boolean containsLemmas(String text, Set<String> lemmas) {
        Set<String> textLemmas = getLemmaSet(text);
        return textLemmas.stream().anyMatch(lemmas::contains);
    }

    /**
     * Очищает HTML от тегов.
     * @param htmlText HTML-текст
     * @return чистый текст
     */
    public String cleanHtml(String htmlText) {
        if (htmlText == null || htmlText.isEmpty()) return "";
        return Jsoup.parse(htmlText).text();
    }

    // Вспомогательные методы
    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) return true;
        }
        return false;
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private boolean isCorrectWordForm(String word) {
        List<String> wordInfo = luceneMorphology.getMorphInfo(word);
        for (String morphInfo : wordInfo) {
            if (morphInfo.matches(WORD_TYPE_REGEX)) return false;
        }
        return true;
    }
}