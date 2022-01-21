package edu.ufl.cise.plc;

import java.util.ArrayList;
import java.util.Arrays;

public class Lexer implements ILexer{


    private final String code;
    private int tokenPosition;
    public ArrayList<IToken> tokens;
    private State currState;
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
        this.currLine = 0;
        this.currColumn = 0;
        this.tokenPosition = 0;
        this.tokens = new ArrayList<>();
        this.currState = State.START;
        dfa();
    }

    private void dfa(){
        StringBuilder currentToken = new StringBuilder();
        int row = 0;
        int column = 0;
        for(int i = 0; i < code.length(); i++) {
            char currentChar = code.charAt(i);

            switch(currState){
                case START -> {
                    ArrayList<Integer> positions = startState(i, row, column);
                    row = positions.get(0);
                    column = positions.get(1);
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
                default -> System.out.println("looool");

            }

        }
        tokens.add(new Token(IToken.Kind.EOF, "sentinel", 0, 0, 0, 0));
    }

    private ArrayList<Integer> startState(int index, int row, int column){
        char currentChar = code.charAt(index);
        int start = index;

        switch(currentChar){
            //'&', '=', '!', ',', '/', '>', '(', '[', '<', '-', '%', '|',  '^', ')', ']', ';', '*'
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


        }
        return new ArrayList<>(Arrays.asList(row, column, index));
    }


    @Override
    public IToken next() throws LexicalException {
       return tokens.get(tokenPosition++);
    }

    @Override
    public IToken peek() throws LexicalException {
        return null;
    }
}
