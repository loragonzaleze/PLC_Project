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
        HAVE_BANG, // '!' -> '!=' | '!' DONE
        HAS_ERROR
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
            if(currState == State.HAS_ERROR){
                break;
            }

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
                    ArrayList<Integer> positions = haveZero(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
                    //System.out.println("HAVE_ZERO"); // If has a zero, then prepare for HAVE_DOT
                }
                case HAVE_DOT -> {
                    ArrayList<Integer> positions = null;
                    try {
                        positions = haveDot(idx, row, column, currentToken);
                        row = positions.get(0);
                        column = positions.get(1);
                        idx = positions.get(2);
                        //System.out.println("HAVE_DOT"); // Look for more numbers for IN_FLOAT
                    } catch (LexicalException e) {
                        e.printStackTrace();
                        currState = State.HAS_ERROR;
                    }
                }
                case IN_FLOAT -> {
                    ArrayList<Integer> positions = inFloat(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
                    //System.out.println("HAVE_FLOAT"); // Continue until no more numbers
                }
                case IN_NUM -> {
                    try {
                        ArrayList<Integer> positions = inNum(idx, row, column, currentToken);
                        row = positions.get(0);
                        column = positions.get(1);
                        idx = positions.get(2);
                        //System.out.println(currentToken);
                    } catch (LexicalException e) {
                        e.printStackTrace();
                        currState = State.HAS_ERROR;
                    }


                    //System.out.println("IN_NUM"); // Look for more numbers or more dots to become a float
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

    // '0' -> '.'
    private ArrayList<Integer> haveZero(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
        switch(currentChar){
            case '.' -> {
                currToken.append(currentChar);
                column++;
                index++;
            }
        }
        currState = State.HAVE_DOT;
        currToken.setLength(currToken.length());
        return new ArrayList<Integer>(Arrays.asList(row, column, index));
    }

    // '.' -> '0'..'9'
    private ArrayList<Integer> haveDot(int index, int row, int column, StringBuilder currToken) throws LexicalException {
        char currentChar = code.charAt(index);
            switch (currentChar) {
                case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {
                    currToken.append(currentChar);
                    column++;
                    index++;
                    currState = State.IN_FLOAT;
                }
                default -> {
                    throw new LexicalException("Not valid character after '.'", row, column);
                }
                //TODO: Add error handling for if next character is not '0'..'9' or expected end

        }
        return new ArrayList<Integer>(Arrays.asList(row, column, index));
    }

    // '0'..'9' -> '0'..'9'
    private ArrayList<Integer> inFloat(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
        switch(currentChar){
            case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {
                currToken.append(currentChar);
                column++;
                index++;
            }

            case '\n' -> {
                tokens.add(new Token(IToken.Kind.FLOAT_LIT, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                row++;
                index++;
                column = 0;
                currState = State.START;
                currToken.setLength(0);

            }
            case '\t' -> {
                tokens.add(new Token(IToken.Kind.FLOAT_LIT, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                column += 3;
                index++;
                currState = State.START;
                currToken.setLength(0);
            }
            default -> {
                tokens.add(new Token(IToken.Kind.FLOAT_LIT, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                currState = State.START;
                currToken.setLength(0);
            }

            //TODO: Add error handling for if next character is not '0'..'9' or expected end
        }
        //System.out.println(currentChar);
        //System.out.println(currToken);
        return new ArrayList<Integer>(Arrays.asList(row, column, index));
    }

    // '1'..'9' -> '0'..'9' | '.' | 'a'..'Z'
    private ArrayList<Integer> inNum(int index, int row, int column, StringBuilder currToken) throws LexicalException {
        char currentChar = code.charAt(index);
        switch(currentChar){
            case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {
                currToken.append(currentChar);
                column++;
                index++;
                System.out.println(Long.parseLong(currToken.toString()));
                if(Long.parseLong(currToken.toString()) > Integer.MAX_VALUE || (Long.parseLong(currToken.toString()) < Integer.MIN_VALUE)){                currToken.append(currentChar);
                    //throw new LexicalException("Integer is too large to be a token", row, column);
                }
            }
            case '.' -> {
                //if transitioning to a float
                currToken.append(currentChar);
                column++;
                index++;
                currState = State.HAVE_DOT;
                //currToken.setLength(currToken.length() + 1);
                //System.out.println(currToken);
            }
            case '\n' -> {
                tokens.add(new Token(IToken.Kind.INT_LIT, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                row++;
                index++;
                column = 0;
                currState = State.START;

                currToken.setLength(0);
            }
            case '\t' -> {
                tokens.add(new Token(IToken.Kind.INT_LIT, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                column += 3;
                index++;
                currState = State.START;
                currToken.setLength(0);
            }
            case 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
                    'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
                    's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
                    'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
                    'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'-> {
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.INT_LIT, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                column++;
                index++;
            }
            default -> {
                tokens.add(new Token(IToken.Kind.INT_LIT, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                currState = State.START;
                currToken.setLength(0);
                //throw new LexicalException("Token is invalid int", row, column);
            }
            //TODO: Add error handling for if next character is not '0'..'9' or expected end
        }
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

        //For numbers:
        if((Character.isDigit(currentChar))){
            //Logic for that
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
            default -> { //For a char that cannot belong as a start token
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.ERROR, currToken.toString(), index, currToken.length(), row, column - currToken.length()));
                currToken.setLength(0);
                currState = State.HAS_ERROR;
            }
            // Case NUMS
            case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {
                if(currentChar == '0'){
                    currToken.append(currentChar);  //If there's a zero, prepare for a dot or not
                    currState = State.HAVE_ZERO;
                }
                else {
                    currToken.append(currentChar);      //If there's no zero, prepare for more numbers
                    currState = State.IN_NUM;
                }
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
       if(tokens.get(tokenPosition).getKind() == IToken.Kind.ERROR){
           throw new LexicalException("Cannot have token " + tokens.get(tokenPosition).getText() + " here!");
       }

       return tokens.get(tokenPosition++);
    }

    @Override
    public IToken peek() throws LexicalException {
        return tokens.get(tokenPosition + 1);
    }
}
