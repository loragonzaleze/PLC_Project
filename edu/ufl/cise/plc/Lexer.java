package edu.ufl.cise.plc;

import java.util.ArrayList;
import java.util.Arrays;

public class Lexer implements ILexer{


    private final String code;
    private int tokenPosition;
    public ArrayList<IToken> tokens;
    private State currState;
    private final int codeLength;
    private enum State {
        START,
        IN_IDENT,
        HAVE_ZERO,
        HAVE_DOT,
        IN_FLOAT,
        IN_NUM,
        HAVE_EQ, // '=' -> '==' | '='
        HAVE_MINUS, // '-' -> '->' | '-'
        HAVE_LT, // '<' -> '<<' | '<=' | '<' | '<-'
        HAVE_GT, // '>' -> '>>' | '>=' | '>'
        HAVE_BANG // '!' -> '!=' | '!'
    }

    public Lexer(String code){
        this.code = code;
        this.tokenPosition = 0;
        this.tokens = new ArrayList<>();
        this.currState = State.START;
        this.codeLength = code.length();
        dfa();
    }

    private void dfa(){
        StringBuilder currentToken = new StringBuilder();
        int row = 0;
        int column = 0;
        int idx = 0;
        while(idx < codeLength) {
            char currentChar = code.charAt(idx);
            switch(currState){
                case START -> {
                    ArrayList<Integer> positions = startState(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
                    if(currState == State.START){
                        currentToken.setLength(0); //Resets string to start creating new token
                    }
                }
                case IN_IDENT -> {
                    System.out.println("IN_IDENT");
                }
                case HAVE_ZERO -> {
                    System.out.println("HAVE_ZERO");
                }
                case HAVE_DOT -> {
                    System.out.println("HAVE_DOT");
                }
                case HAVE_EQ -> {
                    ArrayList<Integer> positions = haveEqualState(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
                }
                case HAVE_MINUS -> {
                    ArrayList<Integer> positions = haveMinusState(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
                }
                default -> System.out.println("Value of currentChar: " + (int)currentChar + " Current State: " + currState);
            }

        }
        //Sentinel value representing end of file
        tokens.add(new Token(IToken.Kind.EOF, "sentinel", 0, 0, 0, 0));

    }



    private ArrayList<Integer> inIdentState(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);

        return new ArrayList<>(Arrays.asList(row, column, index));
    }

    //HAVE_EQ, // '=' -> '==' | '='
    private ArrayList<Integer> haveEqualState(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
        switch(currentChar){
            case '=' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.EQUALS, currToken.toString(), index - 1, 2, row, column - 1));
                column++;
                index++;
            }
            case '\n' -> {
                tokens.add(new Token(IToken.Kind.ASSIGN, currToken.toString(), index - 1, 1, row, column - 1));
                row++;
                index++;
                column = 0;

            }
            case '\t' -> {
                tokens.add(new Token(IToken.Kind.ASSIGN, currToken.toString(), index - 1, 1, row, column - 1));
                column += 3;
                index++;
            }
            default -> {
                tokens.add(new Token(IToken.Kind.ASSIGN, currToken.toString(), index - 1, 1, row, column - 1));
            }
        }
        currState = State.START;
        currToken.setLength(0);
        return new ArrayList<Integer>(Arrays.asList(row, column, index));
    }
    // HAVE_MINUS, // '-' -> '->' | '-'
    private ArrayList<Integer> haveMinusState(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
        switch(currentChar){
            case '>' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.RARROW, currToken.toString(), index, 2, row, column - 1));
                column++;
                index++;
            }
            case '\n' -> {
                tokens.add(new Token(IToken.Kind.MINUS, currToken.toString(), index, 1, row, column - 1));
                row++;
                column = 0;
                index++;
            }
            case '\t' -> {
                tokens.add(new Token(IToken.Kind.MINUS, currToken.toString(), index, 1, row, column - 1));
                column += 3;
                index++;
            }
            default -> {
                tokens.add(new Token(IToken.Kind.MINUS, currToken.toString(), index, 1, row, column - 1));
            }
        }

        currToken.setLength(0);
        currState = State.START;

        return new ArrayList<Integer>(Arrays.asList(row, column, index));
    }


    private ArrayList<Integer> startState(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
        if(currentChar == '\t'){
            index++;
            column += 3;
            currState = State.START;
            return new ArrayList<>(Arrays.asList(row, column, index));
        }
        if(currentChar == '\n'){
            index++;
            row++;
            column = 0;
            currState = State.START;
            return new ArrayList<>(Arrays.asList(row, column, index));
        }

        switch(currentChar){
            case '+' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.PLUS, currToken.toString(), index, 1, row, column));
                currState = State.START;
            }
            case ' ' -> {
                currState = State.START;
            }
            case '&' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.AND, currToken.toString(), index, 1, row, column));
                currState = State.START;
            }

            case ',' -> {
                tokens.add(new Token(IToken.Kind.COMMA, currToken.toString(), index, 1, row, column));
                currState = State.START;
            }
            case '/' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.DIV, currToken.toString(), index, 1, row, column));
                currState = State.START;
            }
            case '(', ')' -> {
                currToken.append(currentChar);
                if(currentChar == '(')
                    tokens.add(new Token(IToken.Kind.LPAREN, currToken.toString(), index, 1, row, column));
                else
                    tokens.add(new Token(IToken.Kind.RPAREN, currToken.toString(), index, 1, row, column));
                currState = State.START;
            }
            case '[', ']' -> {
                currToken.append(currentChar);
                if(currentChar == '[')
                    tokens.add(new Token(IToken.Kind.LSQUARE, currToken.toString(), index, 1, row, column));
                else
                    tokens.add(new Token(IToken.Kind.RSQUARE, currToken.toString(), index, 1, row, column));
                currState = State.START;
            }
            case '%' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.MOD, currToken.toString(), index, 1, row, column));
                currState = State.START;
            }
            case '|' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.OR, currToken.toString(), index, 1, row, column));
                currState = State.START;
            }
            case '^' -> {
                tokens.add(new Token(IToken.Kind.RETURN, currToken.toString(), index, 1, row, column));
                currState = State.START;
            }
            case ';' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.SEMI, currToken.toString(), index, 1, row, column));
                currState = State.START;
            }
            case '*' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.TIMES, currToken.toString(), index, 1, row, column));
                currState = State.START;
            }
            case '=' -> {
                currToken.append(currentChar);
                currState = State.HAVE_EQ;
            }
            case '>' -> {
                if(currState == State.HAVE_MINUS){ //If part of a right arrow '->'
                    currToken.append(currentChar);
                }
                else{
                    currState = State.HAVE_GT;
                }
            }
            case '<' -> {
                currState = State.HAVE_LT;
            }
            case '!' -> {
                currState = State.HAVE_BANG;
            }
            case '-' -> {
                currToken.append(currentChar);
                currState = State.HAVE_MINUS;
            }

            // Case NUMS
            case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {
                if(currentChar == '0'){
                    tokens.add(new Token(IToken.Kind.FLOAT_LIT, currToken.toString(), index, 1, row, column));
                    currState = State.HAVE_ZERO;
                }
                tokens.add(new Token(IToken.Kind.INT_LIT, currToken.toString(), index, 1, row, column));
                currState = State.IN_NUM;
            }
        }
        index++;
        column++;
        return new ArrayList<>(Arrays.asList(row, column, index));
    }


    @Override
    public IToken next() throws LexicalException {
       return tokens.get(tokenPosition++);
    }

    @Override
    public IToken peek() throws LexicalException {
        return tokens.get(tokenPosition + 1);
    }
}
