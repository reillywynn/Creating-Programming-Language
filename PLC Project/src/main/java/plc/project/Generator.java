package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(indent);
        newline(++indent);

        List<Ast.Field> fields = ast.getFields();
        if (!ast.getFields().isEmpty()) {
            for (int i = 0; i < fields.size(); i++) {
                print(fields.get(i));
                newline(indent);
            }
        }
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");

        List<Ast.Method> methods = ast.getMethods();
        for (int i = 0; i < methods.size(); i++) {
            newline(--indent);
            newline(++indent);
            print(methods.get(i));
        }
        newline(--indent);
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        if (!ast.getValue().isPresent()) {
            print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName(), ";");
        }
        else {
            print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName(), " = ", ast.getValue().get(),";");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        String type = Environment.getType(ast.getReturnTypeName().get()).getJvmName();
        //System.out.println(type);
        //System.out.println(ast.getFunction().getReturnType().getJvmName());
        print(type, " ", ast.getName(), "(");
        List<String> typeNames = ast.getParameterTypeNames();
        List<String> params = ast.getParameters();
        for (int i = 0; i < params.size(); i++) {
            print(convertType(typeNames.get(i)), " ", params.get(i));
            if (i != params.size() - 1) {
                print(", ");
            }
        }
        print(") {");
        List<Ast.Stmt> stmts = ast.getStatements();
        if (!stmts.isEmpty()) {
            newline(++indent);
            for (int i = 0; i < stmts.size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(stmts.get(i));
            }
            newline(--indent);
        }

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());

        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (", ast.getCondition(), ")", " {");
        newline(++indent);
        List<Ast.Stmt> stmts = ast.getThenStatements();
        if (!ast.getThenStatements().isEmpty()) {
            for (int i = 0; i < stmts.size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(stmts.get(i));
            }
            newline(--indent);
        }
        print("}");
        stmts = ast.getElseStatements();
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            newline(++indent);
            for (int i = 0; i < stmts.size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(stmts.get(i));
            }
            newline(--indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        //throw new UnsupportedOperationException(); //TODO
        String var = ast.getName();
        Ast.Expr val = ast.getValue();
        print("for (", "int ", var, " : ", val, ") {");
        List<Ast.Stmt> stmts = ast.getStatements();
        if (!stmts.isEmpty()) {
            newline(++indent);
            for (int i = 0; i < stmts.size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(stmts.get(i));
            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("while (", ast.getCondition(), ") {");
        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        //throw new UnsupportedOperationException(); //TODO
        if (ast.getLiteral() instanceof Character) {
            print("'", ast.getLiteral(), "'");
        }
        else if (ast.getLiteral() instanceof String) {
            print("\"", ast.getLiteral(), "\"");
        }
        else if (ast.getLiteral() instanceof BigDecimal) {
            print(BigDecimal.class.cast(ast.getLiteral()).doubleValue());
        }
        else if (ast.getLiteral() instanceof Integer) {
            print(BigInteger.class.cast(ast.getLiteral()).intValue());
        }
        else {
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        //throw new UnsupportedOperationException(); //TODO
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        //throw new UnsupportedOperationException(); //TODO
        print(ast.getLeft());
        String op = ast.getOperator();
        if (op.equals("AND")) {
            print(" && ");
        }
        else if (op.equals("OR")) {
            print(" || ");
        }
        else {
            print(" ", op, " ");
        }
        print(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        //throw new UnsupportedOperationException(); //TODO
        if (ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get(), ".");
        }
        String name = ast.getVariable().getJvmName();
        print(name);
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        //throw new UnsupportedOperationException(); //TODO
        if (ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get(), ".");
        }
        print(ast.getFunction().getJvmName(), "(");
        List<Ast.Expr> stmts = ast.getArguments();
        for (int i = 0; i < stmts.size(); i++) {
            print(stmts.get(i));
            if (i != stmts.size() - 1) { // for every case up until the last entry
                print(", ");
            }
        }
        print(")");
        return null;
    }

    public String convertType(String t) {
        switch (t) {
            case "Any":
                return "Object";
            case "Nil":
                return "Void";
            case "IntegerIterable":
                return "Iterable<Integer>";
            case "Comparable":
                return "Comparable";
            case "Boolean":
                return "boolean";
            case "Integer":
                return "int";
            case "Decimal":
                return "double";
            case "Character":
                return "char";
            case "String":
                return "String";
            default:
                throw new RuntimeException("Illegal argument type!");
        }
    }

}
