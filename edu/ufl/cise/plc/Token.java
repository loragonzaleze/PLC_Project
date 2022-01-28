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
        return Integer.parseInt(input);
    }

    @Override
    public float getFloatValue() {
        return Float.parseFloat(input);
    }

    @Override
    public boolean getBooleanValue() {
        return false;
    }

    @Override
    public String getStringValue() {
        return null;
    }
}
