package wikidatadictionarygenerator;

import java.util.List;

public class Utils {
    public static List<Character> charArrayToCharacterList(char[] chars) {
        return new String(chars).chars().mapToObj(i -> (Character) (char) i).toList();
    }
}
