package src.main.java;

import java.util.ArrayList;
import java.util.List;

public class HanUtils {
    public static boolean containsHan(String string) {
        return Utils.charArrayToCharacterList(string.toCharArray()).stream()
                .anyMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN);
    }

    public static boolean pinyinIsUnambiguous(String hanzi){
        for (char c : hanzi.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                String firstPinyin = Extract.getWordsFromChinese(c).getFirst().getPinyinWithTones().toLowerCase();
                if (Extract.getWordsFromChinese(c).stream()
                        .anyMatch(character -> !character.getPinyinWithTones().toLowerCase().equals(firstPinyin))) {
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
                    if (!word.getSimplifiedChinese().equals(firstMatchSimp)){
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
                    if (!word.getTraditionalChinese().equals(firstMatchTrad)){
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

    public static String tradToSimp(String tradWord) {
        List<Word> words = new ArrayList<Word>();
        StringBuilder acc = new StringBuilder();
        for (char c : tradWord.toCharArray()) {
            //TODO: add anything that isn't HAN straight to acc?
            words.add(Extract.getWordFromChinese(c));
        }
        for (Word word : words) {
            acc.append(word.getSimplifiedChinese());
        }
        return acc.toString();
    }

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
