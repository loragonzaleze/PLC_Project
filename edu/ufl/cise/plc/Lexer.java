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
        HAVE_EQ,
        HAVE_MINUS
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
                    ArrayList<Integer> positions = startState(idx, row, column);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
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
                    System.out.println("HAVE_EQUAL");
                }
                default -> System.out.println("looool");

            }

        }
        tokens.add(new Token(IToken.Kind.EOF, "sentinel", 0, 0, 0, 0));
    }

    private ArrayList<Integer> startState(int index, int row, int column){
        char currentChar = code.charAt(index);
        int start = index;

        switch(currentChar){
            case '+' -> {
                tokens.add(new Token(IToken.Kind.PLUS, "+", index, 1, row, column));
                currState = State.START;
                index++;
                column++;
            }
            case '-' -> {
                tokens.add(new Token(IToken.Kind.MINUS, "-", index, 1, row, column));
                currState = State.START;
                index++;
                column++;
            }
            case '\n' -> {
                index++;
                row++;
                column = 0;
                currState = State.START;
            }
            case ' ' -> {
                index++;
                column++;
                currState = State.START;
            }
            case '&' -> {
                tokens.add(new Token(IToken.Kind.AND, "&", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
            case '=' -> {
                tokens.add(new Token(IToken.Kind.ASSIGN, "=", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
            case '!' -> {
                tokens.add(new Token(IToken.Kind.BANG, "!", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
            case ',' -> {
                tokens.add(new Token(IToken.Kind.COMMA, ",", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
            case '/' -> {
                tokens.add(new Token(IToken.Kind.DIV, "/", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
            case '>', '<' -> {

                if(currentChar == '>')
                    tokens.add(new Token(IToken.Kind.GT, ">", index, 1, row, column));
                else
                    tokens.add(new Token(IToken.Kind.LT, "<", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
            case '(', ')' -> {
                if(currentChar == '(')
                    tokens.add(new Token(IToken.Kind.LPAREN, "(", index, 1, row, column));
                else
                    tokens.add(new Token(IToken.Kind.RPAREN, ")", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
            case '[', ']' -> {
                if(currentChar == '[')
                    tokens.add(new Token(IToken.Kind.LSQUARE, "[", index, 1, row, column));
                else
                    tokens.add(new Token(IToken.Kind.RSQUARE, "]", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
            case '%' -> {
                tokens.add(new Token(IToken.Kind.MOD, "%", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
            case '|' -> {
                tokens.add(new Token(IToken.Kind.OR, "|", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
            case '^' -> {
                tokens.add(new Token(IToken.Kind.RETURN, "^", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
            case ';' -> {
                tokens.add(new Token(IToken.Kind.SEMI, ";", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
            case '*' -> {
                tokens.add(new Token(IToken.Kind.TIMES, "*", index, 1, row, column));
                index++;
                column++;
                currState = State.START;
            }
        }

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
