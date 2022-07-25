package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),

                //Matching
                Arguments.of("Checks any domain name as long as it is lower case and between 2-3 letters", "checkdomain@gmail.abc", true),
                Arguments.of("valid symbols", "symbols-._@A-b.com", true),
                Arguments.of("Nothing after the @ sign", "the@.edu", true),
                Arguments.of("my email", "reillywynn@ufl.edu", true),

                //Non-matching
                Arguments.of("Capitalized Domain Letters", "CapitalizedDomain@gmail.COM", false),
                Arguments.of("Too many letters in domain", "toomanyletters@gmail.comm", false),
                Arguments.of("Not enough letters in domain", "notenoughletters@gmail.c", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas?
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("6 Characters", "6chars", false),
                Arguments.of("13 Characters", "i<3pancakes9!", false),

                //Matching
                Arguments.of("all symbols", "!@#$%^&*()", true),
                Arguments.of("20 characters", "abcdefkghdoqitrjskfm", true),
                Arguments.of("random characters between 10-20", "ffj*^#jK.?-3", true),

                //Non-Matching
                Arguments.of("9 characters", "soooclose", false),
                Arguments.of("21 characters", "toooooooooooooooolong", false),
                Arguments.of("2 character", "$u", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements", "[1,2,3]", true),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),

                //Matching
                Arguments.of("empty list", "[]", true),
                Arguments.of("Mixed Spaces", "[1,2, 3,4, 5]", true),
                Arguments.of("long list and large numbers", "[1,2,12333222,343434,343434235]", true),
                Arguments.of("descending list", "[10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0]", true),

                //Non-Matching
                Arguments.of("list with a decimal number", "[0.5, 9]", false),
                Arguments.of("Letters in list", "[a,b,c]", false),
                Arguments.of("empty list but with a space", "[ ]", false),
                Arguments.of("no bracket", "1, 2, 3", false),
                Arguments.of("Trailing Comma", "[1,2,3,]", false),
                Arguments.of("Only a comma, no numbers", "[,]", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        //throw new UnsupportedOperationException(); //TODO
        test(input, Regex.NUMBER, success);
    }

    public static Stream<Arguments> testNumberRegex() {
        //throw new UnsupportedOperationException() {
        return Stream.of(
                Arguments.of("Single Digit Integer", "1", true),
                Arguments.of("Multiple Digit Decimal", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),

                //Matching
                Arguments.of("decimal number with leading zero", "0.5", true),
                Arguments.of("negative decimal number with leading zero", "-0.5", true),
                Arguments.of("multiple leading zeros", "0000.5", true),
                Arguments.of("multiple trailing zeros", "5.0000000", true),

                //Non-Matching
                Arguments.of("multiple decimals", "5.000.000", false),
                Arguments.of("both + and - sign", "+-5", false),
                Arguments.of("contains a symbol", "50%", false),
                Arguments.of("letter in the number", "5o", false)
        );
         //TODO
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        //throw new UnsupportedOperationException(); //TODO
        test (input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        //throw new UnsupportedOperationException(); //TODO
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Hello World", "\"Hello, World!\"", true),
                Arguments.of("Escape", "\"1\\t2\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),

                //Matching
                Arguments.of("Numbers", "\"1234onetwothreefour\"", true),
                Arguments.of("Valid Escape", "\"valid\\bescape\"", true),
                Arguments.of("Regular String", "\"Hello\"", true),
                Arguments.of("backslash", "\"valid\\\\escapeBackslash\"", true),

                //Non-Matching
                Arguments.of("No double quotes", "Hello, World!", false),
                Arguments.of("Invalid Escape Character", "\"invalid\\&escape\"", false),
                Arguments.of("Invalid escape character number","\"hello\\4world\"", false)


        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
