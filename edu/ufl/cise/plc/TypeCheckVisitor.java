package edu.ufl.cise.plc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.ASTVisitor;
import edu.ufl.cise.plc.ast.AssignmentStatement;
import edu.ufl.cise.plc.ast.BinaryExpr;
import edu.ufl.cise.plc.ast.BooleanLitExpr;
import edu.ufl.cise.plc.ast.ColorConstExpr;
import edu.ufl.cise.plc.ast.ColorExpr;
import edu.ufl.cise.plc.ast.ConditionalExpr;
import edu.ufl.cise.plc.ast.ConsoleExpr;
import edu.ufl.cise.plc.ast.Declaration;
import edu.ufl.cise.plc.ast.Dimension;
import edu.ufl.cise.plc.ast.Expr;
import edu.ufl.cise.plc.ast.FloatLitExpr;
import edu.ufl.cise.plc.ast.IdentExpr;
import edu.ufl.cise.plc.ast.IntLitExpr;
import edu.ufl.cise.plc.ast.NameDef;
import edu.ufl.cise.plc.ast.NameDefWithDim;
import edu.ufl.cise.plc.ast.PixelSelector;
import edu.ufl.cise.plc.ast.Program;
import edu.ufl.cise.plc.ast.ReadStatement;
import edu.ufl.cise.plc.ast.ReturnStatement;
import edu.ufl.cise.plc.ast.StringLitExpr;
import edu.ufl.cise.plc.ast.Types.Type;
import edu.ufl.cise.plc.ast.UnaryExpr;
import edu.ufl.cise.plc.ast.UnaryExprPostfix;
import edu.ufl.cise.plc.ast.VarDeclaration;
import edu.ufl.cise.plc.ast.WriteStatement;

import static edu.ufl.cise.plc.ast.Types.Type.*;

public class TypeCheckVisitor implements ASTVisitor {

	SymbolTable symbolTable = new SymbolTable();  
	Program root;
	
	record Pair<T0,T1>(T0 t0, T1 t1){};  //may be useful for constructing lookup tables.

	private void check(boolean condition, ASTNode node, String message) throws TypeCheckException {
		if (!condition) {
			throw new TypeCheckException(message, node.getSourceLoc());
		}
	}
	
	//The type of a BooleanLitExpr is always BOOLEAN.  
	//Set the type in AST Node for later passes (code generation)
	//Return the type for convenience in this visitor.  
	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		booleanLitExpr.setType(Type.BOOLEAN);
		return Type.BOOLEAN;
	}

	@Override
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		stringLitExpr.setType(Type.STRING);
		return Type.STRING;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		intLitExpr.setType(Type.INT);
		return Type.INT;
	}

	@Override
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		floatLitExpr.setType(Type.FLOAT);
		return Type.FLOAT;
	}

	@Override
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		colorConstExpr.setType(Type.COLOR);
		return Type.COLOR;
	}

	@Override
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		consoleExpr.setType(Type.CONSOLE);
		return Type.CONSOLE;
	}
	
	//Visits the child expressions to get their type (and ensure they are correctly typed)
	//then checks the given conditions.
	//TODO check that color Expr variables that do not exist get added
	@Override
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {


		Type redType = (Type) colorExpr.getRed().visit(this, arg);
		Type greenType = (Type) colorExpr.getGreen().visit(this, arg);
		Type blueType = (Type) colorExpr.getBlue().visit(this, arg);
		check(redType == greenType && redType == blueType, colorExpr, "color components must have same type");
		check(redType == Type.INT || redType == Type.FLOAT, colorExpr, "color component type must be int or float");
		Type exprType = (redType == Type.INT) ? Type.COLOR : Type.COLORFLOAT;
		colorExpr.setType(exprType);
		return exprType;
	}	

	
	
	//Maps forms a lookup table that maps an operator expression pair into result type.  
	//This more convenient than a long chain of if-else statements. 
	//Given combinations are legal; if the operator expression pair is not in the map, it is an error. 
	Map<Pair<Kind,Type>, Type> unaryExprs = Map.of(
			new Pair<Kind,Type>(Kind.BANG,BOOLEAN), BOOLEAN,
			new Pair<Kind,Type>(Kind.MINUS, FLOAT), FLOAT,
			new Pair<Kind,Type>(Kind.MINUS, INT),INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,INT), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,COLOR), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,IMAGE), IMAGE,
			new Pair<Kind,Type>(Kind.IMAGE_OP,IMAGE), INT
			);


	
	//Visits the child expression to get the type, then uses the above table to determine the result type
	//and check that this node represents a legal combination of operator and expression type. 
	@Override
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		// !, -, getRed, getGreen, getBlue
		Kind op = unaryExpr.getOp().getKind();
		Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);
		//Use the lookup table above to both check for a legal combination of operator and expression, and to get result type.
		Type resultType = unaryExprs.get(new Pair<Kind,Type>(op,exprType));
		check(resultType != null, unaryExpr, "incompatible types for unaryExpr");
		//Save the type of the unary expression in the AST node for use in code generation later. 
		unaryExpr.setType(resultType);
		//return the type for convenience in this visitor.
		return resultType;
	}


	//This method has several cases. Work incrementally and test as you go. 
	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		Kind op = binaryExpr.getOp().getKind();
		Type leftType = (Type) binaryExpr.getLeft().visit(this, arg);
		Type rightType = (Type) binaryExpr.getRight().visit(this, arg);
		Type resultType = null;
		switch(op){
			case AND, OR -> {
				if(leftType == BOOLEAN && rightType == BOOLEAN) resultType = BOOLEAN;
				else check(false, binaryExpr, "Incompatible operators: " + leftType + " and " + rightType);
			}
			case EQUALS, NOT_EQUALS -> 	{
				if(leftType == rightType) resultType = BOOLEAN;
				else check(false, binaryExpr, "Incompatible operators: " + leftType + " and " + rightType);

			}
			case PLUS, MINUS ->  {
				if(leftType == INT && rightType == INT) resultType = INT;
				else if(leftType == FLOAT && rightType == FLOAT) resultType = FLOAT;
				else if(leftType == INT && rightType == FLOAT) {
					resultType = FLOAT;
					binaryExpr.getLeft().setCoerceTo(FLOAT);
				}
				else if(leftType == FLOAT && rightType == INT){
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = FLOAT;
				}
				else if(leftType == COLOR && rightType == COLOR) resultType = COLOR;
				else if(leftType == COLORFLOAT && rightType == COLORFLOAT) resultType = COLORFLOAT;
				else if(leftType == COLORFLOAT && rightType == COLOR) {
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == COLOR && rightType == COLORFLOAT) {
					binaryExpr.getLeft().setType(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == IMAGE && rightType == IMAGE) resultType = IMAGE;
				else check(false, binaryExpr, "Incompatible operators: " + leftType + " and " + rightType);
			}
			case TIMES, DIV, MOD -> {
				if(leftType == INT && rightType == INT) resultType = INT;
				else if(leftType == FLOAT && rightType == FLOAT) resultType = FLOAT;
				else if(leftType == INT && rightType == FLOAT) {
					resultType = FLOAT;
					binaryExpr.getLeft().setCoerceTo(FLOAT);
				}
				else if(leftType == FLOAT && rightType == INT){
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = FLOAT;
				}
				else if(leftType == COLOR && rightType == COLOR) resultType = COLOR;
				else if(leftType == COLORFLOAT && rightType == COLORFLOAT) resultType = COLORFLOAT;
				else if(leftType == COLORFLOAT && rightType == COLOR) {
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == COLOR && rightType == COLORFLOAT) {
					binaryExpr.getLeft().setType(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == IMAGE && rightType == IMAGE) resultType = IMAGE;

				else if(leftType == IMAGE && rightType == INT) resultType = IMAGE;
				else if(leftType == IMAGE && rightType == FLOAT) resultType = IMAGE;
				else if(leftType == INT && rightType == COLOR){
					binaryExpr.getLeft().setCoerceTo(COLOR);
					resultType = COLOR;
				}
				else if(leftType == COLOR && rightType == INT) {
					binaryExpr.getRight().setCoerceTo(COLOR);
					resultType = COLOR;
				}
				else if(leftType == FLOAT && rightType == COLOR) {
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else if(leftType == COLOR && rightType == FLOAT){
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					resultType = COLORFLOAT;
				}
				else check(false, binaryExpr, "Incompatible operators: " + leftType + " and " + rightType);
			}
			case LT, LE, GT, GE -> {
				if(leftType == INT && rightType == INT) resultType = BOOLEAN;
				else if(leftType == FLOAT && rightType == FLOAT) resultType = BOOLEAN;
				else if(leftType == INT && rightType == FLOAT){
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					resultType = BOOLEAN;
				}
				else if(leftType == FLOAT && rightType == INT){
					binaryExpr.getRight().setCoerceTo(FLOAT);
					resultType = BOOLEAN;
				}
				else check(false, binaryExpr, "Incompatible operators: " + leftType + " and " + rightType);
			}
		}
		binaryExpr.setType(resultType);
		return resultType;
	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		String name = identExpr.getText();
		Declaration dec = symbolTable.lookup(name);
		check(dec != null, identExpr, "undefined identifier " + name);
		check(dec.isInitialized(), identExpr, "using uninitialized variable " + name);
		identExpr.setDec(dec);
		Type type = dec.getType();
		identExpr.setType(type);
		return type;
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		Type condition = (Type) conditionalExpr.getCondition().visit(this, arg);
		check(condition == BOOLEAN, conditionalExpr, "Condition has to be boolean");

		Type trueCase = (Type) conditionalExpr.getTrueCase().visit(this, arg);
		Type falseCase = (Type) conditionalExpr.getFalseCase().visit(this, arg);

		check(trueCase == falseCase, conditionalExpr, "True Case type must equal False case type");
		conditionalExpr.setType(trueCase);
		return trueCase;
	}

	@Override
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		Type height = (Type) dimension.getHeight().visit(this, arg);
		Type width = (Type) dimension.getWidth().visit(this, arg);
		check(height == INT && width == INT, dimension, "Height and Width must be INT");
		return null;
	}

	@Override
	//This method can only be used to check PixelSelector objects on the right hand side of an assignment. 
	//Either modify to pass in context info and add code to handle both cases, or when on left side
	//of assignment, check fields from parent assignment statement.
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {

		Type xType = (Type) pixelSelector.getX().visit(this, arg);
		check(xType == Type.INT, pixelSelector.getX(), "only ints as pixel selector components");
		Type yType = (Type) pixelSelector.getY().visit(this, arg);
		check(yType == Type.INT, pixelSelector.getY(), "only ints as pixel selector components");
		return null;
	}

	@Override
	//This method several cases--you don't have to implement them all at once.
	//Work incrementally and systematically, testing as you go.  
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		String name = assignmentStatement.getName();
		Type targetType = (Type) symbolTable.lookup(name).getType();

		Expr initializer = assignmentStatement.getExpr();
		PixelSelector ps = assignmentStatement.getSelector();
		assignmentStatement.setTargetDec(symbolTable.lookup(name));
		if(ps != null){
			Expr x = ps.getX();
			Expr y = ps.getY();
			check(x instanceof  IdentExpr && y instanceof IdentExpr, assignmentStatement, "X and Y must be of type IdentExpr");
			Declaration xDef = new NameDef(x.getFirstToken(), "int", x.getText());
			Declaration yDef = new NameDef(y.getFirstToken(), "int", y.getText());
			check(symbolTable.lookup(x.getText()) == null && symbolTable.lookup(y.getText()) == null, ps, "PixelSelector variable names already in use: " + x.getText() + ", " + y.getText());
			check(x instanceof IdentExpr && y instanceof IdentExpr, ps, "X and Y must of type IdentExpr");

			check(symbolTable.lookup(x.getText()) == null && symbolTable.lookup(y.getText()) == null, assignmentStatement, "Local variable name already exists!");
			symbolTable.insert(x.getText(), xDef);
			symbolTable.insert(y.getText(), yDef);
			symbolTable.lookup(x.getText()).setInitialized(true);
			symbolTable.lookup(y.getText()).setInitialized(true);
			ps.visit(this, arg);
			Type initializerType = (Type) initializer.visit(this, arg);

			symbolTable.entries.remove(x.getText());
			symbolTable.entries.remove(y.getText()); //After done with text

			Declaration declaration = assignmentStatement.getTargetDec();

			symbolTable.lookup(name).setInitialized(true);
			if(initializerType == COLOR || initializerType == COLORFLOAT || initializerType == FLOAT || initializerType == INT)
				initializer.setCoerceTo(COLOR);
			else
				check(false, assignmentStatement, "Cannot have IMAGE PixelSelctor and " + initializerType);

			return null;
		}
		Type initializerType = (Type) initializer.visit(this, arg);
		Declaration declaration = assignmentStatement.getTargetDec();
		//declaration.setInitialized(true);
		symbolTable.lookup(name).setInitialized(true);

		if(targetType != IMAGE){
			PixelSelector assignmentPS = assignmentStatement.getSelector();
			check(assignmentPS == null, assignmentStatement, "Cannot have pixel selector");
			check(checkVarCompatibility(targetType, initializerType), assignmentStatement, "Type: " + targetType + " cannot be equal to type: " + initializerType);
			if(targetType != initializerType){
				initializer.setCoerceTo(targetType);
			}
		}
		else {

			if(ps == null){
				if(initializerType == INT) initializer.setCoerceTo(COLOR);
				else if(initializerType == FLOAT) initializer.setCoerceTo(COLORFLOAT);
				else if(initializerType != COLOR && initializerType != COLORFLOAT && initializerType != IMAGE) check(false, assignmentStatement, "Cannot have IMAGE and " + initializerType);
			}

		}
		return null;
	}

	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		Type sourceType = (Type) writeStatement.getSource().visit(this, arg);
		Type destType = (Type) writeStatement.getDest().visit(this, arg);
		check(destType == Type.STRING || destType == Type.CONSOLE, writeStatement,
				"illegal destination type for write");
		check(sourceType != Type.CONSOLE, writeStatement, "illegal source type for write");
		return null;
	}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		Type rhsType = (Type) readStatement.getSource().visit(this, arg);
		String name = readStatement.getName();
		Type targetType = (Type) symbolTable.lookup(name).getType();
		check(rhsType == CONSOLE || rhsType == STRING, readStatement, "Must have CONSOLE or STRING");
		check(readStatement.getSelector() == null, readStatement, "Cannot have Pixel Selector");
		if(rhsType != STRING)
			readStatement.getSource().setCoerceTo(targetType);
		readStatement.setTargetDec(symbolTable.lookup(name));
		symbolTable.lookup(name).setInitialized(true);
		return null;
	}


	private boolean checkVarCompatibility(Type targetType, Type rhsType){
		return(targetType == rhsType || (targetType == INT && rhsType == FLOAT)
				|| (targetType == FLOAT && rhsType == INT) || (targetType == INT && rhsType == COLOR)
				|| (targetType == COLOR && rhsType == INT));
	};
	@Override
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		NameDef nameDefDec = declaration.getNameDef();

		Type nameDefType = (Type) nameDefDec.visit(this, arg);;
		Expr initializer = declaration.getExpr();
		if(nameDefType == IMAGE){
			if(initializer == null){
				if(declaration.getDim() != null)
					declaration.getDim().visit(this, arg);
				else
					check(false, declaration, "Image must be initialized or have a dimension");
			}
			else{
				//TODO: account for assignment values and read values.

				Kind op = declaration.getOp().getKind();

				if(op == Kind.LARROW){
					Type initializerType = (Type) declaration.getExpr().visit(this, arg);
					check(initializerType == CONSOLE || initializerType == STRING, declaration, "Must have CONSOLE or STRING on RHS of IMAGE read statement!");
					nameDefDec.setInitialized(true);
					declaration.setInitialized(true);
					return null;
				}
				else if(op == Kind.ASSIGN){
					Type initializerType = (Type) declaration.getExpr().visit(this, arg);
					check(initializerType == IMAGE || declaration.getDim() != null, declaration, "Must have IMAGE or dimension!");
					nameDefDec.setInitialized(true);
					declaration.setInitialized(true);

					if(initializerType == INT) initializer.setCoerceTo(COLOR);
					else if(initializerType == FLOAT) initializer.setCoerceTo(COLORFLOAT);
					else if(initializerType != COLOR && initializerType != COLORFLOAT && initializerType != IMAGE) check(false, declaration, "Cannot have IMAGE and " + initializerType);
				}

			}

		}
		else if(initializer != null){
			Type initializerType = (Type) declaration.getExpr().visit(this, arg);
			Kind op = declaration.getOp().getKind();
			if(op == Kind.ASSIGN){
				check(checkVarCompatibility(nameDefType, initializerType), declaration, "Cannot have target type: " + nameDefType + " and source type: " + initializerType);
				nameDefDec.setInitialized(true);
				declaration.setInitialized(true);
				if(nameDefType != initializerType){
					initializer.setCoerceTo(nameDefType);
				}
			}
			else if(op == Kind.LARROW){
				check(initializerType == CONSOLE | initializerType == STRING, declaration, "Must have CONSOLE or STRING");
				if(initializerType != STRING)
					initializer.setCoerceTo(nameDefType);

				nameDefDec.setInitialized(true);
				declaration.setInitialized(true);
			}

		}
		return null;
	}


	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {		

		//Save root of AST so return type can be accessed in return statements
		root = program;
		List<NameDef> params = program.getParams();
		for(NameDef param: params){
			param.visit(this, arg);
			param.setInitialized(true);
		}
		//Check declarations and statements
		List<ASTNode> decsAndStatements = program.getDecsAndStatements();
		for (ASTNode node : decsAndStatements) {
			node.visit(this, arg);
		}
		return program;
	}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		String name = nameDef.getName();
		boolean inserted = symbolTable.insert(name, nameDef);
		check(inserted, nameDef, "variable " + name + "already declared");
		return symbolTable.lookup(name).getType();
	}

	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		String name = nameDefWithDim.getName();
		nameDefWithDim.getDim().visit(this, arg);
		boolean inserted = symbolTable.insert(name, nameDefWithDim);
		check(inserted, nameDefWithDim, "variable " + name + "already declared");
		return symbolTable.lookup(name).getType();
	}
 
	@Override
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		Type returnType = root.getReturnType();  //This is why we save program in visitProgram.
		Type expressionType = (Type) returnStatement.getExpr().visit(this, arg);
		check(returnType == expressionType, returnStatement, "return statement with invalid type");
		return null;
	}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		Type expType = (Type) unaryExprPostfix.getExpr().visit(this, arg);
		check(expType == Type.IMAGE, unaryExprPostfix, "pixel selector can only be applied to image");
		unaryExprPostfix.getSelector().visit(this, arg);
		unaryExprPostfix.setType(Type.INT);
		unaryExprPostfix.setCoerceTo(COLOR);
		return Type.COLOR;
	}

}
