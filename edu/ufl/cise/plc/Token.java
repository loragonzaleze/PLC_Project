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
        return null;
    }
}
