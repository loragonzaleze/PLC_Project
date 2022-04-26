package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.runtime.ColorTuple;
import edu.ufl.cise.plc.runtime.ColorTupleFloat;
import edu.ufl.cise.plc.runtime.ConsoleIO;
import edu.ufl.cise.plc.runtime.ImageOps;

import java.awt.image.BufferedImage;
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
            Types.Type.FLOAT, "float",
            Types.Type.COLOR, "ColorTuple",
            Types.Type.COLORFLOAT, "ColorTupleFloat",
            Types.Type.IMAGE, "BufferedImage"
    );

    Map<Types.Type, String> coerceStr = Map.of(
            Types.Type.INT , "\"INT\"",
            Types.Type.STRING, "\"STRING\"",
            Types.Type.BOOLEAN, "\"BOOLEAN\"",
            Types.Type.FLOAT, "\"FLOAT\"",
            Types.Type.COLOR, "\"COLOR\""
    );

    Map<Types.Type, String> promptStr = Map.of(
            Types.Type.INT , "\"Enter integer:\"",
            Types.Type.STRING, "\"Enter string:\"",
            Types.Type.BOOLEAN, "\"Enter boolean:\"",
            Types.Type.FLOAT, "\"Enter float:\"",
            Types.Type.COLOR, "\"Enter red, green, and blue components separated with space:\""
    );

    Map<Types.Type, String> boxedStr = Map.of(
            Types.Type.INT , "(Integer) ",
            Types.Type.STRING, "(String) ",
            Types.Type.BOOLEAN, "(Boolean) ",
            Types.Type.FLOAT, "(Float) ",
            Types.Type.COLOR, "(ColorTuple)"
    );

    Map<String, String> opStr = Map.of(
            "+", "PLUS",
            "-", "MINUS",
            "*", "TIMES",
            "/", "DIV",
            "%", "MOD",
            "getRed", "Red",
            "getGreen", "Green",
            "getBlue", "Blue"
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
        if(intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() == Types.Type.COLOR){
            code.newKW().space().append("ColorTuple").lparen().append(intLitExpr.getText()).rparen();
            return null;
        }

        if(intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Types.Type.INT){
            code.lparen().append(typeStr.get(intLitExpr.getCoerceTo())).rparen().space();
        }

        code.append(intLitExpr.getText());
        return null;
    }

    @Override
    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {

        if(floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() == Types.Type.COLORFLOAT){
            code.newKW().space().append("ColorTupleFloat").lparen().append(floatLitExpr.getText() + "f").rparen();
            return null;
        }
        if(floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() != Types.Type.FLOAT){
            code.lparen().append(typeStr.get(floatLitExpr.getCoerceTo())).rparen().space();
        }
        code.append(Float.toString(floatLitExpr.getValue()) + "f");
        return null;
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
        code.append("ColorTuple.unpack").lparen().append("Color.");
        code.append(colorConstExpr.getFirstToken().getText()).append(".getRGB()").rparen();

        return null;
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {

        code.append(boxedStr.get(consoleExpr.getCoerceTo())).consoleExpr().lparen().append(coerceStr.get(consoleExpr.getCoerceTo()))
                .comma().space().append(promptStr.get(consoleExpr.getCoerceTo())).rparen();


        return null;
    }


    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        Types.Type colorExprType = colorExpr.getType();

        code.space().newKW().space();

        if(colorExprType == Types.Type.COLOR){
            code.colorTuple();
        }
        else if(colorExprType == Types.Type.COLORFLOAT){
            code.colorTupleFloat();
        }
        code.lparen();
        colorExpr.getRed().visit(this, arg);
        code.comma().space();
        colorExpr.getGreen().visit(this, arg);
        code.comma().space();
        colorExpr.getBlue().visit(this, arg);
        code.rparen();
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {


        if(unaryExpression.getOp().getKind() == IToken.Kind.COLOR_OP){
            if(unaryExpression.getExpr().getType() == Types.Type.IMAGE){
                code.append("ImageOps.extract").append(opStr.get(unaryExpression.getOp().getText()))
                        .lparen();
                unaryExpression.getExpr().visit(this, arg);
                code.rparen();
                return null;
            }

            if(unaryExpression.getExpr().getType() == Types.Type.INT || unaryExpression.getExpr().getType() == Types.Type.COLOR){
                code.append("ColorTuple.").append(unaryExpression.getOp().getText())
                        .lparen();
                unaryExpression.getExpr().visit(this, arg);
                code.rparen();
                return null;
            }

        }

        if(unaryExpression.getOp().getKind() == IToken.Kind.IMAGE_OP){
            code.lparen();
            unaryExpression.getExpr().visit(this, arg);
            code.rparen();
            String operation = unaryExpression.getOp().getText();

            if(operation.equals("getWidth")){
                code.append(".getWidth").lparen().rparen();
            }
            else{
                code.append(".getHeight").lparen().rparen();
            }
            return null;
        }

        if(unaryExpression.getCoerceTo() != null){
            code.lparen().append(typeStr.get(unaryExpression.getCoerceTo())).rparen().space();
        }
        code.append(unaryExpression.getOp().getText());
        unaryExpression.getExpr().visit(this, arg);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {

        //For colors
        if(binaryExpr.getType() == Types.Type.COLOR || binaryExpr.getType() == Types.Type.COLORFLOAT){
            code.lparen().append("ImageOps.binaryTupleOp").lparen()
                    .append("ImageOps.OP.")
                    .append(opStr.get(binaryExpr.getOp().getText())).comma().space();
            binaryExpr.getLeft().visit(this, arg);
            code.comma().space();
            binaryExpr.getRight().visit(this, arg);
            code.rparen().rparen();
            return null;

        }


        if(binaryExpr.getType() == Types.Type.IMAGE){
            //For Image and int
            if(binaryExpr.getLeft().getType() == Types.Type.IMAGE && binaryExpr.getRight().getType() == Types.Type.INT){
                code.append("ImageOps.binaryImageScalarOp").lparen()
                        .append("ImageOps.OP.")
                        .append(opStr.get(binaryExpr.getOp().getText()))
                        .comma().space();
                binaryExpr.getLeft().visit(this, arg);
                code.comma().space();
                binaryExpr.getRight().visit(this, arg);
                code.rparen();
            }
            //For Image and Image
            if(binaryExpr.getLeft().getType() == Types.Type.IMAGE && binaryExpr.getRight().getType() == Types.Type.IMAGE){

                code.append("ImageOps.binaryImageImageOp").lparen()
                            .append("ImageOps.OP.")
                            .append(opStr.get(binaryExpr.getOp().getText()))
                            .comma().space();
                binaryExpr.getLeft().visit(this, arg);
                code.comma().space();
                binaryExpr.getRight().visit(this, arg);
                code.rparen();
            }
            return null;
        }

        //For comparing colors:
        if(binaryExpr.getLeft().getType() == Types.Type.COLOR && binaryExpr.getRight().getType() == Types.Type.COLOR
        && binaryExpr.getType() == Types.Type.BOOLEAN){
            if(binaryExpr.getOp().getKind() == IToken.Kind.EQUALS){
                code.lparen();
                code.lparen();
                binaryExpr.getLeft().visit(this, arg);
                code.rparen();
                code.append(".equals").lparen();
                binaryExpr.getRight().visit(this, arg);
                code.rparen().rparen();
                return null;
            }
            if(binaryExpr.getOp().getKind() == IToken.Kind.NOT_EQUALS){
                code.append("!");
                code.lparen();
                code.lparen();
                binaryExpr.getLeft().visit(this, arg);
                code.rparen();
                code.append(".equals").lparen();
                binaryExpr.getRight().visit(this, arg);
                code.rparen().rparen();
                return null;
            }
        }

        if(binaryExpr.getLeft().getType() == Types.Type.IMAGE && binaryExpr.getRight().getType() == Types.Type.IMAGE
                && binaryExpr.getType() == Types.Type.BOOLEAN){
            if(binaryExpr.getOp().getKind() == IToken.Kind.EQUALS){
                code.lparen();
                code.append("ImageOps.equals").lparen();
                binaryExpr.getLeft().visit(this, arg);
                code.comma().space();
                binaryExpr.getRight().visit(this, arg);
                code.rparen().rparen();
                return null;
            }
            else if(binaryExpr.getOp().getKind() == IToken.Kind.NOT_EQUALS){
                code.append("!").lparen();
                code.append("ImageOps.equals").lparen();
                binaryExpr.getLeft().visit(this, arg);
                code.comma().space();
                binaryExpr.getRight().visit(this, arg);
                code.rparen().rparen();
                return null;
            }
            return null;
        }


        if(binaryExpr.getCoerceTo() != null){
            if(binaryExpr.getCoerceTo() == Types.Type.COLOR){
                code.newKW().space().append("ColorTuple").lparen();
                binaryExpr.getLeft().visit(this, arg);
                code.space().append(binaryExpr.getOp().getText()).space();
                binaryExpr.getRight().visit(this, arg);
                code.rparen();
                return null;
            }
            else{
                code.lparen().append(typeStr.get(binaryExpr.getCoerceTo())).rparen().space();
            }
        }
        if(binaryExpr.getOp().getKind() == IToken.Kind.NOT_EQUALS && (binaryExpr.getLeft().getType() == Types.Type.STRING && binaryExpr.getRight().getType() == Types.Type.STRING))
            code.append("!");
        code.lparen();
        binaryExpr.getLeft().visit(this, arg);
        if(binaryExpr.getLeft().getType() == Types.Type.STRING && binaryExpr.getRight().getType() == Types.Type.STRING){
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
        if(identExpr.getCoerceTo() != null && (identExpr.getCoerceTo() == Types.Type.COLOR || identExpr.getCoerceTo() == Types.Type.COLORFLOAT)){
            if(identExpr.getType() == Types.Type.COLOR && identExpr.getCoerceTo() == Types.Type.COLORFLOAT){
                code.append("new ColorTupleFloat").lparen().append(identExpr.getText()).rparen();
                return null;
            }
            if(identExpr.getType() == Types.Type.COLORFLOAT && identExpr.getCoerceTo() == Types.Type.COLORFLOAT){
                code.append("new ColorTuple").lparen().append(identExpr.getText()).rparen();
                return null;
            }
            code.append(identExpr.getText());
            return null;
        }
        if(identExpr.getType() == Types.Type.COLOR && identExpr.getCoerceTo() == Types.Type.INT){
            code.append(identExpr.getText());
            return null;
        }
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
        dimension.getWidth().visit(this, arg);
        code.comma().space();
        dimension.getHeight().visit(this, arg);
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        if(arg == null){
            return null;
        }
        if(arg instanceof AssignmentStatement assignment){ //left hand side
            if(assignment.getSelector() != null){ //For left hand side
                code.append("for").lparen().append("int ");
                pixelSelector.getX().visit(this, arg);
                code.space().equal().space().append("0").semi().space();
                pixelSelector.getX().visit(this, arg);

                code.space().append("<").space().append(assignment.getName()).append(".getWidth()").semi().space();
                pixelSelector.getX().visit(this, arg);
                code.append("++").rparen().newline();

                code.append("\tfor").lparen().append("int ");
                pixelSelector.getY().visit(this, arg);
                code.space().equal().space().append("0").semi().space();
                pixelSelector.getY().visit(this, arg);
                code.space().append("<").space().append(assignment.getName()).append(".getHeight()").semi().space();
                pixelSelector.getY().visit(this, arg);
                code.append("++").rparen().newline();
                code.append("\tImageOps.setColor").lparen().append(assignment.getName()).comma().space();
                pixelSelector.getX().visit(this, arg);
                code.comma().space();
                pixelSelector.getY().visit(this, arg);
                code.comma().space();
                assignment.getExpr().visit(this, arg);
                code.rparen().semi().newline();

                return null;
            }
            return null;
        }
        if(arg instanceof UnaryExprPostfix){ //if on the right side
            pixelSelector.getX().visit(this, arg);
            code.comma().space();
            pixelSelector.getY().visit(this, arg);
            return null;
        }


        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {


        if(assignmentStatement.getSelector() == null){
            if(assignmentStatement.getTargetDec().getType() == Types.Type.IMAGE){
                if(assignmentStatement.getExpr().getCoerceTo() == Types.Type.COLOR || assignmentStatement.getExpr().getType() == Types.Type.COLOR){ // Can be assumed type is always an INT
                    code.append("for").lparen().append("int xIdx").space().equal().space().append("0").semi().space()
                            .append("xIdx <").space().append(assignmentStatement.getName()).append(".getWidth()").semi().space()
                            .append("xIdx++").rparen().newline();

                    //inner for loop
                    code.append("\tfor").lparen().append("int yIdx").space().equal().space().append("0").semi().space()
                            .append("yIdx <").space().append(assignmentStatement.getName()).append(".getHeight()").semi().space()
                            .append("yIdx++").rparen().newline();

                    code.append("\tImageOps.setColor").lparen().append(assignmentStatement.getName()).comma().space()
                            .append("xIdx, yIdx, ");

                    assignmentStatement.getExpr().visit(this, arg);
                    code.rparen().semi().newline();
                    return null;

                }
                else if(assignmentStatement.getExpr().getCoerceTo() == Types.Type.INT){
                    code.semi().newline();
                    return null;
                }
                else if(assignmentStatement.getExpr().getType() == Types.Type.IMAGE){ //For image = image
                    code.append(assignmentStatement.getName()).space().equal().space();
                    if(assignmentStatement.getTargetDec().getDim() != null){
                        code.append("ImageOps.resize").lparen();
                        assignmentStatement.getExpr().visit(this, arg);
                        code.comma().space()
                                .append(assignmentStatement.getName()).append(".getWidth").lparen().rparen()
                                .comma().space()
                                .append(assignmentStatement.getName()).append(".getHeight").lparen().rparen()
                                .rparen().semi().newline();
                        return null;
                    }
                    else if(assignmentStatement.getTargetDec().getDim() == null){
                        code.append("ImageOps.clone").lparen();
                        assignmentStatement.getExpr().visit(this, arg);
                        code.rparen().semi().newline();
                        return null;
                    }

                }
                code.semi().newline();
                return null;
            }

            else if(assignmentStatement.getTargetDec().getType() == Types.Type.COLOR && assignmentStatement.getExpr().getType() == Types.Type.INT
                    && assignmentStatement.getExpr().getCoerceTo()  == Types.Type.COLOR){
                code.append(assignmentStatement.getName()).space().equal().space()
                    .newKW().space().append("ColorTuple").lparen();
                assignmentStatement.getExpr().visit(this, arg);
                code.rparen();
                return null;
            }

            else if(assignmentStatement.getTargetDec().getType() == Types.Type.INT && assignmentStatement.getExpr().getType() == Types.Type.COLOR
                    && assignmentStatement.getExpr().getCoerceTo() == Types.Type.INT  && assignmentStatement.getExpr().getFirstToken().getKind() == IToken.Kind.COLOR_CONST){
                code.append(assignmentStatement.getName()).space().equal().space();
                code.append("Color.").append(assignmentStatement.getExpr().getText()).append(".getRGB").lparen().rparen()
                        .semi().newline();
                return null;
            }
            else{
                code.append(assignmentStatement.getName()).space().equal().space();
                assignmentStatement.getExpr().visit(this, arg);
                code.semi().newline();
                return null;
            }
        }
        else{
            assignmentStatement.getSelector().visit(this, assignmentStatement);
        }
        return null;

    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        if(writeStatement.getSource().getType() == Types.Type.IMAGE && writeStatement.getDest().getType() == Types.Type.CONSOLE){
            code.append("ConsoleIO.displayImageOnScreen").lparen();
            writeStatement.getSource().visit(this, arg);
            code.rparen().semi().newline();
            return null;
        }
        if(writeStatement.getSource().getType() == Types.Type.IMAGE && writeStatement.getDest().getType() == Types.Type.STRING){
            code.append("FileURLIO.writeImage").lparen();
            writeStatement.getSource().visit(this, arg);
            code.comma().space();
            writeStatement.getDest().visit(this, arg);
            code.rparen().semi().newline();
            return null;
        }
        if(writeStatement.getDest().getType() == Types.Type.STRING){
            code.append("FileURLIO.writeValue").lparen();
            writeStatement.getSource().visit(this, arg);
            code.comma().space();
            writeStatement.getDest().visit(this, arg);

            code.rparen().semi().newline();
            return null;
        }
        code.writeToConsole().lparen();
        writeStatement.getSource().visit(this, arg);
        code.rparen().semi().newline();
        return null;
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        code.append(readStatement.getName()).space().equal().space();

        if(readStatement.getSource().getType() == Types.Type.STRING){
            if(readStatement.getTargetDec().getType() == Types.Type.IMAGE){
                if(readStatement.getTargetDec().getDim() != null){
                    code.append("FileURLIO.readImage").lparen();
                    readStatement.getSource().visit(this, arg);
                    code.comma().space();
                    readStatement.getTargetDec().getDim().visit(this, arg);
                    code.rparen();
                }
                else{
                    code.append("FileURLIO.readImage").lparen();
                    readStatement.getSource().visit(this, arg);
                    code.rparen();
                }
                code.semi().newline();
                code.append("FileURLIO.closeFiles()");
            }
            else{
                code.lparen();
                code.append(boxedStr.get(readStatement.getTargetDec().getType()));
                code.append("FileURLIO.readValueFromFile").lparen();
                readStatement.getSource().visit(this, arg);
                code.rparen().rparen();
            }

        }
        else{
            readStatement.getSource().visit(this, readStatement);
        }
        code.semi().newline();
        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        code.append("package " + packageName).semi().newline();
        code.append("import edu.ufl.cise.plc.runtime.*").semi().newline();
        code.append("import java.awt.image.BufferedImage").semi().newline();
        code.append("import java.awt.Color").semi().newline();
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
        code.append(typeStr.get(nameDef.getType()) + " " + nameDef.getName()).space();
        return null;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        code.append(typeStr.get(nameDefWithDim.getType())).space().append(nameDefWithDim.getName()).space();
        return null;
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
            code.equal().space();
            if(declaration.getType() == Types.Type.IMAGE){
                //For read statements
                if(declaration.isInitialized() && declaration.getOp().getKind() == IToken.Kind.LARROW){
                    if(declaration.getDim() != null){
                        code.append("FileURLIO.readImage").lparen();
                        declaration.getExpr().visit(this, arg);
                        code.comma().space();
                        declaration.getDim().visit(this, arg);
                        code.rparen().semi().newline();
                        code.append("FileURLIO.closeFiles()").semi().newline();
                        return null;
                    }
                    else if(declaration.getDim() == null){
                        code.append("FileURLIO.readImage").lparen();
                        declaration.getExpr().visit(this, arg);
                        code.comma().space()
                                .append("null").comma().space()
                                .append("null").rparen().semi().newline();
                        code.append("FileURLIO.closeFiles()").semi().newline();
                        return null;
                    }
                }
                //For assign statements
                else if(declaration.isInitialized() && declaration.getOp().getKind() == IToken.Kind.ASSIGN){
                    if(declaration.getDim() != null){
                        if(declaration.getExpr().getType() == Types.Type.IMAGE){
                            code.newKW().space().append("BufferedImage").lparen();
                            declaration.getDim().visit(this, arg);
                            code.comma().space().append("BufferedImage.TYPE_INT_RGB").rparen().semi().newline();
                            code.append(declaration.getName()).space().equal().space();
                            code.append("ImageOps.resize").lparen();

                            declaration.getExpr().visit(this, arg);
                            code.comma().space()
                                    .append(declaration.getName()).append(".getWidth").lparen().rparen()
                                    .comma().space()
                                    .append(declaration.getName()).append(".getHeight").lparen().rparen()
                                    .rparen().semi().newline();
                            return null;
                        }

                        code.newKW().space().append("BufferedImage").lparen();
                        declaration.getDim().visit(this, arg);
                        code.comma().space().append("BufferedImage.TYPE_INT_RGB").rparen().semi().newline();

                        code.append("for").lparen().append("int xIdx").space().equal().space().append("0").semi().space()
                                .append("xIdx <").space().append(declaration.getNameDef().getName()).append(".getWidth()").semi().space()
                                .append("xIdx++").rparen().newline();

                        //inner for loop
                        code.append("\tfor").lparen().append("int yIdx").space().equal().space().append("0").semi().space()
                                .append("yIdx <").space().append(declaration.getNameDef().getName()).append(".getHeight()").semi().space()
                                .append("yIdx++").rparen().newline();

                        code.append("\tImageOps.setColor").lparen().append(declaration.getNameDef().getName()).comma().space()
                                .append("xIdx, yIdx, ");

                        if(declaration.getExpr().getFirstToken().getKind() == IToken.Kind.COLOR_CONST || declaration.getExpr().getCoerceTo() == Types.Type.COLOR){
                            declaration.getExpr().visit(this, arg);
                            code.rparen().semi().newline();
                            return null;
                        }
                        else{
                            code.append("new ColorTuple").lparen();
                            declaration.getExpr().visit(this,arg);
                        }

                        code.rparen().rparen().semi().newline();

                    }
                    else{

                        if(declaration.getExpr().getType() == Types.Type.IMAGE){
                            code.append("ImageOps.clone").lparen();
                            declaration.getExpr().visit(this, arg);
                            code.rparen().semi().newline();
                            return null;
                        }
                        declaration.getExpr().visit(this, arg);
                        code.semi().newline();
                        return null;
                    }
                }
            }
            else if(declaration.getOp().getKind() == IToken.Kind.LARROW){
                if(declaration.getExpr().getType() == Types.Type.STRING){
                    code.lparen().
                            append(boxedStr.get(declaration.getType()));
                    code.append("FileURLIO.readValueFromFile").lparen();
                    declaration.getExpr().visit(this, arg);
                    code.rparen().rparen();

                    code.semi().newline();
                    return null;
                }
                else{
                    declaration.getExpr().visit(this, arg);
                    code.semi().newline();
                    return null;
                }
            }
            else if(declaration.getType() == Types.Type.COLOR && declaration.getExpr().getType() == Types.Type.INT
            && declaration.getExpr().getCoerceTo() == Types.Type.COLOR){
                code.newKW().space().append("ColorTuple").lparen();
                declaration.getExpr().visit(this, arg);
                code.rparen();
            }
            else if(declaration.getType() == Types.Type.INT && declaration.getExpr().getType() == Types.Type.COLOR
                    && declaration.getExpr().getCoerceTo() == Types.Type.INT && declaration.getExpr().getFirstToken().getKind() == IToken.Kind.COLOR_CONST ){
                code.append("Color.").append(declaration.getExpr().getText()).append(".getRGB").lparen().rparen()
                        .semi().newline();

                return null;
            }

            else if(declaration.getType() == Types.Type.INT && declaration.getExpr().getType() == Types.Type.COLOR
                    && declaration.getExpr().getCoerceTo() == Types.Type.INT){
                code.lparen();
                declaration.getExpr().visit(this, arg);
                code.append(".pack").lparen().rparen();
                code.rparen();
                code.semi().newline();
                return null;
            }

            else{
                declaration.getExpr().visit(this, arg);
            }
        }
        else{
            if(declaration.getType() == Types.Type.IMAGE) {
                code.equal().space();
                if (declaration.getDim() != null) {
                    code.newKW().space().append("BufferedImage").lparen();
                    declaration.getDim().visit(this, arg);
                    code.comma().space().append("BufferedImage.TYPE_INT_RGB").rparen().semi().newline();
                    return null;
                }
            }
        }
        code.semi().newline();
        return null;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        code.append("ColorTuple.unpack").lparen()
            .append(unaryExprPostfix.getText()).append(".getRGB").lparen();
        unaryExprPostfix.getSelector().visit(this, unaryExprPostfix);
        code.rparen().rparen();
        return null;
    }
}
