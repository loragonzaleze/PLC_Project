package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

import java.util.EnumSet;

public class Parser implements IParser {




    private IToken current;
    private final ILexer lexer;
    private final EnumSet<IToken.Kind> logicalOrPredictSet;
    public Parser(ILexer lexer) throws PLCException {
        this.lexer = lexer;
        this.current = lexer.next();
        this.logicalOrPredictSet = EnumSet.range(IToken.Kind.IDENT, IToken.Kind.LPAREN);
    }

    void consume() throws LexicalException {
        if(current.getKind() == IToken.Kind.EOF)
            return;
        current = lexer.next();
    }

    boolean match(IToken.Kind token) throws PLCException {
        if(token == lexer.peek().getKind()){
            current = lexer.next();
            return true;
        }
        throw new PLCException("Expected another token");
    }

    //Expr::= ConditionalExpr | LogicalOrExpr
    private Expr expr() throws PLCException {
        IToken firstToken = current;
        Expr e = null;
        if(firstToken.getKind() == IToken.Kind.KW_IF) { // PREDICT(Expr ::= ConditionalExpr) = {if}
            e = conditionalExpr();
        }
        else if(logicalOrPredictSet.contains(firstToken.getKind()) || firstToken.getKind() == IToken.Kind.BANG || firstToken.getKind() == IToken.Kind.MINUS //PREDICT(Expr ::= LogicalOrExpr) = {!,-,COLOR_OP,IMAGE_OP)
               || firstToken.getKind() == IToken.Kind.COLOR_OP || firstToken.getKind() == IToken.Kind.IMAGE_OP || firstToken.getKind() == IToken.Kind.IDENT){
            e = logicalOrExpr();
        }
        else{
            throw new PLCException("Threw an error lo");
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
        if(match(IToken.Kind.LPAREN)) { // if '('
            consume();// TODO: Write exception handling
            condition = expr();
            consume();
            trueCase = expr();
            consume();
            falseCase = expr();
            e = new ConditionalExpr(firstToken, condition, trueCase, falseCase);
            consume();
        }
        return e;
    }

    //LogicalOrExpr ::= LogicalAndExpr ( '|' LogicalAndExpr)*
    private Expr logicalOrExpr() throws PLCException {
        IToken firstToken = current;
        Expr left = null;
        Expr right = null;

        left = logicalAndExpr();
        //TODO: implement right
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
            right = comparisonExpr();
            left = new BinaryExpr(firstToken, left, op, right);
        }
        return left;
    }

    private Expr comparisonExpr() throws PLCException {
        IToken firstToken = current;
        Expr left = null;
        Expr right = null;
        left = additiveExpr();
        //TODO: implement right
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
            Expr right = unaryExpr();
            e = new UnaryExpr(firstToken, op, right);
        }
        if(e == null){ // For unaryExprPostfix
            Expr left = primaryExpr();
            consume();
            PixelSelector right = pixelSelector();
            if(right != null)
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
    private Expr primaryExpr() throws LexicalException {
        IToken firstToken = current;
        Expr e = null;
        e = switch(firstToken.getKind()){
            case BOOLEAN_LIT -> new BooleanLitExpr(firstToken);
            case STRING_LIT -> new StringLitExpr(firstToken);
            case INT_LIT -> new IntLitExpr(firstToken);
            case FLOAT_LIT -> new FloatLitExpr(firstToken);
            case IDENT -> new IdentExpr(firstToken);
            default -> null;
        };
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
        consume();
        return new PixelSelector(firstToken, x, y);
    }
    @Override
    public ASTNode parse() throws PLCException {
        return expr();
    }
}
