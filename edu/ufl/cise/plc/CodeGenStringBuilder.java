package edu.ufl.cise.plc;

public class CodeGenStringBuilder {
    StringBuilder delegate = new StringBuilder();

    public CodeGenStringBuilder append(String str){
        delegate.append(str);
        return this;
    }

    public CodeGenStringBuilder comma(){
        delegate.append(',');
        return this;
    }

    public CodeGenStringBuilder semi(){
        delegate.append(';');
        return this;
    }

    public CodeGenStringBuilder lparen(){
        delegate.append('(');
        return this;
    }

    public CodeGenStringBuilder rparen(){
        delegate.append(')');
        return this;
    }

    public CodeGenStringBuilder lcurlybracket(){
        delegate.append('{');
        return this;
    }

    public CodeGenStringBuilder rcurlybracket(){
        delegate.append("}");
        return this;
    }

    public CodeGenStringBuilder newline(){
        delegate.append('\n');
        return this;
    }

    public CodeGenStringBuilder equal(){
        delegate.append('=');
        return this;
    }

    public CodeGenStringBuilder space(){
        delegate.append(' ');
        return this;
    }

    public CodeGenStringBuilder question(){
        delegate.append('?');
        return this;
    }

    public CodeGenStringBuilder colon(){
        delegate.append(':');
        return this;
    }

    public CodeGenStringBuilder consoleExpr(){
        delegate.append("ConsoleIO.readValueFromConsole");
        return this;
    }

    public CodeGenStringBuilder writeToConsole(){
        delegate.append("ConsoleIO.console.println");
        return this;
    }

    public CodeGenStringBuilder quote(){
        delegate.append('\"');
        return this;
    }

    public CodeGenStringBuilder newKW(){
        delegate.append("new");
        return this;
    }

    public CodeGenStringBuilder colorTuple(){
        delegate.append("ColorTuple");
        return this;
    }

    public CodeGenStringBuilder colorTupleFloat(){
        delegate.append("ColorTupleFloat");
        return this;
    }
}
