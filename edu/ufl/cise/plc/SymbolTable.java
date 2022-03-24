package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.Declaration;

import java.util.HashMap;

public class SymbolTable {

    public HashMap<String, Declaration> entries = new HashMap<>();

    public boolean insert(String name, Declaration declaration){
        return (entries.putIfAbsent(name, declaration) == null);
    }

    public Declaration lookup(String name){
        return entries.get(name);
    }
}
