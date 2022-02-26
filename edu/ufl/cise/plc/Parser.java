package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class Parser implements IParser {




    private IToken current;
    private final ILexer lexer;
    private final EnumSet<IToken.Kind> logicalOrPredictSet;
    private final EnumSet<IToken.Kind> comparisonExprOperatorSet;
    public Parser(ILexer lexer) throws PLCException {
        this.lexer = lexer;
        this.current = lexer.next();
        this.logicalOrPredictSet = EnumSet.range(IToken.Kind.IDENT, IToken.Kind.LPAREN);
        this.comparisonExprOperatorSet = EnumSet.range(IToken.Kind.LT, IToken.Kind.GE);
    }

    void consume() throws LexicalException {
        if(current.getKind() == IToken.Kind.EOF)
            return;
        current = lexer.next();
    }

    boolean matchExprPredictSet(IToken firstToken) throws PLCException {
        if(logicalOrPredictSet.contains(firstToken.getKind()) || firstToken.getKind() == IToken.Kind.BANG || firstToken.getKind() == IToken.Kind.MINUS //PREDICT(Expr ::= LogicalOrExpr) = {!,-,COLOR_OP,IMAGE_OP)
                || firstToken.getKind() == IToken.Kind.COLOR_OP || firstToken.getKind() == IToken.Kind.IMAGE_OP || firstToken.getKind() == IToken.Kind.IDENT || firstToken.getKind() == IToken.Kind.KW_IF)
        {
            return true;
        }
        throw new SyntaxException("Expected Boolean, String, Int, Float, Ident, !, -, COLOR_OP, IMAGE_OP, (,  if, Actual token: " + firstToken.getKind());
    }

    boolean matchPredictSet(IToken firstToken) throws PLCException{
        if(logicalOrPredictSet.contains(firstToken.getKind()) || firstToken.getKind() == IToken.Kind.BANG || firstToken.getKind() == IToken.Kind.MINUS //PREDICT(Expr ::= LogicalOrExpr) = {!,-,COLOR_OP,IMAGE_OP)
                || firstToken.getKind() == IToken.Kind.COLOR_OP || firstToken.getKind() == IToken.Kind.IMAGE_OP || firstToken.getKind() == IToken.Kind.IDENT)
        {
            return true;
        }
        throw new SyntaxException("Expected Boolean, String, Int, Float, Ident, !, -, COLOR_OP, IMAGE_OP, (,  token, Actual token: " + firstToken.getKind());
    }
    boolean match(IToken.Kind token) throws PLCException {
        if(token == current.getKind()){
            return true;
        }
        throw new SyntaxException("Expected " + token +" Actual token: " + current.getKind());
    }

    boolean matchPlusOne(IToken.Kind token) throws PLCException {
        if(lexer.peek().getKind() == IToken.Kind.EOF)
            return false;
        return lexer.peek().getKind() == token;
    }

    private Program program() throws PLCException {
        IToken firstToken = current;
        Types.Type returnType = Types.Type.toType(current.getText());

        consume();
        match(IToken.Kind.IDENT);
        String programName = current.getText();
        consume();
        match(IToken.Kind.LPAREN);
        consume();
        List<NameDef> parameters = paramList();
        match(IToken.Kind.RPAREN);
        consume();
        List<ASTNode> decsAndStatements = decsAndStatementsList();

        return new Program(firstToken, returnType, programName, parameters, decsAndStatements);
    }

    private List<NameDef> paramList() throws PLCException {
        List<NameDef> params = new ArrayList<>();
        while(current.getKind() == IToken.Kind.TYPE || current.getKind() == IToken.Kind.KW_CONSOLE){

            IToken firstToken = current;
            String type = current.getText();
            consume();

            Dimension dimension = dimension();

            match(IToken.Kind.IDENT);
            String name = current.getText();
            consume();
            if(dimension == null)
                params.add(new NameDef(firstToken, type, name));
            else
                params.add(new NameDefWithDim(firstToken, type, name, dimension));

            if(current.getKind() != IToken.Kind.COMMA)
                break;

            match(IToken.Kind.COMMA);
            consume();
        }
        if(current.getKind() == IToken.Kind.KW_VOID)
            throw new SyntaxException("Cannot have void type");
        return params;
    }

    private Dimension dimension() throws PLCException {
        IToken firstToken = current;
        if(firstToken.getKind() != IToken.Kind.LSQUARE)
            return null;

        match(IToken.Kind.LSQUARE);
        consume();
        Expr width = expr();
        match(IToken.Kind.COMMA);
        consume();
        Expr height = expr();
        match(IToken.Kind.RSQUARE);
        consume();

        return new Dimension(firstToken, width, height);
    }
    private NameDef nameDef() throws PLCException {
        IToken firstToken = current;
        String type = current.getText();
        consume();

        Dimension dimension = dimension();

        match(IToken.Kind.IDENT);
        String name = current.getText();
        consume();
        if(dimension == null)
            return new NameDef(firstToken, type, name);
        else
            return new NameDefWithDim(firstToken, type, name, dimension);
    }

    private List<ASTNode> decsAndStatementsList() throws PLCException {
        List<ASTNode> decsAndStatements = new ArrayList<>();
        while(current.getKind() != IToken.Kind.EOF){
            if(current.getKind() == IToken.Kind.TYPE || current.getKind() == IToken.Kind.KW_CONSOLE){ // Declaration
                ASTNode declaration = declaration();
                decsAndStatements.add(declaration);
            }
            else if(current.getKind() == IToken.Kind.IDENT || current.getKind() == IToken.Kind.KW_WRITE || current.getKind() == IToken.Kind.RETURN){ // Statement
                ASTNode statement = statement();
                decsAndStatements.add(statement);
            }
            match(IToken.Kind.SEMI);
            consume();
        }
        return decsAndStatements;
    }

    private ASTNode declaration() throws PLCException {
        IToken firstToken = current;
        NameDef nameDef = nameDef();
        if(current.getKind() == IToken.Kind.ASSIGN || current.getKind() == IToken.Kind.LARROW) {
            IToken op = current;
            consume();
            Expr expr = expr();
            return new VarDeclaration(firstToken, nameDef, op, expr);
        }

        return new VarDeclaration(firstToken, nameDef, null, null);
    }

    private ASTNode statement() throws PLCException {
        IToken firstToken = current;

        if(current.getKind() == IToken.Kind.IDENT){
            String name = firstToken.getText();

            match(IToken.Kind.IDENT);
            consume();
            PixelSelector optionalPS = pixelSelector();
            if(current.getKind() == IToken.Kind.ASSIGN){ // IDENT PixelSelector? '=' Expr
                match(IToken.Kind.ASSIGN);
                consume();
                Expr expr = expr();
                return new AssignmentStatement(firstToken, name, optionalPS, expr);

            }
            else if(current.getKind() == IToken.Kind.LARROW){ //IDENT PixelSelector? ‘<-’ Expr
                match(IToken.Kind.LARROW);
                consume();
                Expr expr = expr();
                return new ReadStatement(firstToken, name, optionalPS, expr);
            }
            throw new SyntaxException("Expected '=' or '<-', got: " + current.getKind());
        }
        else if(current.getKind() == IToken.Kind.KW_WRITE){
            match(IToken.Kind.KW_WRITE);
            consume();

            Expr source = expr();
            match(IToken.Kind.RARROW);
            consume();
            Expr dest = expr();

            return new WriteStatement(firstToken, source, dest);
        }

        else if(current.getKind() == IToken.Kind.RETURN){
            match(IToken.Kind.RETURN);
            consume();
            Expr returnExpr = expr();

            return new ReturnStatement(firstToken, returnExpr);
        }
        throw new SyntaxException("Expected IDENT, write, or ^, got: " + current.getKind());
    }

    //Expr::= ConditionalExpr | LogicalOrExpr
    private Expr expr() throws PLCException {
        IToken firstToken = current;
        Expr e = null;
        if(firstToken.getKind() == IToken.Kind.KW_IF) { // PREDICT(Expr ::= ConditionalExpr) = {if}
            e = conditionalExpr();
        }
        else if(logicalOrPredictSet.contains(firstToken.getKind()) || firstToken.getKind() == IToken.Kind.BANG || firstToken.getKind() == IToken.Kind.MINUS //PREDICT(Expr ::= LogicalOrExpr) = {!,-,COLOR_OP,IMAGE_OP)
               || firstToken.getKind() == IToken.Kind.COLOR_OP || firstToken.getKind() == IToken.Kind.IMAGE_OP || firstToken.getKind() == IToken.Kind.IDENT || firstToken.getKind() == IToken.Kind.LANGLE
                || firstToken.getKind() == IToken.Kind.COLOR_CONST || firstToken.getKind() == IToken.Kind.KW_CONSOLE){
            e = logicalOrExpr();
        }
        else{
            throw new SyntaxException("Throwing error due to invalid token: " + firstToken.getText() + " token kind: " + firstToken.getKind());
        }

        return e;
    }

    // ConditionalExpr ::= 'if' '(' Expr ')' Expr 'else' Expr 'fi'
    private Expr conditionalExpr() throws PLCException {
        IToken firstToken = current;
        Expr e = null;
        Expr condition = null; // ( Expr )
        Expr trueCase = null; // Expr
        Expr falseCase = null; // Expr
        consume(); // 'if'
        match(IToken.Kind.LPAREN); // check that if ends with'('
        consume();
        matchExprPredictSet(current);
        condition = expr(); // Expr
        match(IToken.Kind.RPAREN); // ')'
        consume();
        matchExprPredictSet(current);
        trueCase = expr(); // Expr
        match(IToken.Kind.KW_ELSE); // Check that there is an else
        consume();
        matchExprPredictSet(current);
        falseCase = expr();
        match(IToken.Kind.KW_FI);
        e = new ConditionalExpr(firstToken, condition, trueCase, falseCase);
        consume();
        return e;
    }

    //LogicalOrExpr ::= LogicalAndExpr ( '|' LogicalAndExpr)*
    private Expr logicalOrExpr() throws PLCException {
        IToken firstToken = current;
        Expr left = null;
        Expr right = null;

        left = logicalAndExpr();
        while(current.getKind() == IToken.Kind.OR){
            IToken op = current;
            consume();
            matchPredictSet(current);
            right = logicalAndExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    // LogicalAndExpr ::= ComparisonExpr ( '&' ComparisonExpr)
    private Expr logicalAndExpr() throws PLCException {
        IToken firstToken = current;
        Expr left = null;
        Expr right = null;
        left = comparisonExpr();
        while(current.getKind() == IToken.Kind.AND){
            IToken op = current;
            consume();
            matchPredictSet(current);
            right = comparisonExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    //ComparisonExpr ::= AdditiveExpr ( op = ('<' | '>' | '==' | '!=' | '<=' | '>=') AdditiveExpr)*
    private Expr comparisonExpr() throws PLCException {
        IToken firstToken = current;
        Expr left = null;
        Expr right = null;
        left = additiveExpr();
        while(comparisonExprOperatorSet.contains(current.getKind())){
            IToken op = current;
            consume();
            matchPredictSet(current);
            right = additiveExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    private Expr additiveExpr() throws PLCException {
        IToken firstToken = current;
        Expr left = null;
        Expr right = null;
        left = multiplicativeExpr();
        while(current.getKind() == IToken.Kind.MINUS || current.getKind() == IToken.Kind.PLUS){
            IToken op = current;
            consume();
            matchPredictSet(current); //Make sure the current token is in the predict set of UnaryExpr
            right = multiplicativeExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    private Expr multiplicativeExpr() throws PLCException {
        IToken firstToken = current;
        Expr left = null;
        Expr right = null;
        left = unaryExpr();
        while(current.getKind() == IToken.Kind.TIMES || current.getKind() == IToken.Kind.DIV
                || current.getKind() == IToken.Kind.MOD){
            IToken op = current;
            consume();
            matchPredictSet(current);
            right = unaryExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    //UnaryExpr ::= ('!'|'-'| COLOR_OP | IMAGE_OP) UnaryExpr |UnaryExprPostfix
    private Expr unaryExpr() throws PLCException {
        IToken firstToken = current;

        Expr e = null;

        while(current.getKind() == IToken.Kind.BANG || current.getKind() == IToken.Kind.MINUS //PREDICT(Expr ::= LogicalOrExpr) = {!,-,COLOR_OP,IMAGE_OP)
                || current.getKind() == IToken.Kind.COLOR_OP || current.getKind() == IToken.Kind.IMAGE_OP){
            IToken op = firstToken;
            consume();
            matchPredictSet(current);
            Expr right = unaryExpr();
            e = new UnaryExpr(firstToken, op, right);
        }
        if(e == null){ // For unaryExprPostfix
            Expr left = primaryExpr();
            PixelSelector right = null;
            if(current.getKind() == IToken.Kind.SEMI){
                return left;
            }
            consume();
            right = pixelSelector();
            if(right != null) //If it does not contain a pixel selector
                e = new UnaryExprPostfix(firstToken, left, right);
            else
                e = left;
        }
        return e;
    }

    private Expr unaryExprPostfix() throws LexicalException {
        return null;
    }
    /*
    * PrimaryExpr ::= BOOLEAN_LIT | STRING_LIT | INT_LIT | FLOAT_LIT | IDENT | '(' Expr ')'
    * */
    private Expr primaryExpr() throws PLCException {
        IToken firstToken = current;
        Expr e = null;
        e = switch(firstToken.getKind()){
            case BOOLEAN_LIT -> new BooleanLitExpr(firstToken);
            case STRING_LIT -> new StringLitExpr(firstToken);
            case INT_LIT -> new IntLitExpr(firstToken);
            case FLOAT_LIT -> new FloatLitExpr(firstToken);
            case IDENT -> new IdentExpr(firstToken);
            case COLOR_CONST -> new ColorConstExpr(firstToken);
            case KW_CONSOLE -> new ConsoleExpr(firstToken);
            default -> null;
        };
        if(e == null){
            if(current.getKind() == IToken.Kind.LANGLE){
                match(IToken.Kind.LANGLE);
                consume();
                Expr red = expr();
                match(IToken.Kind.COMMA);
                consume();
                Expr green = expr();
                match(IToken.Kind.COMMA);
                consume();
                Expr blue = expr();
                match(IToken.Kind.RANGLE);
                consume();
                e = new ColorExpr(firstToken, red, green, blue);

            }
            else if(current.getKind() == IToken.Kind.LPAREN){
                match(IToken.Kind.LPAREN); // Has to be left parentheses, or else error
                consume();
                e = expr();
                match(IToken.Kind.RPAREN);
            }
            else{
                throw new SyntaxException("Expected Boolean, String, Int, Float, Ident, !, -, COLOR_OP, IMAGE_OP, (,  <<, Actual token: " + firstToken.getKind());
            }
        }

        return e;
    }

    private PixelSelector pixelSelector() throws PLCException {
        if(current.getKind() != IToken.Kind.LSQUARE)
            return null;
        IToken firstToken = current;
        consume();
        Expr x = expr();
        consume();
        Expr y = expr();
        match(IToken.Kind.RSQUARE);
        consume();
        return new PixelSelector(firstToken, x, y);
    }
    @Override
    public ASTNode parse() throws PLCException {
        return program();
    }
}
