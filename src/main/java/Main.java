package src.main.java;

import java.util.ArrayList;
import java.util.List;

/*
options:
include/exclude cases where pinyin is ambiguouss
only run if both simp + trad exist (whether we autodetect if zh is ttrad/simp or autoconvert iin cases where it is
 unambiguous to do so remains to be decided
if this ^^ we may only want to do it if the simp/trad are equivalent characters

 */

public class Main {
    // these represent the indices in the tsv of the intermediate file
    // TODO: did we forget zh-CN??
    private static final int ZH = 0;
    private static final int ZH_HANS = 1;
    private static final int ZH_HANT = 2;
    private static final int ZH_HK = 3;
    private static final int ZH_MO = 4;
    private static final int ZH_MY = 5;
    private static final int ZH_SG = 6;
    private static final int ZH_TW = 7;
    private static final int ENGLISH = 8;
    private static final int DESCRIPTION = 10;//not sure why 10 and not 9 :P

    private enum OutputFormat {
        CEDICT,
        PLECO
    }

    private static final String INPUT_FILE = "intermediate_data/intermediate_after_excluding_stuff.tsv";

    private static boolean UNAMBIGUOUS_PINYIN_ONLY = true;
    // TODO: Add a check that the trad is equivalent to the simp for transliteration purposes (also how does this play with different romanisations?)
    private static boolean AUTO_CONVERT_TRAD_TO_SIMP_WHEN_AMBIGUOUS = false;
    private static boolean AUTO_CONVERT_SIMP_TO_TRAD_WHEN_AMBIGUOUS = false;
    private static boolean SIMP_REQUIRED = true; //for these two need to consider what to put in other field if empty
    private static boolean TRAD_REQUIRED = true;
    private static boolean IGNORE_ENTRIES_WITH_NO_EN_LABEL = true;
    private static boolean IGNORE_ENTRIES_WITH_NO_DESCRIPTION = false;
    private static final OutputFormat OUTPUT_FORMAT = OutputFormat.PLECO;

    public static void main(String[] args) {
        Extract.readInDictionary();
        List<String> lines = FileUtils.fileToStringArray(INPUT_FILE);
        for (String line : lines) {
            String[] segments = line.split("\t");
            if (segments.length > 10 && (segments[ENGLISH].isEmpty() && segments[DESCRIPTION].isEmpty())) {
                continue;
            }

            if (empty(segments)) continue;
            try {
                if ((TRAD_REQUIRED && getTraditional(segments).isEmpty()) || !containsHan(getTraditional(segments))) {
                    continue;
                }
                if ((SIMP_REQUIRED && getSimplified(segments).isEmpty()) || !containsHan(getSimplified(segments))) {
                    continue;
                }
                if (!containsHan(getTraditional(segments)) && !containsHan(getSimplified(segments))) {
                    continue;
                }
                if (UNAMBIGUOUS_PINYIN_ONLY && !pinyinIsUnambiguous(getSimplified(segments)) && !pinyinIsUnambiguous(getTraditional(segments))) {
                    continue;
                }
                if (IGNORE_ENTRIES_WITH_NO_DESCRIPTION && getNameAndDescription(segments).isEmpty()){
                    continue;
                }
                if (IGNORE_ENTRIES_WITH_NO_EN_LABEL && segments[ENGLISH].isEmpty()) {
                    continue;
                }

                if (OUTPUT_FORMAT == OutputFormat.CEDICT) {
                    System.out.println(getTraditional(segments) + " " + getSimplified(segments)
                            + " [" + getPinyin(segments) + "]"
                            + " /" + getNameAndDescription(segments) + "/");
                } else if (OUTPUT_FORMAT == OutputFormat.PLECO) {
                    System.out.println(getSimplified(segments) + "["
                            + getTraditional(segments) + "]\t"
                            + getPinyin(segments) + "\t"
                            + getNameAndDescription(segments));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean containsHan(String name) {
        for (char c : name.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    public static boolean pinyinIsUnambiguous(String name){
        for (char c : name.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                // TODO: think about how to handle this
//                if (Extract.getWordsFromChinese(c).isEmpty()) {
//                    throw new Exception("getWordsFromChinese returned empty list in pinyinIsUnambiguous");
//                }
                String firstPinyin = Extract.getWordsFromChinese(c).getFirst().getPinyinWithTones().toLowerCase();
                for (Word character : Extract.getWordsFromChinese(c)) {
                    if (!character.getPinyinWithTones().toLowerCase().equals(firstPinyin)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean empty(String[] segments) {
        if (segments.length <= ZH_TW) return true;// need to investigate though...
        return segments[ZH].isEmpty() && segments[ZH_HANS].isEmpty() && segments[ZH_TW].isEmpty();
    }

    public static String getPinyin(String[] segments) {
        //TODO:have some sort of priority order of different Chineses
        String simplifiedPinyin = getPinyin(getSimplified(segments));
        String traditionalPinyin = getPinyin(getTraditional(segments));
        if (!simplifiedPinyin.isEmpty()) {
            return simplifiedPinyin;
        } else if (!traditionalPinyin.isEmpty()) {
            return traditionalPinyin;
        } else {
            return "";
        }
    }

    public static String getPinyin(String givenWord) {
        if (givenWord.isEmpty()) {
            return "";
        }

        List<String> pinyinSegments = new ArrayList<>();
        char[] chars = givenWord.toCharArray();
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

    public static String getSimplified(String[] segments) {
        if (isSimp(segments[ZH_HANS]) && !segments[ZH_HANS].isEmpty()) {
            return segments[ZH_HANS];
        } else if (isSimp(segments[ZH]) && !segments[ZH].isEmpty()) {
            return segments[ZH];
        } else if (!tradToSimpUnambiguous(segments[ZH]).isEmpty()) {//do we need this if it is covered by below?
            return tradToSimpUnambiguous(segments[ZH]);
        }else if (!tradToSimpUnambiguous(getTraditional(segments)).isEmpty()) {
            return tradToSimpUnambiguous(getTraditional(segments));
        }
        else if (!getTraditional(segments).isEmpty() && AUTO_CONVERT_TRAD_TO_SIMP_WHEN_AMBIGUOUS) {
            return tradToSimp(getTraditional(segments));
        } else {
            return "";
        }
    }

    public static String tradToSimpUnambiguous(String tradWord){
        StringBuilder simpBuilder = new StringBuilder();
        for (char c : tradWord.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                List<Word> matches = Extract.getWordsFromChinese(c);
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

    public static String simpToTradUnambiguous(String simpWord){
        StringBuilder tradBuilder = new StringBuilder();
        for (char c : simpWord.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                List<Word> matches = Extract.getWordsFromChinese(c);
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
        String acc = "";
        for (char c : tradWord.toCharArray()) {
            //TODO: add anything that isn't HAN straight to acc?
            words.add(Extract.getWordFromChinese(c));
        }
        for (Word word : words) {
            acc = acc + word.getSimplifiedChinese();
        }
        return acc;
    }

    public static String simpToTrad(String simpWord) {
        List<Word> words = new ArrayList<Word>();
        String acc = "";
        for (char c : simpWord.toCharArray()) {
            System.out.print(c + ":");
            words.add(Extract.getWordFromChinese(c));
        }
        for (Word word : words) {
            acc = acc + word.getTraditionalChinese();
        }
        return acc;
    }

    //TODO spotted a possible bug in tis actual data - see星震学, zh-hant loks simplified in our data but trad in wikidata

    public static String getTraditional(String[] segments) {
        if (isTrad(segments[ZH_HANT]) && !segments[ZH_HANT].isEmpty()) {
            return segments[ZH_HANT];
        } else if (isTrad(segments[ZH_TW]) && !segments[ZH_TW].isEmpty()) {
            return segments[ZH_TW];
        } else if (isTrad(segments[ZH]) && !segments[ZH].isEmpty()) {
            return segments[ZH];
            //.. and so on...
        } else if (isSimp(segments[ZH]) && !simpToTradUnambiguous(segments[ZH]).isEmpty()) {
            return simpToTradUnambiguous(segments[ZH]);
        } else if (!simpToTradUnambiguous(segments[ZH_HANS]).isEmpty()) {
            return simpToTradUnambiguous(segments[ZH_HANS]);
        } else if (!segments[ZH].isEmpty() && AUTO_CONVERT_SIMP_TO_TRAD_WHEN_AMBIGUOUS) {
            return simpToTrad(segments[ZH_HANS]);
        } else if (!segments[ZH_HANS].isEmpty() && AUTO_CONVERT_SIMP_TO_TRAD_WHEN_AMBIGUOUS) {
            return simpToTrad(segments[ZH_HANS]);
        } else {
            return "";
        }
    }

    // TODO: think about streams
    // TODO: move elsewhere?
    public static boolean isSimp(String name) {
        for (char c : name.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN &&
                    Extract.getWordFromSimplifiedChinese(c) == null){
                return false;
            }
        }
        return true;
    }

    public static boolean isTrad(String name) {
        for (char c : name.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN &&
                    Extract.getWordFromTraditionalChinese(c) == null){
                return false;
            }
        }
        return true;
    }

    // TODO: not sure description hasn't stopped working
    public static String getNameAndDescription(String[] segments) {
        return segments[ENGLISH] + ((segments.length > 9 && !segments[DESCRIPTION].isEmpty()) ? ", " + segments[DESCRIPTION] : "");
    }
}
