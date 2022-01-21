package edu.ufl.cise.plc;

public class Main {
    public static void main(String[] args){

        String input = """
				+ 
				- 	 
				""";
        ILexer lexer = CompilerComponentFactory.getLexer(input);

        for(int i = 0; i < input.length(); i++){
            System.out.println((input.charAt(i) == '\n') + " " + input.charAt(i));
        }
    }
}
