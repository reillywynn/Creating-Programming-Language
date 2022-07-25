package plc.project;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;


    Pattern CHARACTER = Pattern.compile("\'[^']|[\\\\][bnrt'\"]\'");
    Pattern STRING = Pattern.compile("\"([^\\\\\"]|[\\\\][bnrt'])*\"");

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */

    //helper function
    //source: https://www.craftinginterpreters.com/scanning.html
    private boolean isAtEnd() {
        return chars.index >= chars.input.length();
    }

    private boolean isWhitespace() {
        if (chars.get(0) == ' ') {
            return true;
        }
        else if (chars.get(0) == '\b') {
            return true;
        }
        else if (chars.get(0) == '\n') {
            return true;
        }
        else if (chars.get(0) == '\r') {
            return true;
        }
        else if (chars.get(0) == '\t') {
            return true;
        }

        return false;
    }



    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while (!isAtEnd()) {
            if (isWhitespace()) {
                chars.advance();
                chars.skip();
            }
            // We are at the beginning of the next lexeme.
            else{
                tokens.add(lexToken());
            }
        }
        return tokens;


    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if(peek("[A-Za-z_]")) {   // Identifier cannot start with a digit or hyphen so it is excluded, only starts with underscore or letter.
            return lexIdentifier();
        }
        if(peek("[+\\-]","[0-9]") || peek("[0-9]")) {  // Produces the token for a Integer/Decimal, either a number 0-9 or if it starts with a + or - , cannot start with decimal point.
            return lexNumber();
        }
        char c = chars.get(0);
        switch (c) {
            case '\"': return lexString();
            case '\'': return lexCharacter();
            case '<': return lexOperator();
            case '>': return lexOperator();
            case '!': return lexOperator();
            case '=': return lexOperator();


            default: return lexOperator();
        }

    }

    public Token lexIdentifier() {
        //throw new UnsupportedOperationException(); //TODO
        while (!isAtEnd() &&match("[A-Za-z0-9_-]")){};
        return chars.emit(Token.Type.IDENTIFIER);

    }

    public Token lexNumber() {
        //throw new UnsupportedOperationException(); //TODO
        match("[+-]"); // Handles the negative/positive numbers
        while(!isAtEnd() && match("[0-9]")){}
        if(!isAtEnd() && match("[\\.]","[0-9]")){
            while(!isAtEnd()&& match("[0-9]")){}
            return chars.emit(Token.Type.DECIMAL);
        }
        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        chars.advance();
        if (!isAtEnd()) {
            if (chars.get(0) == '\n') {
                throw new ParseException("Invalid character", chars.index);
            }
            else if (chars.get(0) == '\r') {
                throw new ParseException("Invalid character", chars.index);
            }
            else if (chars.get(0) == '\'') {
                throw new ParseException("Invalid character", chars.index);
            }
        }
        if (!isAtEnd() && chars.get(0) == '\\') {

            chars.advance();
            if (peek("b")) {
                chars.advance();
            }
            else if (peek("n")) {
                chars.advance();
            }
            else if (peek("r")) {
                chars.advance();
            }
            else if (peek("t")) {
                chars.advance();
            }
            else if (peek("'")) {
                chars.advance();
            }
            else if (peek("\"")) {
                chars.advance();
            }
            else if (peek("\\\\")) {
                chars.advance();
            }
            else {
                throw new ParseException("Invalid character", chars.index+1);
            }

        }

        else if (!isAtEnd()) {
            chars.advance();
        }

        if (isAtEnd() || chars.get(0) != '\'') {
            throw new ParseException("Unterminated character", chars.index);
        }

        else if (chars.get(-1) == '\'') {
            throw new ParseException("Empty character", chars.index-1);
        }

        // The closing '.
        chars.advance();

        // emit token
        return chars.emit(Token.Type.CHARACTER);
    }

    //https://www.craftinginterpreters.com/scanning.html
    public Token lexString() {
        chars.advance();
        while (!isAtEnd() && !peek("\"")) {

            if (chars.get(0) == '\n') {
                lexEscape();
            }
            else if (chars.get(0) == '\r') {
                lexEscape();
            }
            else if (chars.get(0) == '\"') {
                lexEscape();
            }
            if (chars.get(0) == '\\') {
                chars.advance();
                if (!isAtEnd() && !peek("b") && !peek("n") && !peek("r") && !peek("t") && !peek("'") && !peek("\\\\") && !peek("\"")) {
                    //throw new ParseException("Invalid escape", chars.index + 1);
                    lexEscape();
                }
            }

            if (!isAtEnd()) {
                chars.advance();
            }
        }


        // The closing ".
        if (!isAtEnd() && chars.get(0) == '"') {
            chars.advance();
        }
        else {
            //throw new ParseException("Unterminated string", chars.index);
            lexEscape();
        }

        // emit token
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        //throw new UnsupportedOperationException(); //TODO
        throw new ParseException("Invalid String",chars.index);
    }

    public Token lexOperator() {
        if (match("[<]")) {
            match("=");
        }
        else if (match("[>]")) {
            match("=");
        }
        else if (match("[!]")) {
            match("=");
        }
        else if (match("[=]")) {
            match("=");
        }
        else {
            chars.advance();
        }
        return chars.emit(Token.Type.OPERATOR);

    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}

