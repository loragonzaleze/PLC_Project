package edu.ufl.cise.plc;

public class Token implements IToken {
    final Kind kind;
    final String input;
    final int pos;
    final int length;
    final SourceLocation location;
    public Token(Kind kind, String input, int pos, int length, int line, int column){

        this.kind = kind;
        this.input = input;
        this.pos = pos; //Position within string where token starts
        this.length = length;
        this.location = new SourceLocation(line, column);

    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getText() {
        return input;
    }

    @Override
    public IToken.SourceLocation getSourceLocation() {
        return location;
    }

    @Override
    public int getIntValue() {
        if (kind != Kind.INT_LIT){
            throw new UnsupportedOperationException();
        }
        return Integer.parseInt(input);
    }

    @Override
    public float getFloatValue() {
        if(kind != Kind.FLOAT_LIT){
            throw new UnsupportedOperationException();
        }
        return Float.parseFloat(input);
    }

    @Override
    public boolean getBooleanValue() {
        if(kind != Kind.BOOLEAN_LIT){
            throw new UnsupportedOperationException();
        }
        return input.equals("true"); }

    @Override
    public String getStringValue() {
        if(kind != Kind.STRING_LIT){
            throw new UnsupportedOperationException();
        }
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < this.input.length(); i++) {
            if (i != 0 && i != this.input.length()-1) {
                if (this.input.charAt(i - 1) == '\\') {
                    switch (this.input.charAt(i)) {
                        case 'b' -> {
                            output.append((char) 8);
                        }
                        case 't' -> {
                            output.append((char) 9);
                        }
                        case 'n' -> {
                            output.append((char) 10);
                        }
                        case 'f' -> {
                            output.append((char) 12);
                        }
                        case 'r' -> {
                            output.append((char) 13);
                        }
                        case '\'' -> {
                            output.append('\'');
                        }
                        case '\"' -> {
                            output.append('\"');
                        }
                        case '\\' -> {
                            output.append((char)92);
                        }
                        default -> {
                            output.append(this.input.charAt(i));
                        }
                    }
                }
                else if(this.input.charAt(i) != '\\')
                    output.append(this.input.charAt(i));
            }
            else if (this.input.charAt(i) != '\\' && this.input.charAt(i) != '\"') {
                output.append(this.input.charAt(i));
            }
        }
        return output.toString();
    }
}
