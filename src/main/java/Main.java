package src.main.java;

import java.util.List;

public class Main {
    // these represent the indices in the tsv of the intermediate file
    // TODO: Add zh-CN and others
    private static final int ZH = 0;
    private static final int ZH_HANS = 1;
    private static final int ZH_HANT = 2;
    private static final int ZH_HK = 3;
    private static final int ZH_MO = 4;
    private static final int ZH_MY = 5;
    private static final int ZH_SG = 6;
    private static final int ZH_TW = 7;
    private static final int ENGLISH = 8;
    private static final int DESCRIPTION = 10;// We seem to have two tabs between the last field and this

    // Note that these are in precedence order
    private static final int[] SIMPLIFIED_FIELDS = new int[] { ZH_HANS, ZH_MY, ZH_SG };
    private static final int[] TRADITIONAL_FIELDS = new int[] { ZH_HANT, ZH_TW, ZH_HK, ZH_MO };

    private enum OutputFormat {
        CEDICT,
        PLECO
    }

    private static final String INPUT_FILE = "intermediate_data/intermediate_after_excluding_stuff.tsv";

    private static boolean UNAMBIGUOUS_PINYIN_ONLY = true;
    // TODO: Add a check that the trad is equivalent to the simp for transliteration purposes (also how does this play with different romanisations?)
    // also that means multiple combinations to check etc..
    private static boolean AUTO_CONVERT_TRAD_TO_SIMP_WHEN_AMBIGUOUS = false;
    private static boolean AUTO_CONVERT_SIMP_TO_TRAD_WHEN_AMBIGUOUS = false;
    private static boolean SIMP_REQUIRED = true; //for these two need to consider what to put in other field if empty
    private static boolean TRAD_REQUIRED = true;
    private static boolean IGNORE_ENTRIES_WITH_NO_EN_LABEL = true;
    private static boolean IGNORE_ENTRIES_WITH_NO_NAME_OR_DESCRIPTION = false;
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
            String[] segments = line.split("\t", -1);

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

    private static boolean ignoreRow(String[] segments) {
        if (empty(segments)) {
            return true;
        }
        if (segments[ENGLISH].isEmpty() && segments[DESCRIPTION].isEmpty()) {
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
        if (IGNORE_ENTRIES_WITH_NO_NAME_OR_DESCRIPTION && getNameAndDescription(segments).isEmpty()){
            return true;
        }
        if (IGNORE_ENTRIES_WITH_NO_EN_LABEL && segments[ENGLISH].isEmpty()) {
            return true;
        }

        return false;
    }

    private static boolean empty(String[] segments) {
        return segments[ZH].isEmpty() && segments[ZH_HANS].isEmpty() && segments[ZH_TW].isEmpty();
    }

    private static String getPinyin(String[] segments) {
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

    private static String getSimplified(String[] segments) {
        for (int i : SIMPLIFIED_FIELDS) {
            if (HanUtils.isSimp(segments[i]) && !segments[i].isEmpty()) {
                return segments[i];
            }
        }

        // The problem with the 'zh' label is that it can be either simplified or traditional so we have to figure out
        // which it is if possible
        if (HanUtils.isSimp(segments[ZH]) && !segments[ZH].isEmpty()) {
            return segments[ZH];
        } else if (!HanUtils.tradToSimpUnambiguous(segments[ZH]).isEmpty()) {//do we need this if it is covered by below?
            return HanUtils.tradToSimpUnambiguous(segments[ZH]);
        }
        for (int i : TRADITIONAL_FIELDS) {
            if (!HanUtils.tradToSimpUnambiguous(segments[i]).isEmpty()) {
                return HanUtils.tradToSimpUnambiguous(segments[i]);
            }
        }

        if (AUTO_CONVERT_TRAD_TO_SIMP_WHEN_AMBIGUOUS) {
            for (int i : TRADITIONAL_FIELDS) {
                if (!segments[i].isEmpty() && !HanUtils.tradToSimp(segments[i]).isEmpty()) {
                    return HanUtils.tradToSimp(segments[i]);
                }
            }
        }
        return "";
    }

    private static String getTraditional(String[] segments) {
        for (int i : TRADITIONAL_FIELDS) {
            if (HanUtils.isTrad(segments[i]) && !segments[i].isEmpty()) {
                return segments[i];
            }
        }

        // The problem with the 'zh' label is that it can be either simplified or traditional so we have to figure out
        // which it is if possible
        if (HanUtils.isTrad(segments[ZH]) && !segments[ZH].isEmpty()) {
            return segments[ZH];
        } else if (HanUtils.isSimp(segments[ZH]) && !HanUtils.simpToTradUnambiguous(segments[ZH]).isEmpty()) {
            return HanUtils.simpToTradUnambiguous(segments[ZH]);
        }

        for (int i : SIMPLIFIED_FIELDS) {
            if (!HanUtils.simpToTradUnambiguous(segments[i]).isEmpty()) {
                return HanUtils.simpToTradUnambiguous(segments[i]);
            }
        }

        if (AUTO_CONVERT_SIMP_TO_TRAD_WHEN_AMBIGUOUS) {
            if (!segments[ZH].isEmpty() && !HanUtils.simpToTrad(segments[ZH]).isEmpty()) {
                return HanUtils.simpToTrad(segments[ZH]);
            }
            for (int i : SIMPLIFIED_FIELDS) {
                if (!segments[i].isEmpty() && !HanUtils.simpToTrad(segments[i]).isEmpty()) {
                    return HanUtils.simpToTrad(segments[i]);
                }
            }
        }
        return "";
    }

    private static String getNameAndDescription(String[] segments) {
        return segments[ENGLISH] + (!segments[DESCRIPTION].isEmpty() ? ", " + segments[DESCRIPTION] : "");
    }
}
