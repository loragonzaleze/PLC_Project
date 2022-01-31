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
        IN_IDENT,   // DONE? TODO: these areas require more testing for exception states -Luna
        HAVE_ZERO,  // DONE
        HAVE_DOT,   // DONE?
        IN_FLOAT,   // DONE?
        IN_NUM,     // DONE?
        IN_STRING,  // working on it (string_lit)
        IN_BSLASH,  // working on it (backslash)
        IN_COMMENT, // TODO: Start on comments
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
                case IN_BSLASH -> {
                    ArrayList<Integer> positions = inBSlash(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
                }
                case HAVE_DOT -> {
                    ArrayList<Integer> positions = haveDot(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
                }
                case IN_FLOAT -> {
                    ArrayList<Integer> positions = inFloat(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
                    //System.out.println("HAVE_FLOAT"); // Continue until no more numbers
                }
                case IN_NUM -> {
                    ArrayList<Integer> positions = inNum(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
                }
                case IN_STRING -> {
                    ArrayList<Integer> positions = inString(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
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
                case IN_COMMENT -> {
                    ArrayList<Integer> positions = commentState(idx, row, column, currentToken);
                    row = positions.get(0);
                    column = positions.get(1);
                    idx = positions.get(2);
                }
            }

        }
        //Sentinel value representing end of file
        tokens.add(new Token(IToken.Kind.EOF, "sentinel", 0, 0, 0, 0));
    }

    // WITH b, t, n, f, r, ", ', \
    private ArrayList<Integer> inBSlash(int index, int row, int column, StringBuilder currentToken) {
        char currentChar = code.charAt(index);
        switch(currentChar) {
            case 'b' -> {
                //insert here
            }
            case 't' -> {
                //insert here
            }
            case 'n' -> {
                //insert here
            }
            case 'f' -> {
                //insert here
            }
            case 'r' -> {
                //insert here
            }
            case '"' -> {
                //insert here
            }
            case '\'' -> {
                //insert here
            }
            case '\\' -> {
                //insert here
            }
        }
        return new ArrayList<>(Arrays.asList(row, column, index));
    }

    // '"' [  '\' ( 'b' | 't' | 'n' | 'f' | 'r' | '"' | ' ' ' | '\')  |  NOT(  '\'  |  '"'  ) ]* '"'
    // Starts and ends with "
    // Then a \ WITH b, t, n, f, r, ", ', \
    // OR anything EXCEPT \ or ", in any amount
    private ArrayList<Integer> inString(int index, int row, int column, StringBuilder currentToken) {
        char currentChar = code.charAt(index);
        switch(currentChar){
            case '\\' -> {
                currentToken.append(currentChar);
                column++;
                index++;
                currState = State.IN_BSLASH;
            }
            case '"' -> {
                currentToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.STRING_LIT, currentToken.toString(), index - (currentToken.length() - 1), currentToken.length(), row, column - (currentToken.length() - 1)));
                column++;
                index++;
                currState = State.START;
                currentToken.setLength(0);
            }
            default -> {
                // TODO: Make a test and functionality for if the string_lit doesn't end with a quote by the end of the input
                currentToken.append(currentChar);
                column++;
                index++;
            }
        }
        return new ArrayList<>(Arrays.asList(row, column, index));
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
            case '\n', '\r' -> {
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
            case '\n', '\r' -> {
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
            case '\n', '\r' -> {
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
            case '\n', '\r' -> {
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
            case '\n', '\r' -> {
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
            case '\n', '\r' -> {
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
        if(currentChar == '.'){ //Means it is going to be an int
            currToken.append(currentChar);
            column++;
            index++;
            currState = State.HAVE_DOT;
            return new ArrayList<Integer>(Arrays.asList(row, column, index));
        }
        switch(currentChar){ // For any end state
            case '\n', '\r' -> {
                tokens.add(new Token(IToken.Kind.INT_LIT, currToken.toString(), index, 1, row, column - 1));
                row++;
                column = 0;
                index++;
                currState = State.START;
            }
            case '\t' -> {
                tokens.add(new Token(IToken.Kind.INT_LIT, currToken.toString(), index, 1, row, column - 1));
                column += 3;
                index++;
                currState = State.START;
            }
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> { //Causes error since cannot have 0 in front of any number
                tokens.add(new Token(IToken.Kind.ERROR, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                currState = State.HAS_ERROR;
            }
            default -> {
                tokens.add(new Token(IToken.Kind.INT_LIT, currToken.toString(), index, 1, row, column - 1));
                currState = State.START;
            }
        }
        currToken.setLength(0);
        return new ArrayList<Integer>(Arrays.asList(row, column, index));
    }

    // '.' -> '0'..'9'
    private ArrayList<Integer> haveDot(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
            switch (currentChar) {
                case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {
                    currToken.append(currentChar);
                    column++;
                    index++;
                    currState = State.IN_FLOAT;
                }
                default -> { // If it catches anything that isn't a number, should throw an error
                    tokens.add(new Token(IToken.Kind.ERROR, currToken.toString(), 0, 0, 0, 0));
                    currState = State.HAS_ERROR;
                    currToken.setLength(0);
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
            case '\n', '\r'-> {
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
                tokens.add(new Token(IToken.Kind.FLOAT_LIT, currToken.toString(), index - (currToken.length()), currToken.length(), row, column - (currToken.length())));
                currState = State.START;
                currToken.setLength(0);
            }

        }
        return new ArrayList<Integer>(Arrays.asList(row, column, index));
    }

    // '1'..'9' -> '0'..'9' | '.' | 'a'..'Z'
    private ArrayList<Integer> inNum(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
        switch(currentChar){
            case '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' -> {
                currToken.append(currentChar);
                column++;
                index++;
            }
            case '.' -> {
                //if transitioning to a float
                currToken.append(currentChar);
                column++;
                index++;
                currState = State.HAVE_DOT;
            }
            case '\n', '\r' -> {
                if(currToken.length() < 11) {
                    tokens.add(new Token(IToken.Kind.INT_LIT, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                    row++;
                    index++;
                    column = 0;
                    currState = State.START;

                    currToken.setLength(0);
                }
                else {
                    tokens.add(new Token(IToken.Kind.ERROR, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                    index++;
                    currState = State.HAS_ERROR;
                    currToken.setLength(0);
                }
            }
            case '\t' -> {
                if(currToken.length() < 11) {
                    tokens.add(new Token(IToken.Kind.INT_LIT, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                    column += 3;
                    index++;
                    currState = State.START;
                    currToken.setLength(0);
                }
                else {
                    tokens.add(new Token(IToken.Kind.ERROR, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                    index++;
                    currState = State.HAS_ERROR;
                    currToken.setLength(0);
                }
            }
            default -> {
                if(currToken.length() < 11) { // Changed wording of logic to make it more readable
                    tokens.add(new Token(IToken.Kind.INT_LIT, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                    currState = State.START;
                    currToken.setLength(0);
                }
                else{
                    tokens.add(new Token(IToken.Kind.ERROR, currToken.toString(), index - (currToken.length() - 1), currToken.length(), row, column - (currToken.length())));
                    currState = State.HAS_ERROR;
                }
            }
        }
        return new ArrayList<Integer>(Arrays.asList(row, column, index));
    }
    // Different then other comments, will run to the end of a new line
    private ArrayList<Integer> commentState(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
        index++;
        column++;
        if(currentChar == '\n' || currentChar == '\r'){
            column = 0;
            row++;
            currToken.setLength(0); // Comments are ignored, so not tokenized
            currState = State.START;
            return new ArrayList<>(Arrays.asList(row, column, index));
        }
        currToken.append(currentChar);
        return new ArrayList<>(Arrays.asList(row, column, index));
    }

    private ArrayList<Integer> startState(int index, int row, int column, StringBuilder currToken){
        char currentChar = code.charAt(index);
        if(currentChar == '\t' && currState == State.START){
            index++;
            column += 3;
            return new ArrayList<>(Arrays.asList(row, column, index));
        }
        if((currentChar == '\n' || currentChar == '\r' )&& currState == State.START){
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
            case '"' -> {
                currToken.append(currentChar);
                currState = State.IN_STRING;
            }
            // for commments
            case '#' -> {
                currToken.append(currentChar);
                currState = State.IN_COMMENT;
            }
            default -> { //For a char that cannot belong as a start token
                currToken.append(currentChar);
                tokens.add(new Token(IToken.Kind.ERROR, currToken.toString(), index, currToken.length(), row, column - currToken.length()));
                currToken.setLength(0);
                currState = State.HAS_ERROR;
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
