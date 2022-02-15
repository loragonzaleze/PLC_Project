package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

import java.util.EnumSet;

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
            throw new PLCException("Throwing error due to invalid token: " + firstToken.getText() + " token kind: " + firstToken.getKind());
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
            default -> null;
        };
        if(e == null){
            match(IToken.Kind.LPAREN); // Has to be left parentheses, or else error
            consume();
            e = expr();
            match(IToken.Kind.RPAREN);
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
        return expr();
    }
}
