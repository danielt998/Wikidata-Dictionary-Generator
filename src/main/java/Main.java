package src.main.java;

import java.util.List;

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
    private static final OutputFormat DEFAULT_OUTPUT_FORMAT = OutputFormat.CEDICT;

    public static void main(String[] args) {
        OutputFormat outputFormat = DEFAULT_OUTPUT_FORMAT;
        for (int argIndex = 0; argIndex < args.length; argIndex++) {
            switch (args[argIndex]) {
                case "-f", "--format" -> {
                    String format = args[++argIndex].toLowerCase();
                    outputFormat = switch (format) {
                        case "pleco" -> OutputFormat.PLECO;
                        case "cedict" -> OutputFormat.CEDICT;
                        default -> throw new IllegalArgumentException("Unrecognised output format: " + format);
                    };
                }
            }
        }

        Extract.readInDictionary();
        List<String> lines = FileUtils.fileToStringArray(INPUT_FILE);
        for (String line : lines) {
            String[] segments = line.split("\t");

            try {
                if (ignoreRow(segments)) {
                    continue;
                }
                if (outputFormat == OutputFormat.CEDICT) {
                    System.out.println(getTraditional(segments) + " " + getSimplified(segments)
                            + " [" + getPinyin(segments) + "]"
                            + " /" + getNameAndDescription(segments) + "/");
                } else if (outputFormat == OutputFormat.PLECO) {
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

    public static boolean ignoreRow(String[] segments) {
        if (empty(segments)) {
            return true;
        }
        if (segments.length > 10 && (segments[ENGLISH].isEmpty() && segments[DESCRIPTION].isEmpty())) {
            return true;
        }
        if ((TRAD_REQUIRED && getTraditional(segments).isEmpty()) || !HanUtils.containsHan(getTraditional(segments))) {
            return true;
        }
        if ((SIMP_REQUIRED && getSimplified(segments).isEmpty()) || !HanUtils.containsHan(getSimplified(segments))) {
            return true;
        }
        if (!HanUtils.containsHan(getTraditional(segments)) && !HanUtils.containsHan(getSimplified(segments))) {
            return true;
        }
        if (UNAMBIGUOUS_PINYIN_ONLY && !HanUtils.pinyinIsUnambiguous(getSimplified(segments)) && !HanUtils.pinyinIsUnambiguous(getTraditional(segments))) {
            return true;
        }
        if (IGNORE_ENTRIES_WITH_NO_DESCRIPTION && getNameAndDescription(segments).isEmpty()){
            return true;
        }
        if (IGNORE_ENTRIES_WITH_NO_EN_LABEL && segments[ENGLISH].isEmpty()) {
            return true;
        }

        return false;
    }

    private static boolean empty(String[] segments) {
        if (segments.length <= ZH_TW) return true;// need to investigate though...
        return segments[ZH].isEmpty() && segments[ZH_HANS].isEmpty() && segments[ZH_TW].isEmpty();
    }

    public static String getPinyin(String[] segments) {
        //TODO:have some sort of priority order of different Chineses
        String simplifiedPinyin = HanUtils.getPinyin(getSimplified(segments));
        String traditionalPinyin = HanUtils.getPinyin(getTraditional(segments));
        if (!simplifiedPinyin.isEmpty()) {
            return simplifiedPinyin;
        } else if (!traditionalPinyin.isEmpty()) {
            return traditionalPinyin;
        } else {
            return "";
        }
    }

    public static String getSimplified(String[] segments) {
        if (HanUtils.isSimp(segments[ZH_HANS]) && !segments[ZH_HANS].isEmpty()) {
            return segments[ZH_HANS];
        } else if (HanUtils.isSimp(segments[ZH]) && !segments[ZH].isEmpty()) {
            return segments[ZH];
        } else if (!HanUtils.tradToSimpUnambiguous(segments[ZH]).isEmpty()) {//do we need this if it is covered by below?
            return HanUtils.tradToSimpUnambiguous(segments[ZH]);
        }else if (!HanUtils.tradToSimpUnambiguous(getTraditional(segments)).isEmpty()) {
            return HanUtils.tradToSimpUnambiguous(getTraditional(segments));
        }
        else if (!getTraditional(segments).isEmpty() && AUTO_CONVERT_TRAD_TO_SIMP_WHEN_AMBIGUOUS) {
            return HanUtils.tradToSimp(getTraditional(segments));
        } else {
            return "";
        }
    }

    //TODO spotted a possible bug in tis actual data - see星震学, zh-hant looks simplified in our data but trad in wikidata

    public static String getTraditional(String[] segments) {
        if (HanUtils.isTrad(segments[ZH_HANT]) && !segments[ZH_HANT].isEmpty()) {
            return segments[ZH_HANT];
        } else if (HanUtils.isTrad(segments[ZH_TW]) && !segments[ZH_TW].isEmpty()) {
            return segments[ZH_TW];
        } else if (HanUtils.isTrad(segments[ZH]) && !segments[ZH].isEmpty()) {
            return segments[ZH];
            //.. and so on...
        } else if (HanUtils.isSimp(segments[ZH]) && !HanUtils.simpToTradUnambiguous(segments[ZH]).isEmpty()) {
            return HanUtils.simpToTradUnambiguous(segments[ZH]);
        } else if (!HanUtils.simpToTradUnambiguous(segments[ZH_HANS]).isEmpty()) {
            return HanUtils.simpToTradUnambiguous(segments[ZH_HANS]);
        } else if (!segments[ZH].isEmpty() && AUTO_CONVERT_SIMP_TO_TRAD_WHEN_AMBIGUOUS) {
            return HanUtils.simpToTrad(segments[ZH_HANS]);
        } else if (!segments[ZH_HANS].isEmpty() && AUTO_CONVERT_SIMP_TO_TRAD_WHEN_AMBIGUOUS) {
            return HanUtils.simpToTrad(segments[ZH_HANS]);
        } else {
            return "";
        }
    }

    public static String getNameAndDescription(String[] segments) {
        return segments[ENGLISH] + ((segments.length > 9 && !segments[DESCRIPTION].isEmpty()) ? ", " + segments[DESCRIPTION] : "");
    }
}
