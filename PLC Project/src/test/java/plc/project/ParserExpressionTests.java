package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Standard JUnit5 parameterized tests. See the RegexTests file from Homework 1
 * or the LexerTests file from the last project part for more information.
 */
final class ParserExpressionTests {

    @ParameterizedTest
    @MethodSource
    void testExpressionStatement(String test, List<Token> tokens, Ast.Stmt.Expression expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testExpressionStatement() {
        return Stream.of(
                Arguments.of("Function Expression",
                        Arrays.asList(
                                //name();
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5),
                                new Token(Token.Type.OPERATOR, ";", 6)
                        ),
                        new Ast.Stmt.Expression(new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList()))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStatement(String test, List<Token> tokens, Ast.Stmt.Assignment expected) {
        test(tokens, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testAssignmentStatement() {
        return Stream.of(
                Arguments.of("Assignment",
                        Arrays.asList(
                                //name = value;
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "=", 5),
                                new Token(Token.Type.IDENTIFIER, "value", 7),
                                new Token(Token.Type.OPERATOR, ";", 12)
                        ),
                        new Ast.Stmt.Assignment(
                                new Ast.Expr.Access(Optional.empty(), "name"),
                                new Ast.Expr.Access(Optional.empty(), "value")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpression(String test, List<Token> tokens, Ast.Expr.Literal expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testLiteralExpression() {
        return Stream.of(
                Arguments.of("Boolean Literal",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "TRUE", 0)),
                        new Ast.Expr.Literal(Boolean.TRUE)
                ),
                Arguments.of("Integer Literal",
                        Arrays.asList(new Token(Token.Type.INTEGER, "1", 0)),
                        new Ast.Expr.Literal(new BigInteger("1"))
                ),
                Arguments.of("Decimal Literal",
                        Arrays.asList(new Token(Token.Type.DECIMAL, "2.0", 0)),
                        new Ast.Expr.Literal(new BigDecimal("2.0"))
                ),
                Arguments.of("Character Literal",
                        Arrays.asList(new Token(Token.Type.CHARACTER, "'c'", 0)),
                        new Ast.Expr.Literal('c')
                ),
                Arguments.of("String Literal",
                        Arrays.asList(new Token(Token.Type.STRING, "\"string\"", 0)),
                        new Ast.Expr.Literal("string")
                ),
                Arguments.of("Escape Character",
                        Arrays.asList(new Token(Token.Type.STRING, "\"Hello,\\nWorld!\"", 0)),
                        new Ast.Expr.Literal("Hello,\nWorld!")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpression(String test, List<Token> tokens, Ast.Expr.Group expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testGroupExpression() {
        return Stream.of(
                Arguments.of("Grouped Variable",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expr.Group(new Ast.Expr.Access(Optional.empty(), "expr"))
                ),
                /*Arguments.of("No parenthesis",
                        Arrays.asList(
                                //(expr)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr", 1)
                        ),
                        new Ast.Expr.Group(new Ast.Expr.Access(Optional.empty(), "expr"))
                ),*/
                Arguments.of("Grouped Binary",
                        Arrays.asList(
                                //(expr1 + expr2)
                                new Token(Token.Type.OPERATOR, "(", 0),
                                new Token(Token.Type.IDENTIFIER, "expr1", 1),
                                new Token(Token.Type.OPERATOR, "+", 7),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9),
                                new Token(Token.Type.OPERATOR, ")", 14)
                        ),
                        new Ast.Expr.Group(new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        ))
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, List<Token> tokens, Ast.Expr.Binary expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("Binary And",
                        Arrays.asList(
                                //expr1 AND expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "AND", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expr.Binary("AND",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary OR",
                        Arrays.asList(
                                //expr1 AND expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.IDENTIFIER, "OR", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 10)
                        ),
                        new Ast.Expr.Binary("OR",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Equality",
                        Arrays.asList(
                                //expr1 == expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "==", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 9)
                        ),
                        new Ast.Expr.Binary("==",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Addition",
                        Arrays.asList(
                                //expr1 + expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "+", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expr.Binary("+",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                ),
                Arguments.of("Binary Multiplication",
                        Arrays.asList(
                                //expr1 * expr2
                                new Token(Token.Type.IDENTIFIER, "expr1", 0),
                                new Token(Token.Type.OPERATOR, "*", 6),
                                new Token(Token.Type.IDENTIFIER, "expr2", 8)
                        ),
                        new Ast.Expr.Binary("*",
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAccessExpression(String test, List<Token> tokens, Ast.Expr.Access expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testAccessExpression() {
        return Stream.of(
                Arguments.of("Variable",
                        Arrays.asList(new Token(Token.Type.IDENTIFIER, "name", 0)),
                        new Ast.Expr.Access(Optional.empty(), "name")
                ),
                Arguments.of("Field Access",
                        Arrays.asList(
                                //obj.field
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "field", 4)
                        ),
                        new Ast.Expr.Access(Optional.of(new Ast.Expr.Access(Optional.empty(), "obj")), "field")
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpression(String test, List<Token> tokens, Ast.Expr.Function expected) {
        test(tokens, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Zero Arguments",
                        Arrays.asList(
                                //name()
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.OPERATOR, ")", 5)
                        ),
                        new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList())
                ),
                Arguments.of("Multiple Arguments",
                        Arrays.asList(
                                //name(expr1, expr2, expr3)
                                new Token(Token.Type.IDENTIFIER, "name", 0),
                                new Token(Token.Type.OPERATOR, "(", 4),
                                new Token(Token.Type.IDENTIFIER, "expr1", 5),
                                new Token(Token.Type.OPERATOR, ",", 10),
                                new Token(Token.Type.IDENTIFIER, "expr2", 12),
                                new Token(Token.Type.OPERATOR, ",", 17),
                                new Token(Token.Type.IDENTIFIER, "expr3", 19),
                                new Token(Token.Type.OPERATOR, ")", 24)
                        ),
                        new Ast.Expr.Function(Optional.empty(), "name", Arrays.asList(
                                new Ast.Expr.Access(Optional.empty(), "expr1"),
                                new Ast.Expr.Access(Optional.empty(), "expr2"),
                                new Ast.Expr.Access(Optional.empty(), "expr3")
                        ))
                ),
                Arguments.of("Method Call",
                        Arrays.asList(
                                //obj.method()
                                new Token(Token.Type.IDENTIFIER, "obj", 0),
                                new Token(Token.Type.OPERATOR, ".", 3),
                                new Token(Token.Type.IDENTIFIER, "method", 4),
                                new Token(Token.Type.OPERATOR, "(", 10),
                                new Token(Token.Type.OPERATOR, ")", 11)
                        ),
                        new Ast.Expr.Function(Optional.of(new Ast.Expr.Access(Optional.empty(), "obj")), "method", Arrays.asList())
                )
        );
    }


    /**
     * Standard test function. If expected is null, a ParseException is expected
     * to be thrown (not used in the provided tests).
     */
    private static <T extends Ast> void test(List<Token> tokens, T expected, Function<Parser, T> function) {
        Parser parser = new Parser(tokens);
        if (expected != null) {
            Assertions.assertEquals(expected, function.apply(parser));
        } else {
            Assertions.assertThrows(ParseException.class, () -> function.apply(parser));
        }
    }

}

/*package plc.project;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.BigInteger;


*//**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 *//*
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    *//**
 * Parses the {@code source} rule.
 *//*
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    *//**
 * Parses the {@code field} rule. This method should only be called if the
 * next tokens start a field, aka {@code LET}.
 *//*
    public Ast.Field parseField() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    *//**
 * Parses the {@code method} rule. This method should only be called if the
 * next tokens start a method, aka {@code DEF}.
 *//*
    public Ast.Method parseMethod() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    *//**
 * Parses the {@code statement} rule and delegates to the necessary method.
 * If the next tokens do not start a declaration, if, while, or return
 * statement, then it is an expression/assignment statement.
 *//*
    public Ast.Stmt parseStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        //'LET' identifier ('=' expression )? ';'
        if (peek("LET")) {
            return parseDeclarationStatement();
        }
        // expression ('=' expression)? '?'
        else {
            Ast.Expr expr = parseExpression();


            //FIXME: handle if next token is =, then handle assignment
            if (match("=")) {
                Ast.Expr expr2 = parseExpression();

                if (!match(";")) {
                    //FIXME: char index
                    throw new ParseException("Expected semicolon", -1);
                }

                return new Ast.Stmt.Assignment(expr, expr2);
            }

            if (!match(";")) {
                //FIXME: char index
                throw new ParseException("Expected semicolon", -1);
            }
            return new Ast.Stmt.Expression(expr);
        }
    }

    *//**
 * Parses a declaration statement from the {@code statement} rule. This
 * method should only be called if the next tokens start a declaration
 * statement, aka {@code LET}.
 *//*
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        //'LET' identifier ('=' expression )? ';'
        match("LET");

        if (!match(Token.Type.IDENTIFIER)) {
            //FIXME: char index
            throw new ParseException("Expected identifier", -1);
        }

        //get variable name
        String name = tokens.get(-1).getLiteral();

        Optional<Ast.Expr> value = Optional.empty();

        //
        if (match("=")) {
            value = Optional.of(parseExpression());
        }

        if (!match(";")) {
            //FIXME: char index
            throw new ParseException("Expected semicolon", -1);
        }

        return new Ast.Stmt.Declaration(name, value);
    }

    *//**
 * Parses an if statement from the {@code statement} rule. This method
 * should only be called if the next tokens start an if statement, aka
 * {@code IF}.
 *//*
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    *//**
 * Parses a for statement from the {@code statement} rule. This method
 * should only be called if the next tokens start a for statement, aka
 * {@code FOR}.
 *//*
    public Ast.Stmt.For parseForStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    *//**
 * Parses a while statement from the {@code statement} rule. This method
 * should only be called if the next tokens start a while statement, aka
 * {@code WHILE}.
 *//*
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    *//**
 * Parses a return statement from the {@code statement} rule. This method
 * should only be called if the next tokens start a return statement, aka
 * {@code RETURN}.
 *//*
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    *//**
 * Parses the {@code expression} rule.
 *//*
    public Ast.Expr parseExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        return parseLogicalExpression();
    }

    *//**
 * Parses the {@code logical-expression} rule.
 *//*
    public Ast.Expr parseLogicalExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        Ast.Expr expr1 = parseEqualityExpression();

        boolean andOrFound = false;
        String word = "";

        if (match("AND") || !match("OR")) {
            andOrFound = true;
            word = tokens.get(-1).getLiteral();
        }

        Ast.Expr value;

        while (andOrFound) {
            Ast.Expr expr2 = parseEqualityExpression();

            value = new Ast.Expr.Binary(word, expr1, expr2);
            if (match("AND") || !match("OR")) {
                word = tokens.get(-1).getLiteral();
                expr1 = value;
            }
            else {
                return value;
            }
        }


        return expr1;
    }

    *//**
 * Parses the {@code equality-expression} rule.
 *//*
    public Ast.Expr parseEqualityExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        Ast.Expr expr1 = parseAdditiveExpression();

        boolean compFound = false;
        String word = "";

        if (match("<") || !match("<=") || !match(">") || !match(">=") || !match("==") || !match("!=")) {
            compFound = true;
            word = tokens.get(-1).getLiteral();
        }

        Ast.Expr value;

        while (compFound) {
            Ast.Expr expr2 = parseAdditiveExpression();

            value = new Ast.Expr.Binary(word, expr1, expr2);
            if (match("<") || !match("<=") || !match(">") || !match(">=") || !match("==") || !match("!=")) {
                word = tokens.get(-1).getLiteral();
                expr1 = value;
            }
            else {
                return value;
            }
        }


        return expr1;
    }

    *//**
 * Parses the {@code additive-expression} rule.
 *//*
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        Ast.Expr expr1 = parseMultiplicativeExpression();

        boolean plusMinusFound = false;
        String word = "";

        if (match("+") || !match("-")) {
            plusMinusFound = true;
            word = tokens.get(-1).getLiteral();
        }

        Ast.Expr value;

        while (plusMinusFound) {
            Ast.Expr expr2 = parseMultiplicativeExpression();

            value = new Ast.Expr.Binary(word, expr1, expr2);
            if (match("AND") || !match("OR")) {
                word = tokens.get(-1).getLiteral();
                expr1 = value;
            }
            else {
                return value;
            }
        }


        return expr1;
    }

    *//**
 * Parses the {@code multiplicative-expression} rule.
 *//*
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        Ast.Expr expr1 = parseSecondaryExpression();

        boolean timesDivideFound = false;
        String word = "";

        if (match("*") || !match("/")) {
            timesDivideFound = true;
            word = tokens.get(-1).getLiteral();
        }

        Ast.Expr value;

        while (timesDivideFound) {
            Ast.Expr expr2 = parseSecondaryExpression();

            value = new Ast.Expr.Binary(word, expr1, expr2);
            if (match("AND") || !match("OR")) {
                word = tokens.get(-1).getLiteral();
                expr1 = value;
            }
            else {
                return value;
            }
        }


        return expr1;
    }

    *//**
 * Parses the {@code secondary-expression} rule.
 *//*
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        List<Ast.Expr> list = new ArrayList<Ast.Expr>();

        Ast.Expr expr1 = parsePrimaryExpression();
        String name = tokens.get(-1).getLiteral();

        while (peek(".", Token.Type.IDENTIFIER)) {
            if (peek(".", Token.Type.IDENTIFIER, "(")) {
                if(peek("(")){
                    match("(");
                    while(!peek(")")) {
                        if (peek(",")) {
                            match(",");
                        } else{
                            list.add(new Ast.Expr.Access(Optional.empty(), tokens.get(0).getLiteral()));
                            match(tokens.get(0).getLiteral());
                        }
                    }
                    if(peek(")")) {
                        match(")");
                    }

                    else{
                        throw new ParseException("Missing Parenthesis", tokens.index);
                    }
                }

                else{
                    return new Ast.Expr.Access(Optional.of(expr1), name);
                }

                return new Ast.Expr.Function(Optional.of(expr1), name, list);
            }
            else {
                expr1 = new Ast.Expr.Access(Optional.of(expr1), tokens.get(1).getLiteral());
                match(".", Token.Type.IDENTIFIER);
            }
        }
        if (peek(".")) {
            if (!tokens.has(0))
                throw new ParseException("Invalid", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            else
                throw new ParseException("Missing parenthesis", tokens.get(0).getIndex());
        }
        return expr1;
    }

    *//**
 * Parses the {@code primary-expression} rule. This is the top-level rule
 * for expressions and includes literal values, grouping, variables, and
 * functions. It may be helpful to break these up into other methods but is
 * not strictly necessary.
 *//*
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        if (match("TRUE")) {
            return new Ast.Expr.Literal(true);
        }

        else if (match("FALSE")) {
            return new Ast.Expr.Literal(false);
        }

        else if (match("NIL")) {
            return new Ast.Expr.Literal(null);
        }

        else if (match(Token.Type.INTEGER)) {
            String name = tokens.get(-1).getLiteral();
            return new Ast.Expr.Literal(new BigInteger(name));
        }

        else if (match(Token.Type.DECIMAL)) {
            String name = tokens.get(-1).getLiteral();
            return new Ast.Expr.Literal(new BigDecimal(name));
        }

        else if (match(Token.Type.CHARACTER)) {
            String name = tokens.get(-1).getLiteral();
            name = name.replaceAll("\'","");
            name = name.replaceAll("\\\\b", "\b");
            name = name.replaceAll("\\\\n", "\n");
            name = name.replaceAll("\\\\r", "\r");
            name = name.replaceAll("\\\\t", "\t");
            name = name.replaceAll("\\\\'", "'");
            name = name.replaceAll("\\\\\"", "\"");
            name = name.replaceAll("\\\\", "\\");
            return new Ast.Expr.Literal(name.charAt(0));

        }

        else if (match(Token.Type.STRING)) {
            String name = tokens.get(-1).getLiteral();
            name = name.replaceAll("\"","");
            name = name.replaceAll("\\\\b", "\b");
            name = name.replaceAll("\\\\n", "\n");
            name = name.replaceAll("\\\\r", "\r");
            name = name.replaceAll("\\\\t", "\t");
            name = name.replaceAll("\\\\'", "'");
            name = name.replaceAll("\\\\\"", "\"");
            name = name.replaceAll("\\\\", "\\");
            return new Ast.Expr.Literal(name);

        }

        else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            if (match(Token.Type.IDENTIFIER, "(")) {
                List<Ast.Expr> list = new ArrayList<>();
                if (!match(")")) {
                    list.add(parseExpression());
                }
                while (match(",") && !peek(")")) {
                    list.add(parseExpression());
                }
                if (!tokens.has(0)) {
                    throw new ParseException("No closing parenthesis",tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
                tokens.advance();
            }
            return new Ast.Expr.Access(Optional.empty(), name);
        }

        else if (match("(")) {
            Ast.Expr expr = parseExpression();
            if (!match(")")){
                throw new ParseException("Expected closing parenthesis.", -1); //TODO
            }
            return new Ast.Expr.Group(expr);
        }

        else {
            throw new ParseException("Invalid primary expression.", -1);
            // TODO: handle actual character index instead of -1
        }
    }

    *//**
 * As in the lexer, returns {@code true} if the current sequence of tokens
 * matches the given patterns. Unlike the lexer, the pattern is not a regex;
 * instead it is either a {@link Token.Type}, which matches if the token's
 * type is the same, or a {@link String}, which matches if the token's
 * literal is the same.
 *
 * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
 * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
 *//*
    private boolean peek(Object... patterns) {
        //throw new UnsupportedOperationException(); //TODO (in lecture)
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    *//**
 * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
 * and advances the token stream.
 *//*
    private boolean match(Object... patterns) {
        //throw new UnsupportedOperationException(); //TODO (in lecture)
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        *//**
 * Returns true if there is a token at index + offset.
 *//*
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        *//**
 * Gets the token at index + offset.
 *//*
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        *//**
 * Advances to the next token, incrementing the index.
 *//*
        public void advance() {
            index++;
        }

    }

}*/
