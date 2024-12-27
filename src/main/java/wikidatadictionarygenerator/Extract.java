package wikidatadictionarygenerator;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Extract {
    private static final String DEFAULT_DICTIONARY_FILENAME = "resources/cedict_ts.u8";
    private static final char COMMENT_CHARACTER = '#';
    private static final Map<String, List<Word>> simplifiedMapping = new HashMap<String, List<Word>>();
    private static final Map<String, List<Word>> traditionalMapping = new HashMap<String, List<Word>>();

    public static void readInDictionary() {
        readInDictionary(DEFAULT_DICTIONARY_FILENAME);
    }

    public static void readInDictionary(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(filename)))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.charAt(0) == COMMENT_CHARACTER) {
                    continue;
                }
                Word word = new Word();
                String[] str = line.split(" /");
                word.setDefinition(str[1]);
                String[] rem = str[0].split("\\[");
                word.setPinyinNoTones(rem[1].replaceAll("[\\[\\]12345 ]", "").toLowerCase());
                word.setPinyinWithTones(rem[1].replaceAll("[\\[\\]]", "").toLowerCase());

                String[] remRem = rem[0].split(" ");
                word.setTraditionalChinese(Normalizer.normalize(remRem[0], Normalizer.Form.NFC));
                word.setSimplifiedChinese(Normalizer.normalize(remRem[1], Normalizer.Form.NFC));

                simplifiedMapping.computeIfAbsent(word.getSimplifiedChinese(), k -> new ArrayList<>());
                traditionalMapping.computeIfAbsent(word.getTraditionalChinese(), k -> new ArrayList<>());

                simplifiedMapping.get(word.getSimplifiedChinese()).add(word);
                traditionalMapping.get(word.getTraditionalChinese()).add(word);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Word getWordFromChinese(char c) {
        return getWordFromChinese("" + c);
    }

    public static Word getWordFromChinese(String chineseWord) {
        Word simplified = getWordFromSimplifiedChinese(chineseWord);
        if (simplified != null) {
            return simplified;
        }
        return getWordFromTraditionalChinese(chineseWord);
    }


    public static Word getWordFromTraditionalChinese(char c) {
        return getWordFromTraditionalChinese("" + c);
    }

    public static Word getWordFromTraditionalChinese(String chineseWord) {
        if (traditionalMapping.get(Normalizer.normalize(chineseWord, Normalizer.Form.NFC)) == null) {
            return null;
        }
        return traditionalMapping.get(Normalizer.normalize(chineseWord, Normalizer.Form.NFC)).getFirst();
    }

    public static Word getWordFromSimplifiedChinese(char c) {
        return getWordFromSimplifiedChinese("" + c);
    }

    public static List<Word> getWordsFromTraditionalChinese(char c) {
        return getWordsFromTraditionalChinese("" + c);
    }

    public static List<Word> getWordsFromSimplifiedChinese(char c) {
        return getWordsFromSimplifiedChinese("" + c);
    }

    public static List<Word> getWordsFromTraditionalChinese(String chineseWord) {
        return traditionalMapping.get(Normalizer.normalize(chineseWord, Normalizer.Form.NFC));
    }

    public static List<Word> getWordsFromSimplifiedChinese(String chineseWord) {
        return simplifiedMapping.get(Normalizer.normalize(chineseWord, Normalizer.Form.NFC));
    }

    public static List<Word> getWordsFromChinese(char c) {
        return getWordsFromChinese("" + c);
    }

    public static List<Word> getWordsFromChinese(String chineseWord) {
        // NEED TO BE CAREFUL NOT TO MUTATE RETURNED LISTS!!
        // TODO: try to make the lists somehow immutable (as in can't modify actual contents)
        List<Word> words = new ArrayList<>();
        if (getWordsFromTraditionalChinese(chineseWord) != null) {
            words.addAll(getWordsFromTraditionalChinese(chineseWord));
        } else if (getWordsFromSimplifiedChinese(chineseWord) != null){
            words.addAll(getWordsFromSimplifiedChinese(chineseWord));
        }
        return words;
    }

    public static Word getWordFromSimplifiedChinese(String chineseWord) {
        if (simplifiedMapping.get(Normalizer.normalize(chineseWord, Normalizer.Form.NFC)) == null) {
            return null;
        }
        return simplifiedMapping.get(Normalizer.normalize(chineseWord, Normalizer.Form.NFC)).getFirst();
    }
}
