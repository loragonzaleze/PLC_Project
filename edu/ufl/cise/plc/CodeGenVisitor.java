package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.runtime.ConsoleIO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeGenVisitor implements ASTVisitor {
    CodeGenStringBuilder code = new CodeGenStringBuilder();
    Map<Types.Type, String> typeStr = Map.of(
            Types.Type.INT, "int",
            Types.Type.BOOLEAN, "boolean",
            Types.Type.STRING, "String",
            Types.Type.VOID, "void",
            Types.Type.FLOAT, "float"
    );

    Map<Types.Type, String> coerceStr = Map.of(
            Types.Type.INT , "\"INT\"",
            Types.Type.STRING, "\"STRING\"",
            Types.Type.BOOLEAN, "\"BOOLEAN\"",
            Types.Type.FLOAT, "\"FLOAT\""
    );

    Map<Types.Type, String> promptStr = Map.of(
            Types.Type.INT , "\"Enter integer:\"",
            Types.Type.STRING, "\"Enter string:\"",
            Types.Type.BOOLEAN, "\"Enter boolean:\"",
            Types.Type.FLOAT, "\"Enter float:\""
    );

    Map<Types.Type, String> boxedStr = Map.of(
            Types.Type.INT , "(Integer) ",
            Types.Type.STRING, "(String) ",
            Types.Type.BOOLEAN, "(Boolean) ",
            Types.Type.FLOAT, "(Float) "
    );

    Map<String, Types.Type> vars = new HashMap<>();

    private String packageName = "";
    public CodeGenVisitor(String packageName){
        this.packageName = packageName;
    }
    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
        String boolValue = booleanLitExpr.getValue() ? "true" : "false";
        code.append(boolValue);
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
        code.append("\"\"\"\n" + stringLitExpr.getValue() + "\"\"\"");
        return null;
    }

    @Override
    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
        if(intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Types.Type.INT){
            code.lparen().append(typeStr.get(intLitExpr.getCoerceTo())).rparen().space();
        }
        code.append(intLitExpr.getText());
        return null;
    }

    @Override
    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
        if(floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() != Types.Type.FLOAT){
            code.lparen().append(typeStr.get(floatLitExpr.getCoerceTo())).rparen().space();
        }
        code.append(Float.toString(floatLitExpr.getValue()) + "f");
        return null;
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        code.append(boxedStr.get(consoleExpr.getCoerceTo())).consoleExpr().lparen().append(coerceStr.get(consoleExpr.getCoerceTo()))
                .comma().space().append(promptStr.get(consoleExpr.getCoerceTo())).rparen();
        return null;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {
        if(unaryExpression.getCoerceTo() != null){
            code.lparen().append(typeStr.get(unaryExpression.getCoerceTo())).rparen().space();
        }
        code.append(unaryExpression.getOp().getText());
        unaryExpression.getExpr().visit(this, arg);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        if(binaryExpr.getCoerceTo() != null){
            code.lparen().append(typeStr.get(binaryExpr.getCoerceTo())).rparen().space();
        }
        if(binaryExpr.getOp().getKind() == IToken.Kind.NOT_EQUALS && binaryExpr.getLeft().getType() == Types.Type.STRING && binaryExpr.getLeft().getType() == Types.Type.STRING)
            code.append("!");
        code.lparen();
        binaryExpr.getLeft().visit(this, arg);
        if(binaryExpr.getLeft().getType() == Types.Type.STRING && binaryExpr.getLeft().getType() == Types.Type.STRING){
            if(binaryExpr.getOp().getKind() == IToken.Kind.EQUALS){
                code.append(".equals").lparen();
                binaryExpr.getRight().visit(this, arg);
                code.rparen().rparen();
                return null;
            }
            else if(binaryExpr.getOp().getKind() == IToken.Kind.NOT_EQUALS){
                code.append(".equals").lparen();
                binaryExpr.getRight().visit(this, arg);
                code.rparen().rparen();
                return null;
            }
        }
        else{
            code.space().append(binaryExpr.getOp().getText()).space();
        }

        binaryExpr.getRight().visit(this, arg);
        code.rparen();

        return null;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        if(identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != identExpr.getType()){
            code.lparen().append(typeStr.get(identExpr.getCoerceTo())).rparen().space();
        }
        code.append(identExpr.getText());
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        if(conditionalExpr.getCoerceTo() != null){
            code.lparen().append(typeStr.get(conditionalExpr.getCoerceTo())).rparen().space();
        }

        code.lparen();
        code.lparen();
        conditionalExpr.getCondition().visit(this, arg);
        code.rparen().space().question().space();
        conditionalExpr.getTrueCase().visit(this, arg);
        code.space().colon().space();
        conditionalExpr.getFalseCase().visit(this, arg);
        code.rparen();
        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        code.append(assignmentStatement.getName()).space().equal().space();
        assignmentStatement.getExpr().visit(this, arg);
        code.semi().newline();
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        code.writeToConsole().lparen();
        writeStatement.getSource().visit(this, arg);
        code.rparen().semi().newline();
        return null;
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        code.append(readStatement.getName()).space().equal().space();
        readStatement.getSource().visit(this, readStatement);
        code.semi().newline();
        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        code.append("package " + packageName).semi().newline();
        code.append("import edu.ufl.cise.plc.runtime.*").semi().newline();
        code.append("public class " + program.getName() + "{\n");
        code.append("public static " + typeStr.get(program.getReturnType()) + " apply").lparen();
        List<NameDef> params = program.getParams();
        if(params.size() > 0){
            for(int i = 0; i < params.size() - 1; i++){

                code.append(typeStr.get(params.get(i).getType())+ " " + params.get(i).getName()).comma();
            }
            code.append(typeStr.get(params.get(params.size() - 1).getType()) + " " +  params.get(params.size() - 1).getName());
        }
        code.rparen().lcurlybracket().newline();

        List<ASTNode> decsAndStatements = program.getDecsAndStatements();
        for(ASTNode node: decsAndStatements){
            node.visit(this, arg);
        }
        code.rcurlybracket().newline().rcurlybracket();
        return code.delegate.toString();
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
        if(!vars.containsKey(nameDef.getName())){
            vars.put(nameDef.getName(), nameDef.getType());
        }
        code.append(typeStr.get(nameDef.getType()) + " " + nameDef.getName());
        return null;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
        code.append("return ");
        returnStatement.getExpr().visit(this, arg);
        code.semi().newline();
        return null;
    }

    @Override
    public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
        declaration.getNameDef().visit(this, arg);
        if(declaration.getExpr() != null){
            code.equal();
            declaration.getExpr().visit(this, arg);
        }
        code.semi().newline();
        return null;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        throw new UnsupportedOperationException();
    }


}
