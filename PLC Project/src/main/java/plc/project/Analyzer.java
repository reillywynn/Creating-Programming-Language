package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (Ast.Field iter : ast.getFields()) {
            visit(iter);
        }
        for (Ast.Method iter : ast.getMethods()) {
            visit(iter);
        }
        Environment.Function m = scope.lookupFunction("main", 0); //throws runtime exception if main not found
        requireAssignable(Environment.Type.INTEGER, m.getReturnType());
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
        }

        String name = ast.getName();
        Environment.Type type = Environment.getType(ast.getTypeName());
        ast.setVariable(scope.defineVariable(name, name, type, Environment.NIL));

        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        String name = ast.getName();
        List<Environment.Type> parameterTypes = new ArrayList<>();
        Optional<String> t = ast.getReturnTypeName();
        Environment.Type returnType = Environment.Type.NIL;

        if (t.isPresent()) {
            returnType = Environment.getType(t.get());
        }
        scope.defineFunction(name, name, parameterTypes, returnType, args -> Environment.NIL);
        ast.setFunction(scope.lookupFunction(name, parameterTypes.size()));

        scope = new Scope(scope);
        for (String typeName : ast.getParameterTypeNames()) {
            parameterTypes.add(Environment.getType(typeName));
        }

        scope.defineVariable("returnType", returnType.getName(), returnType, Environment.NIL);
        for (int i = 0; i < ast.getStatements().size(); i++) {
            visit(ast.getStatements().get(i));
        }
        scope = scope.getParent();

        return null;

        //Environment.PlcObject
        //need to fix function here
        /*if (t.isPresent()) {
            String t2 = t.get();
            java.util.function.Function<List<Environment.PlcObject>, Environment.PlcObject> f = new java.util.function.Function<ast.getParameters(), Environment.NIL>();
            ast.setFunction(scope.defineFunction(name, name, parameterTypes, Environment.getType(t.get()), f));
        }
        else {
            java.util.function.Function<List<Environment.PlcObject>, Environment.PlcObject> f = new java.util.function.Function<ast.getParameters(), Environment.NIL>();
            ast.setFunction(scope.defineFunction(name, name, parameterTypes, Environment.NIL, f));
        }*/
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        Object expr = ast.getExpression();
        if (!(expr instanceof Ast.Expr.Function)) {
            throw new RuntimeException("Statement expression cannot be function!");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        if (!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) {
            throw new RuntimeException("Declaration must have type or value to infer type!");
        }

        Environment.Type type = null;

        if (ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());

            if (type == null) {
                type = ast.getValue().get().getType();
            }

            requireAssignable(type, ast.getValue().get().getType());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL));

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {

        if (!(ast.getReceiver() instanceof Ast.Expr.Access)) {
            throw new RuntimeException("Receiver of assignment must be access expression!");
        }
        visit(ast.getReceiver());
        visit(ast.getValue());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {

        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("Then statements empty for if statement!");
        }

        List<Ast.Stmt> statements = ast.getThenStatements();
        for (Ast.Stmt stmt : statements) {
            scope = new Scope(scope);

            visit(stmt);
            /*
            if (stmt instanceof Ast.Stmt.Expression) {
                Ast.Expr expr = ((Ast.Stmt.Expression) stmt).getExpression();
                if (expr instanceof Ast.Expr.Function) {
                    Ast.Expr.Function f = (Ast.Expr.Function) expr;
                    if (f.getType() == Environment.Type.INTEGER) {
                        if (f.)
                    }
                }
            }

             */


            scope = scope.getParent();
        }

        statements = ast.getElseStatements();
        for (Ast.Stmt stmt : statements) {
            scope = new Scope(scope);
            visit(stmt);
            scope = scope.getParent();
        }



        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        visit(ast.getValue());
        requireAssignable(Environment.Type.INTEGER_ITERABLE, ast.getValue().getType());
        if (ast.getStatements().isEmpty()) {
            throw new RuntimeException("For statement statements list is empty!");
        }

        for (Ast.Stmt stmt : ast.getStatements()) {
            try {
                scope = new Scope(scope);
                String name = ast.getName();
                scope.defineVariable(name, name, Environment.Type.INTEGER, Environment.NIL);
                visit(stmt);
            }
            finally {
                scope = scope.getParent();
            }
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        Environment.Type type = ast.getValue().getType();
        Ast.Expr val = ast.getValue();
        visit(val);
        Environment.Variable validate = scope.lookupVariable("returnType");
        requireAssignable(type, validate.getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        //throw new UnsupportedOperationException();  // TODO
        Object type = ast.getLiteral();
        if (type == Environment.NIL) {
            ast.setType(Environment.Type.NIL);
        }
        else if (type instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (type instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (type instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        else if (type instanceof BigInteger) {
            /*
            if (BigInteger.class.cast(ast.getLiteral()).intValueExact() > Integer.MAX_VALUE || BigInteger.class.cast(ast.getLiteral()).intValueExact() < Integer.MIN_VALUE) {
                throw new RuntimeException("Integer value is out of range of Java int (32-bit signed int)");
            }
             */
            try {
                int test = BigInteger.class.cast(ast.getLiteral()).intValueExact();
            }
            catch (ArithmeticException e) {
                throw new RuntimeException("Integer value is out of range of Java int (32-bit signed int)");
            }
            ast.setType(Environment.Type.INTEGER);
        }
        else if (type instanceof BigDecimal) {
            if (BigDecimal.class.cast(ast.getLiteral()).doubleValue() > Double.MAX_VALUE || BigDecimal.class.cast(ast.getLiteral()).doubleValue() < Double.MIN_VALUE) {
                throw new RuntimeException("Decimal value is out of range of Java double value (64-bit signed float)");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        else {
            throw new RuntimeException("Type did not match any defined literal.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        //throw new UnsupportedOperationException();  // TODO
        visit(ast.getExpression());
        Ast.Expr expr = ast.getExpression();
        if (!((expr) instanceof Ast.Expr.Binary)) {
            throw new RuntimeException("The contained expression is not a binary expression");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        //throw new UnsupportedOperationException();  // TODO
        String operator = ast.getOperator();
        Ast.Expr left = ast.getLeft();
        Ast.Expr right = ast.getRight();
        visit(ast.getLeft());
        visit(ast.getRight());

        if (operator.equals("AND") || operator.equals("OR")) {
            requireAssignable(left.getType(), Environment.Type.BOOLEAN);
            requireAssignable(right.getType(), Environment.Type.BOOLEAN);
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (operator.equals("<") || operator.equals("<=") || operator.equals(">") || operator.equals(">=") || operator.equals("==") || operator.equals("!=")) {
            requireAssignable(left.getType(), Environment.Type.BOOLEAN);
            requireAssignable(right.getType(), Environment.Type.BOOLEAN);
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (operator.equals("+")) {
            if (left.getType() == Environment.Type.STRING || right.getType() == Environment.Type.STRING) {
                ast.setType(Environment.Type.STRING);
            }
            else if (left.getType() == Environment.Type.INTEGER && right.getType() == Environment.Type.INTEGER) {
                ast.setType(Environment.Type.INTEGER);
            }
            else if (left.getType() == Environment.Type.DECIMAL && right.getType() == Environment.Type.DECIMAL) {
                ast.setType(Environment.Type.DECIMAL);
            }
            else {
                throw new RuntimeException("Left and Right do not match or type is wrong.");
            }
        }
        else if (operator.equals("-") || operator.equals("*") || operator.equals("/")) {
            if (left.getType() == Environment.Type.INTEGER && right.getType() == Environment.Type.INTEGER) {
                ast.setType(Environment.Type.INTEGER);
            }
            else if (left.getType() == Environment.Type.DECIMAL && right.getType() == Environment.Type.DECIMAL) {
                ast.setType(Environment.Type.DECIMAL);
            }
            else {
                throw new RuntimeException("Incompatible Types, both must be the same.");
            }
        }
        else {
            throw new RuntimeException("Operator did not match on any specific operator defined.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        //throw new UnsupportedOperationException();  // TODO
        Optional<Ast.Expr> receiver = ast.getReceiver();
        String name = ast.getName();

        if (receiver.isPresent()) {
            visit(receiver.get());
            ast.setVariable(receiver.get().getType().getField(name));
        }
        else{
            ast.setVariable(scope.lookupVariable(name));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        //throw new UnsupportedOperationException();  // TODO
        Optional<Ast.Expr> receiver = ast.getReceiver();
        String name = ast.getName();
        List<Ast.Expr> args = ast.getArguments();
        if (receiver.isPresent()) {
            visit(receiver.get());
            Environment.Function var = ast.getReceiver().get().getType().getMethod(name, args.size());
            for (int i = 0; i < args.size(); i++) {
                visit(args.get(i));
                requireAssignable(var.getParameterTypes().get(i+1),args.get(i).getType());
            }
            ast.setFunction(var);
        }
        else {
            List<Environment.Type> types = scope.lookupFunction(name, args.size()).getParameterTypes();
            for (int j = 0; j < args.size(); j++) {
                visit(args.get(j));
                requireAssignable(types.get(j), args.get(j).getType());
            }
            ast.setFunction(scope.lookupFunction(name, args.size()));
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        //throw new UnsupportedOperationException();  // TODO
        if (target != type) {
            if (target != Environment.Type.ANY) {
                if (target != Environment.Type.COMPARABLE) {
                    throw new RuntimeException("Target type does not match the type being used or assigned.");
                }
                else if (type != Environment.Type.INTEGER && type != Environment.Type.CHARACTER && type != Environment.Type.DECIMAL && type != Environment.Type.STRING) {
                    throw new RuntimeException("Target type does not match the type being used or assigned.");
                }
            }
        }
    }
}