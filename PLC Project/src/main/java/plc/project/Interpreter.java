package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        //throw new UnsupportedOperationException(); //TODO
        for (Ast.Field iter : ast.getFields()) {
            visit(iter);
        }
        for (Ast.Method iter : ast.getMethods()) {
            visit(iter);
        }
        List<Environment.PlcObject> list = new ArrayList<>();
        return scope.lookupFunction("main", 0).invoke(list);
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        //throw new UnsupportedOperationException(); //TODO
        String name = ast.getName();
        if (ast.getValue().isPresent()) {
            Environment.PlcObject value = visit(ast.getValue().get());
            scope.defineVariable(name, value);
        }
        else {
            scope.defineVariable(name, Environment.NIL);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        //throw new UnsupportedOperationException(); //TODO
        Scope currentScope = new Scope(scope);
        scope.defineFunction(ast.getName(), ast.getParameters().size(), functions -> {
            try {
                scope = new Scope(currentScope);
                int parameters = ast.getParameters().size();
                for (int i = 0; i < parameters; i++) {
                    scope.defineVariable(ast.getParameters().get(i), functions.get(i));
                }

                for (Ast.Stmt statement : ast.getStatements()) {
                    visit(statement);
                }

            }
            catch (Return returnException) {
                return returnException.value;
            }
            finally {
                scope = scope.getParent();
            }
            return Environment.NIL;
        });


        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        //throw new UnsupportedOperationException(); //TODO
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        //throw new UnsupportedOperationException(); //TODO (in lecture)
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        //throw new UnsupportedOperationException(); //TODO
        if (ast.getReceiver().getClass() != Ast.Expr.Access.class) {
            throw new RuntimeException("Receiver is not Ast.Expr.Access type");
        }
        else {
            Ast.Expr.Access tmp = (Ast.Expr.Access)ast.getReceiver();
            if (tmp.getReceiver().isPresent()) {
                Environment.PlcObject receiver = visit(tmp.getReceiver().get());
                receiver.setField(tmp.getName(), visit(ast.getValue()));
            }
            else {
                scope.lookupVariable(tmp.getName()).setValue(visit(ast.getValue()));
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        //throw new UnsupportedOperationException(); //TODO
        if(requireType(Boolean.class, visit(ast.getCondition()))) {
            scope = new Scope(scope);
            for(Ast.Stmt statement1: ast.getThenStatements()) {
                visit(statement1);
            }
        }
        else {
            for(Ast.Stmt statement2: ast.getElseStatements()) {
                visit(statement2);
            }
        }
        scope = scope.getParent();
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        //throw new UnsupportedOperationException(); //TODO
        Iterator iter = requireType(Iterable.class, visit(ast.getValue())).iterator();
        Scope currentScope = new Scope(scope);
        while (iter.hasNext()) {
            try {
                scope = new Scope(currentScope);
                String name = ast.getName();
                Environment.PlcObject obj = (Environment.PlcObject) iter.next();
                scope.defineVariable(name,obj);
                for (Ast.Stmt statement : ast.getStatements()) {
                    visit(statement);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        //throw new UnsupportedOperationException(); //TODO (in lecture)
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        //throw new UnsupportedOperationException(); //TODO
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        //throw new UnsupportedOperationException(); //TODO
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        //throw new UnsupportedOperationException(); //TODO
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {

        //throw new UnsupportedOperationException(); //TODO

        String operator = ast.getOperator();
        if (operator.equals("AND")) {
            if(requireType(Boolean.class, visit(ast.getLeft()))) {
                if (ast.getLeft().equals(Boolean.TRUE)) {
                    if (requireType(Boolean.class, visit(ast.getRight()))) {
                        if (ast.getRight().equals(Boolean.TRUE)) {
                            return Environment.create(Boolean.TRUE);
                        }
                        else {
                            return Environment.create(Boolean.FALSE);
                        }
                    }
                }
                else {
                    return Environment.create(Boolean.FALSE);
                }
            }
        }
        else if (operator.equals("OR")) {
            if(requireType(Boolean.class, visit(ast.getLeft())) == Boolean.TRUE) {
                return Environment.create(Boolean.TRUE);
            }
            else if (requireType(Boolean.class, visit(ast.getRight())) == Boolean.TRUE){
                return Environment.create(Boolean.TRUE);
            }
            else {
                return Environment.create(Boolean.FALSE);
            }
        }

        else if (operator.equals("<") || operator.equals("<=") || operator.equals(">") || operator.equals(">=")) {
            Environment.PlcObject o1 = visit(ast.getLeft());
            Environment.PlcObject o2 = visit(ast.getRight());
            Comparable left = requireType(Comparable.class,o1);
            Comparable right = requireType(Comparable.class, o2);
            int remainder = 0;
            remainder = left.compareTo(right);
            if (operator.equals("<")) {
                if (remainder < 0) {
                    return Environment.create(Boolean.TRUE);
                }
                else {
                    return Environment.create(Boolean.FALSE);
                }
            }
            if (operator.equals("<=")) {
                if (remainder <= 0) {
                    return Environment.create(Boolean.TRUE);
                }
                else {
                    return Environment.create(Boolean.FALSE);
                }
            }
            if (operator.equals(">")) {
                if (remainder > 0) {
                    return Environment.create(Boolean.TRUE);
                }
                else {
                    return Environment.create(Boolean.FALSE);
                }
            }
            if (operator.equals(">=")) {
                if (remainder >= 0) {
                    return Environment.create(Boolean.TRUE);
                }
                else {
                    return Environment.create(Boolean.FALSE);
                }
            }

        }
        else if (operator.equals("==")) {
            Environment.PlcObject o1 = visit(ast.getLeft());
            Environment.PlcObject o2 = visit(ast.getRight());
            if (Objects.equals(o1,o2)) {
                return Environment.create(Boolean.TRUE);
            }
            return Environment.create(Boolean.FALSE);
        }
        else if (operator.equals("!=")) {
            Environment.PlcObject o1 = visit(ast.getLeft());
            Environment.PlcObject o2 = visit(ast.getRight());
            if (Objects.equals(o1,o2)) {
                return Environment.create(Boolean.FALSE);
            }
            return Environment.create(Boolean.TRUE);
        }

        else if (operator.equals("+")) {
            //if an operand is a String
            Object l1 = visit(ast.getLeft()).getValue();
            Object l2 = visit(ast.getRight()).getValue();
            if (l1.getClass() == String.class || l2.getClass() == String.class) {
                return Environment.create(l1.toString() + l2.toString());
            }

            else if (l1 instanceof BigInteger && l2 instanceof BigInteger) {
                BigInteger i1 = BigInteger.class.cast(visit(ast.getLeft()).getValue());
                BigInteger i2 = BigInteger.class.cast(visit(ast.getRight()).getValue());
                BigInteger result = new BigInteger(String.valueOf(i1)).add(i2);
                return Environment.create(result);

            }
            else if (l1 instanceof BigDecimal && l2 instanceof BigDecimal) {
                BigDecimal i1 = BigDecimal.class.cast(visit(ast.getLeft()).getValue());
                BigDecimal i2 = BigDecimal.class.cast(visit(ast.getRight()).getValue());
                BigDecimal result = new BigDecimal(String.valueOf(i1)).add(i2);
                return Environment.create(result);
            }
            else {
                throw new RuntimeException("Incompatible types");
            }
        }

        else if (operator.equals("-")) {
            Object l1 = visit(ast.getLeft()).getValue();
            Object l2 = visit(ast.getRight()).getValue();

            if (l1 instanceof BigInteger && l2 instanceof BigInteger) {
                BigInteger i1 = BigInteger.class.cast(visit(ast.getLeft()).getValue());
                BigInteger i2 = BigInteger.class.cast(visit(ast.getRight()).getValue());
                BigInteger result = new BigInteger(String.valueOf(i1)).subtract(i2);
                return Environment.create(result);
            }
            else if (l1 instanceof BigDecimal && l2 instanceof BigDecimal) {
                BigDecimal i1 = BigDecimal.class.cast(visit(ast.getLeft()).getValue());
                BigDecimal i2 = BigDecimal.class.cast(visit(ast.getRight()).getValue());
                BigDecimal result = new BigDecimal(String.valueOf(i1)).subtract(i2);
                return Environment.create(result);
            }
            else {
                throw new RuntimeException("Incompatible Types");
            }
        }

        else if (operator.equals("*")) {
            Object l1 = visit(ast.getLeft()).getValue();
            Object l2 = visit(ast.getRight()).getValue();

            if (l1 instanceof BigInteger) {
                if (l2 instanceof BigInteger) {
                    BigInteger i1 = BigInteger.class.cast(visit(ast.getLeft()).getValue());
                    BigInteger i2 = BigInteger.class.cast(visit(ast.getRight()).getValue());
                    BigInteger result = new BigInteger(String.valueOf(i1)).multiply(i2);
                    return Environment.create(result);

                }
                else {
                    throw new RuntimeException("Right operand should be BigInteger");
                }
            }
            else if (l1 instanceof BigDecimal) {
                if (l2 instanceof BigDecimal) {
                    BigDecimal i1 = BigDecimal.class.cast(visit(ast.getLeft()).getValue());
                    BigDecimal i2 = BigDecimal.class.cast(visit(ast.getRight()).getValue());
                    BigDecimal result = new BigDecimal(String.valueOf(i1)).multiply(i2);
                    return Environment.create(result);
                }
                else {
                    throw new RuntimeException("Right operand should be BigDecimal");
                }
            }

        }
        else if (operator.equals("/")) {
            Object l1 = visit(ast.getLeft()).getValue();
            Object l2 = visit(ast.getRight()).getValue();
            if (l1 instanceof BigInteger && l2 instanceof BigInteger) {
                BigInteger i1 = (BigInteger) ((Ast.Expr.Literal) ast.getLeft()).getLiteral();
                BigInteger i2 = (BigInteger) ((Ast.Expr.Literal) ast.getRight()).getLiteral();
                if (Objects.equals(i2, BigInteger.ZERO)) {
                    throw new RuntimeException("Divisor is zero!");
                }
                BigInteger result = new BigInteger(String.valueOf(i1)).divide(i2);
                return Environment.create(result);
            }
            else if (l1 instanceof BigDecimal && l2 instanceof BigDecimal) {
                BigDecimal i1 = (BigDecimal) ((Ast.Expr.Literal) ast.getLeft()).getLiteral();
                BigDecimal i2 = (BigDecimal) ((Ast.Expr.Literal) ast.getRight()).getLiteral();
                if (Objects.equals(i2, BigDecimal.ZERO)) {
                    throw new RuntimeException("Divisor is zero!");
                }
                BigDecimal result = i1.divide(i2, 1, RoundingMode.HALF_EVEN);
                return Environment.create(result);
            }
            else {
                throw new RuntimeException("Incompatible Types");
            }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {

        //throw new UnsupportedOperationException(); //TODO
        /*if (ast.getReceiver().isPresent()) {
            Optional<Ast.Expr> tmp = ast.getReceiver();
            String s = tmp.toString();
            s = s.substring(s.lastIndexOf("=") + 1);
            s = s.substring(1, s.length()-3);
            return Environment.create(s + "." + ast.getName());

        }
        else {
            return Environment.create(ast.getName());
        }*/
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            return receiver.getField(ast.getName()).getValue();
        }
        else {
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        //throw new UnsupportedOperationException(); //TODO
        List<Environment.PlcObject> list= new ArrayList<Environment.PlcObject>();
        List<Ast.Expr> arguments = ast.getArguments();
        for (Ast.Expr iter : arguments) {
            list.add(visit(iter));
        }

        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            return receiver.callMethod(ast.getName(), list);
        }
        else {
            return scope.lookupFunction(ast.getName(), ast.getArguments().size()).invoke(list);
        }

    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}

