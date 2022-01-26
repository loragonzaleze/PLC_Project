package edu.ufl.cise.plc;

import java.util.*;

import static java.util.Map.entry;

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
        HAVE_EQ, // '=' -> '==' | '=' DONE
        HAVE_MINUS, // '-' -> '->' | '-' DONE
        HAVE_LT, // '<' -> '<<' | '<=' | '<' | '<-' DONE
        HAVE_GT, // '>' -> '>>' | '>=' | '>' DONE
        HAVE_BANG // '!' -> '!=' | '!' DONE
    }
    private final Map<String, IToken.Kind> reserved = Map.ofEntries(
            entry("string", IToken.Kind.TYPE),
            entry("int", IToken.Kind.TYPE),
            entry("float", IToken.Kind.TYPE),
            entry("boolean", IToken.Kind.TYPE),
            entry("color", IToken.Kind.TYPE),
            entry("image", IToken.Kind.TYPE),
            entry("void", IToken.Kind.TYPE),
            entry("getWidth", IToken.Kind.IMAGE_OP),
            entry("getHeight", IToken.Kind.IMAGE_OP),
            entry("getRed", IToken.Kind.COLOR_OP),
            entry("getGreen", IToken.Kind.COLOR_OP),
            entry("getBlue", IToken.Kind.COLOR_OP),
            entry("BLACK", IToken.Kind.COLOR_CONST),
            entry("BLUE", IToken.Kind.COLOR_CONST),
            entry("CYAN", IToken.Kind.COLOR_CONST),
            entry("DARK_GRAY", IToken.Kind.COLOR_CONST),
            entry("GRAY", IToken.Kind.COLOR_CONST),
            entry("GREEN", IToken.Kind.COLOR_CONST),
            entry("LIGHT_GRAY", IToken.Kind.COLOR_CONST),
            entry("MAGENTA", IToken.Kind.COLOR_CONST),
            entry("ORANGE", IToken.Kind.COLOR_CONST),
            entry("PINK", IToken.Kind.COLOR_CONST),
            entry("RED", IToken.Kind.COLOR_CONST),
            entry("WHITE", IToken.Kind.COLOR_CONST),
            entry("YELLOW", IToken.Kind.COLOR_CONST),
            entry("true", IToken.Kind.BOOLEAN_LIT),
            entry("false", IToken.Kind.BOOLEAN_LIT),
            entry("if", IToken.Kind.KW_IF),
            entry("else", IToken.Kind.KW_ELSE),
            entry("fi", IToken.Kind.KW_FI),
            entry("write", IToken.Kind.KW_WRITE),
            entry("console", IToken.Kind.KW_CONSOLE)
    );

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
                    ArrayList<Integer> positions = inIdentState(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
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
                case HAVE_BANG -> {
                    ArrayList<Integer> positions = haveBangState(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
                }
                case HAVE_GT -> {
                    ArrayList<Integer> positions = haveGTState(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
                }
                case HAVE_LT -> {
                    ArrayList<Integer> positions = haveLTState(idx, row, column, currentToken);
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

    //TODO: Might add global column, row, variable to facilitate strings
    private ArrayList<Integer> inIdentState(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
        if (Character.isLetter(currentChar) || currentChar == '_' || currentChar == '$' || Character.isDigit(currentChar)){
            currToken.append(currentChar);
            index++;
            column++;
            return new ArrayList<>(Arrays.asList(row, column, index));
        }

        String strToken = currToken.toString();
        //CHECK FOR RESERVED WORDS HERE
        if(reserved.containsKey(strToken)){
            tokens.add(new Token(reserved.get(strToken), strToken, index - strToken.length(), strToken.length(), row, column - strToken.length()));
        }
        else{
            tokens.add(new Token(IToken.Kind.IDENT, currToken.toString(), index - currToken.length(), currToken.length(), row, column - currToken.length()));
        }

        switch(currentChar){
            case '\n' -> {
                row++;
                index++;
                column = 0;

            }
            case '\t' -> {
                column += 3;
                index++;
            }
        }
        currState = State.START;
        currToken.setLength(0);
        return new ArrayList<>(Arrays.asList(row, column, index));
    }

    // '<' -> '<<' | '<=' | '<' | '<-'
    private ArrayList<Integer> haveLTState(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
        switch(currentChar){
            case '<' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.LANGLE, currToken.toString(), index - 1, 2, row, column - 1));
                column++;
                index++;
            }
            case '=' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.LE, currToken.toString(), index - 1, 2, row, column - 1));
                column++;
                index++;
            }
            case '-' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.LARROW, currToken.toString(), index - 1, 2, row, column - 1));
                column++;
                index++;
            }
            case '\n' -> {
                tokens.add(new Token(IToken.Kind.LT, currToken.toString(), index - 1, 1, row, column - 1));
                row++;
                index++;
                column = 0;

            }
            case '\t' -> {
                tokens.add(new Token(IToken.Kind.LT, currToken.toString(), index - 1, 1, row, column - 1));
                column += 3;
                index++;
            }
            default -> {
                tokens.add(new Token(IToken.Kind.LT, currToken.toString(), index - 1, 1, row, column - 1));
            }

        }
        currState = State.START;
        currToken.setLength(0);

        return new ArrayList<>(Arrays.asList(row, column, index));
    }

    // '!' -> '!=' | '!'
    private ArrayList<Integer> haveBangState(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
        switch(currentChar){
            case '=' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.NOT_EQUALS, currToken.toString(), index - 1, 2, row, column - 1));
                column++;
                index++;
            }
            case '\n' -> {
                tokens.add(new Token(IToken.Kind.BANG, currToken.toString(), index - 1, 1, row, column - 1));
                row++;
                index++;
                column = 0;

            }
            case '\t' -> {
                tokens.add(new Token(IToken.Kind.BANG, currToken.toString(), index - 1, 1, row, column - 1));
                column += 3;
                index++;
            }
            default -> {
                tokens.add(new Token(IToken.Kind.BANG, currToken.toString(), index - 1, 1, row, column - 1));
            }
        }
        currState = State.START;
        currToken.setLength(0);

        return new ArrayList<>(Arrays.asList(row, column, index));
    }

    //HAVE_GT, '>' -> '>>' | '>=' | '>'
    private ArrayList<Integer> haveGTState(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
        switch(currentChar) {
            case '>' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.RANGLE, currToken.toString(), index - 1, 2, row, column - 1));
                column++;
                index++;
            }
            case '=' -> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.GE, currToken.toString(), index - 1, 2, row, column - 1));
                column++;
                index++;
            }
            case '\n' -> {
                tokens.add(new Token(IToken.Kind.GT, currToken.toString(), index - 1, 1, row, column - 1));
                row++;
                index++;
                column = 0;

            }
            case '\t' -> {
                tokens.add(new Token(IToken.Kind.GT, currToken.toString(), index - 1, 1, row, column - 1));
                column += 3;
                index++;
            }
            default -> {
                tokens.add(new Token(IToken.Kind.GT, currToken.toString(), index - 1, 1, row, column - 1));
            }
        }
        currState = State.START;
        currToken.setLength(0);
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
        if(currentChar == '\t' && currState == State.START){
            index++;
            column += 3;
            return new ArrayList<>(Arrays.asList(row, column, index));
        }
        if(currentChar == '\n' && currState == State.START){
            index++;
            row++;
            column = 0;
            return new ArrayList<>(Arrays.asList(row, column, index));
        }
        //For idents:
        if ((Character.isLetter(currentChar) || currentChar == '_' || currentChar == '$') && currState == State.START){
            index++;
            column++;
            currToken.append(currentChar);
            currState = State.IN_IDENT;
            return new ArrayList<>(Arrays.asList(row, column, index));
        }

        switch(currentChar){

            /**Everything below this line is to process symbols**/
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
                if(currState == State.START){
                    currState = State.HAVE_EQ;
                }
                currToken.append(currentChar);
            }
            case '>' -> {
                if(currState == State.START){
                    currState = State.HAVE_GT;
                }

                currToken.append(currentChar);
            }
            case '<' -> {
                if(currState == State.START){
                    currState = State.HAVE_LT;
                }
                currToken.append(currentChar);
            }
            case '!' -> {
                currToken.append(currentChar);
                currState = State.HAVE_BANG;
            }
            case '-' -> {
                if(currState == State.START){
                    currState = State.HAVE_MINUS;
                }
                currToken.append(currentChar);
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
