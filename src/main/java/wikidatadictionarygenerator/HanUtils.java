package wikidatadictionarygenerator;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.text.Transliterator;

public class HanUtils {
    // ICU seems much slower than using CC-CEDICT with a map:(
    public static final boolean USE_ICU_FOR_TRANSLITERATION = false;

    public static boolean tradAndSimpMatch(String trad, String simp) {
        if (Normalizer.normalize(trad, Normalizer.Form.NFC).length()
                != Normalizer.normalize(simp, Normalizer.Form.NFC).length()) return false;
        for (int i = 0; i < trad.length(); i++) {
            if (Character.UnicodeScript.of(trad.charAt(i)) == Character.UnicodeScript.HAN &&
                    Character.UnicodeScript.of(simp.charAt(i)) == Character.UnicodeScript.HAN) {
                if (!tradAndSimpMatch(trad.charAt(i), simp.charAt(i))) {
                    return false;
                } else {
                    continue;
                }
            }

            if (trad.charAt(i) != simp.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean tradAndSimpMatch(char tradChar, char simpChar) {
        try {
            return Extract.getWordsFromTraditionalChinese(tradChar).stream()
                    .anyMatch(word -> normalisedStringEquals("" + simpChar, word.getSimplifiedChinese()));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean normalisedStringEquals(String a, String b) {
        return Normalizer.normalize(a, Normalizer.Form.NFC)
                .equals(Normalizer.normalize(b, Normalizer.Form.NFC));
    }

    public static boolean containsHan(String string) {
        return Utils.charArrayToCharacterList(string.toCharArray()).stream()
                .anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN);
    }

    public static boolean pinyinIsUnambiguous(String hanzi){
        for (char c : hanzi.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                String firstPinyin = Extract.getWordsFromChinese(c).getFirst().getPinyinWithTones().toLowerCase();
                if (Extract.getWordsFromChinese(c).stream()
                        .anyMatch(character -> !normalisedStringEquals(character.getPinyinWithTones().toLowerCase(), firstPinyin))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String getPinyin(String hanzi) {
        if (hanzi.isEmpty()) {
            return "";
        }

        if (USE_ICU_FOR_TRANSLITERATION || !pinyinIsUnambiguous(hanzi)) {
            // frustratingly there's no easy answer as to whether to use the "Han-Latin/Names" transliterator or the other
            // "Han-Latin" one - /Names is better for Chinese language people's names, but "Han-Latin" is better for
            // almost everything else - e.g. po2 here is very unusual in any other context:
            // https://github.com/unicode-org/cldr/blob/9bbbc7769d6824229c49817c143ca7afebb00a34/common/transforms/Han-Latin-Names.xml#L50
            Transliterator transliterator = Transliterator.getInstance("Han-Latin");
            Transliterator pinyinToNumericPinyinTransliterator = Transliterator.getInstance("Latin-NumericPinyin");
            return pinyinToNumericPinyinTransliterator.transliterate(transliterator.transliterate(hanzi))
                    .replaceAll("ü", "u:");// doublle check that Pleco also wants this
        } else {
            List<String> pinyinSegments = new ArrayList<>();
            char[] chars = hanzi.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if (Character.UnicodeScript.of(chars[i]) == Character.UnicodeScript.HAN) {
                    pinyinSegments.add(Extract.getWordFromChinese(chars[i]).getPinyinWithTones());
                } else {
                    StringBuilder nonHanString = new StringBuilder("" + chars[i]);
                    while (i + 1 < chars.length && Character.UnicodeScript.of(chars[i + 1]) != Character.UnicodeScript.HAN) {
                        nonHanString.append(chars[++i]);
                    }
                    pinyinSegments.add(nonHanString.toString());
                }
            }
            return String.join(" ", pinyinSegments);
        }
    }

    public static String tradToSimpUnambiguous(String tradWord){
        StringBuilder simpBuilder = new StringBuilder();
        for (char c : tradWord.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                List<Word> matches = Extract.getWordsFromChinese(c);
                if (matches.isEmpty()) {
                    System.err.println("ERROR: no match found for char " + c);
                    return "";
                }
                String firstMatchSimp = matches.getFirst().getSimplifiedChinese();
                for (Word word : matches) {
                    if (!normalisedStringEquals(word.getSimplifiedChinese(), firstMatchSimp)){
                        return "";// TODO: find a better way to fail
                    }
                }
                simpBuilder.append(firstMatchSimp);
            } else {
                simpBuilder.append(c);
            }
        }
        return simpBuilder.toString();
    }

    //TODO: merge ambiguous and unambiguous?
    public static String simpToTradUnambiguous(String simpWord){
        StringBuilder tradBuilder = new StringBuilder();
        for (char c : simpWord.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                List<Word> matches = Extract.getWordsFromChinese(c);
                if (matches.isEmpty()) {
                    System.err.println("ERROR: no match found for char " + c);
                    return "";
                }
                String firstMatchTrad = matches.getFirst().getTraditionalChinese();
                for (Word word : matches) {
                    if (!normalisedStringEquals(word.getTraditionalChinese(), firstMatchTrad)){
                        return "";// TODO: find a better way to fail
                    }
                }
                tradBuilder.append(firstMatchTrad);
            } else {
                tradBuilder.append(c);
            }
        }
        return tradBuilder.toString();
    }

    //TODO: Refactor to look more like tradToSimpUnambiguous and pass through chars that aren't hanzi
    public static String tradToSimp(String tradWord) {
        List<Word> words = new ArrayList<Word>();
        StringBuilder acc = new StringBuilder();
        for (char c : tradWord.toCharArray()) {
            words.add(Extract.getWordFromChinese(c));
        }
        for (Word word : words) {
            acc.append(word.getSimplifiedChinese());
        }
        return acc.toString();
    }

    //TODO: Refactor to look more like tradToSimpUnambiguous and pass through chars that aren't hanzi
    public static String simpToTrad(String simpWord) {
        List<Word> words = new ArrayList<Word>();
        StringBuilder acc = new StringBuilder();
        for (char c : simpWord.toCharArray()) {
            System.out.print(c + ":");
            words.add(Extract.getWordFromChinese(c));
        }
        for (Word word : words) {
            acc.append(word.getTraditionalChinese());
        }
        return acc.toString();
    }

    public static boolean isSimp(String string) {
        return Utils.charArrayToCharacterList(string.toCharArray()).stream().noneMatch(c ->
                Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN &&
                        Extract.getWordFromSimplifiedChinese(c) == null);
    }

    public static boolean isTrad(String string) {
        return Utils.charArrayToCharacterList(string.toCharArray()).stream().noneMatch(c ->
                        Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN &&
                    Extract.getWordFromTraditionalChinese(c) == null);
    }
}
