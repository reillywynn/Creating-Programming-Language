package plc.project;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
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
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> f = new ArrayList<Ast.Field>();
        List<Ast.Method> m = new ArrayList<Ast.Method>();
        while (peek("LET")) {
            Ast.Field temp = parseField();
            f.add(temp);
        }

        while (peek("DEF")) {
            Ast.Method temp = parseMethod();
            m.add(temp);
        }

        if (tokens.has(0)) {
            throw new ParseException("Token after methods", tokens.index);
        }

        return new Ast.Source(f, m);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        Ast.Stmt.Declaration s = parseDeclarationStatement();
        String name = s.getName();
        Optional<Ast.Expr> value = s.getValue();
        return new Ast.Field(name, value);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        List<String> parameters = new ArrayList<String>();
        List<Ast.Stmt> statements = new ArrayList<Ast.Stmt>();
        match("DEF");

        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier in method", tokens.index);
        }

        String name = tokens.get(-1).getLiteral();

        if (!match("(")) {
            throw new ParseException("Expected opening parenthesis in method", tokens.index);
        }

        if (match(Token.Type.IDENTIFIER)) {
            parameters.add(tokens.get(-1).getLiteral());

            while (match(",")) {
                if (!match(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected parameter in method after comma", tokens.index);
                }
                parameters.add(tokens.get(-1).getLiteral());
            }
        }

        if (!match(")")) {
            throw new ParseException("Expected closing parenthesis in method", tokens.index);
        }

        if (!match("DO")) {
            throw new ParseException("Expected DO in method", tokens.index);
        }

        while (!peek("END")) {
            statements.add(parseStatement());
        }

        if (!match("END")) {
            throw new ParseException("Expected END in method", tokens.index);
        }

        return new Ast.Method(name, parameters, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        //'LET' identifier ('=' expression )? ';'
        if (peek("LET")) {
            return parseDeclarationStatement();
        }
        if (peek("IF")) {
            return parseIfStatement();
        }
        if (peek("FOR")) {
            return parseForStatement();
        }
        if (peek("WHILE")) {
            return parseWhileStatement();
        }
        if (peek("RETURN")) {
            return parseReturnStatement();
        }

        // expression ('=' expression)? '?'
        else {
            Ast.Expr expr = parseExpression();


            //FIXME: handle if next token is =, then handle assignment
            if (match("=")) {
                Ast.Expr expr2 = parseExpression();

                if (!match(";")) {
                    //FIXME: char index
                    throw new ParseException("Expected semicolon after expr after equals sign", tokens.index);
                }

                return new Ast.Stmt.Assignment(expr, expr2);
            }


            if (!match(";")) {
                //FIXME: char index
                throw new ParseException("Expected semicolon after expr" + expr.toString(), tokens.index);
            }

            return new Ast.Stmt.Expression(expr);
        }

    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        //'LET' identifier ('=' expression )? ';'
        match("LET");

        if (!match(Token.Type.IDENTIFIER)) {
            //FIXME: char index
            throw new ParseException("Expected identifier", tokens.index);
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
            throw new ParseException("Expected semicolon", tokens.index);
        }

        return new Ast.Stmt.Declaration(name, value);

    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("IF");
        Ast.Expr expr1 = parseExpression();

        List<Ast.Stmt> DO_Statements = new ArrayList<>();
        List<Ast.Stmt> ELSE_Statements = new ArrayList<>();

        if (match("DO")) {
            while (!peek("ELSE") && !peek("END"))
                DO_Statements.add(parseStatement());

            if (match("ELSE")) {
                while (!peek("END"))
                    ELSE_Statements.add(parseStatement());
            }
        }

        else {
            if (tokens.has(0)) {
                throw new ParseException("Expected DO, Received different statement" + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            }
            else {
                throw new ParseException("Missing DO" + tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length(), tokens.index);
            }
        }

        if (match ("END")) {
            return new Ast.Stmt.If(expr1,DO_Statements,ELSE_Statements);
        }

        else {
            throw new ParseException("Missing END", tokens.index);
        }
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("FOR");
        String word = "";

        // Get identifier
        if (match(Token.Type.IDENTIFIER)) {
            word = tokens.get(-1).getLiteral();
        }
        else {
            throw new ParseException("No identifier after FOR",tokens.index);
        }

        if (!match("IN")) {
            throw new ParseException("Expected IN", tokens.index);
        }
        else {
            match("IN");
        }

        Ast.Expr expr1 = parseExpression();

        if (!match("DO")) {
            if (tokens.has(0)) {
                throw new ParseException("Expected DO, Received different statement", tokens.get(0).getIndex());
            }
            else {
                throw new ParseException("Expected DO", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
        else {
            match("DO");
        }

        List<Ast.Stmt> DO_Statements = new ArrayList<Ast.Stmt>();

        while (!peek("END")) {
            DO_Statements.add(parseStatement());
        }

        if (!match("END")) {
            throw new ParseException("Expected END at the end", tokens.index);
        }
        else {
            match("END");
            return new Ast.Stmt.For(word, expr1, DO_Statements);
        }

    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("WHILE");

        Ast.Expr expr1 = parseExpression();

        if (!match("DO")) {
            if (tokens.has(0)) {
                throw new ParseException("Expected DO, Received different statement", tokens.get(0).getIndex());
            }
            else {
                throw new ParseException("Expected DO", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
        else {
            match("DO");
        }

        List<Ast.Stmt> DO_Statements = new ArrayList<Ast.Stmt>();

        while (!peek("END"))
            DO_Statements.add(parseStatement());

        if (!match("END")) {
            throw new ParseException("Expected END", tokens.index);
        }
        else {
            match("DO");
            return new Ast.Stmt.While(expr1, DO_Statements);
        }
    }


    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("RETURN");

        Ast.Expr expr1 = parseExpression();

        if (!match(";")) {
            throw new ParseException("Expected a semicolon", tokens.index);
        }
        else {
            match(";");
            return new Ast.Stmt.Return(expr1);
        }
    }


    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        Ast.Expr expr1 = parseEqualityExpression();

        String word = "";

        while (match("AND") || match("OR")) {
            word = tokens.get(-1).getLiteral();
            Ast.Expr expr2 = parseEqualityExpression();
            expr1 = new Ast.Expr.Binary(word, expr1, expr2);
        }
        return expr1;

    }


    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        Ast.Expr expr1 = parseAdditiveExpression();

        String word = "";

        while (match("<") || match("<=") || match(">") || match(">=") || match("==") || match("!=")) {
            word = tokens.get(-1).getLiteral();
            Ast.Expr expr2 = parseAdditiveExpression();
            expr1 = new Ast.Expr.Binary(word,expr1, expr2);
        }
        return expr1;

    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        Ast.Expr expr1 = parseMultiplicativeExpression();

        String word = "";

        while (match("+") || match("-")) {
            word = tokens.get(-1).getLiteral();
            Ast.Expr expr2 = parseMultiplicativeExpression();
            expr1 = new Ast.Expr.Binary(word,expr1,expr2);
        }
        return expr1;

    }


    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr expr1 = parseSecondaryExpression();

        String word = "";

        while (match("*") || match("/")) {
            word = tokens.get(-1).getLiteral();
            Ast.Expr expr2 = parseSecondaryExpression();
            expr1 = new Ast.Expr.Binary(word,expr1,expr2);
        }

        return expr1;

    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        Ast.Expr expr1 = parsePrimaryExpression();
        List<Ast.Expr> list = new ArrayList<>();
        //If it doesnt match on a period then it goes on to be a primary expression
        if (!match(".")) {
            return expr1;
        }

        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier after period in secondary expression", tokens.index);
        }

        String name = tokens.get(0).getLiteral();
        tokens.advance();

        if (!match("(")) {
            return new Ast.Expr.Access(Optional.of(expr1), name);
        }

        if (!match(")")) {
            list.add(parseExpression());
        }

        Ast.Expr value;
        value = new Ast.Expr.Function(Optional.of(expr1), name, list);
        return value;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
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
            switch (name) {
                case "'\b'":
                    return new Ast.Expr.Literal('\b');

                case "'\n'":
                    return new Ast.Expr.Literal('\n');

                case "'\r'":
                    return new Ast.Expr.Literal('\r');

                case "'\t'":
                    return new Ast.Expr.Literal('\t');

                case "\'":
                    return new Ast.Expr.Literal('\'');

                case "'\"'":
                    return new Ast.Expr.Literal('\"');

                case "'\'":
                    return new Ast.Expr.Literal('\\');

                default:
                    return new Ast.Expr.Literal(name.charAt(1));
            }
        }
        else if (match(Token.Type.STRING)) {
            String name = tokens.get(-1).getLiteral();
            name = name.replaceAll("\"","");
            name = name.replaceAll("\\\\b", "\b");
            name = name.replaceAll("\\\\n", "\n");
            name = name.replaceAll("\\\\r", "\r");
            name = name.replaceAll("\\\\t", "\t");
            return new Ast.Expr.Literal(name);

        }
        else if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            if (match(Token.Type.IDENTIFIER, "(")) {
                List<Ast.Expr> list = new ArrayList<>();
                if(!match(")")) {
                    list.add(parseExpression());
                    while (tokens.has(0) && !peek(")")) {
                        if(!match(",")){
                            throw new ParseException("No comma between arguments", tokens.index);
                        }
                        list.add(parseExpression());
                    }
                    if (!match(")")) {
                        throw new ParseException("No closing parenthesis in function call", tokens.index);
                    }
                }

                return new Ast.Expr.Function(Optional.empty(), name, list);
            }

            else {
                tokens.advance();
                return new Ast.Expr.Access(Optional.empty(), name);
            }
        }

        else if(match("(")) {
            Ast.Expr expr = parseExpression();
            if (!match(")")) {
                throw new ParseException("Expected closing parentheses ", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            return new Ast.Expr.Group(expr);
        }

        else {
            throw new ParseException("Invalid primary expression.", tokens.index);
        }
    }


    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for(int i = 0; i < patterns.length; i ++){
            if(!tokens.has(i)) {
                return false;
            }else if(patterns[i] instanceof Token.Type) {
                if(patterns[i] != tokens.get(i).getType()){
                    return false;
                }
            }else if (patterns[i] instanceof String){
                if(!patterns[i].equals((tokens.get(i).getLiteral()))){
                    return false;
                }
            }else {
                throw new ParseException("not valid pattern object" , i);
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if(peek) {
            for(int i = 0; i < patterns.length; i ++) {
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

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}










