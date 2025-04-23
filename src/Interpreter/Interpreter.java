package Interpreter;

import AST.*;
import com.sun.jdi.IntegerType;

import java.util.*;

public class Interpreter {


    private final TranNode top;

    /** Constructor - get the interpreter ready to run. Set members from parameters and "prepare" the class.
     *
     * Store the tran node.
     * Add any built-in methods to the AST
     * @param top - the head of the AST
     */
    public Interpreter(TranNode top) {
        this.top = top;
        var classNode = new ClassNode();
        classNode.name = "console";

        var x = new ConsoleWrite();
        x.isShared = true;
        x.name = "write";
        x.isVariadic = true;
        classNode.methods.add(x);//made a class and given a method
        top.Classes.add(classNode);

    }

    /**
     * This is the public interface to the interpreter. After parsing, we will create an interpreter and call start to
     * start interpreting the code.
     *
     * Search the classes in Tran for a method that is "isShared", named "start", that is not private and has no parameters
     * Call "InterpretMethodCall" on that method, then return.
     * Throw an exception if no such method exists.
     */

    public void start() {
        // Find the "start" method
        var tranNode = new TranNode();
        for (var classNode : top.Classes) {
            ObjectIDT object = new ObjectIDT(classNode);
            for (var m : classNode.methods){//method in list of methods
                if (Objects.equals(m.name, "start")) {
                    if (m.isShared) {
                        if (m.parameters.isEmpty()) {
                            //create list of empty values
                            LinkedList <InterpreterDataType> values = new LinkedList<>();

                            interpretMethodCall(Optional.of(object), m, values);
                            return;
                        }
                    }
                }
            }
        }
        throw new RuntimeException("No 'start' method found");
    }

    //              Running Methods

    /**
     * Find the method (local to this class, shared (like Java's system.out.print), or a method on another class)
     * Evaluate the parameters to have a list of values
     * Use interpretMethodCall() to actually run the method.
     *
     * Call GetParameters() to get the parameter value list
     * Find the method. This is tricky - there are several cases:
     * someLocalMethod() - has NO object name. Look in "object"
     * console.write() - the objectName is a CLASS and the method is shared
     * bestStudent.getGPA() - the objectName is a local or a member
     *
     * Once you find the method, call InterpretMethodCall() on it. Return the list that it returns.
     * Throw an exception if we can't find a match.
     * @param object - the object we are inside right now (might be empty)
     * @param locals - the current local variables
     * @param mc - the method call
     * @return - the return values
     */
    private List<InterpreterDataType> findMethodForMethodCallAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) {
        //try writing this inverted (use exceptions so the method uses one return statement

        List<InterpreterDataType> result = null;
        //call getParameters()
        result = getParameters(object, locals, mc);

        //find the method
        //someLocalMethod() - has NO object name. Look in "object"
        if(mc.objectName == null) {
            MethodDeclarationNode method = getMethodFromObject(object.get(), mc, result);
            interpretMethodCall(object, method, result);
            return result;
        }

        if(mc.objectName != null) {
            Optional <ClassNode> classNode = getClassByName(mc.objectName.get());
            //console.write() - the objectName is a CLASS and the method is shared
            if (classNode.isPresent()) { //IS a class and IS shared
                ObjectIDT objectIDT = new ObjectIDT(classNode.get());
                MethodDeclarationNode method = getMethodFromObject(objectIDT, mc, result);
                if(method.isShared ){ //if the object is not console
                    interpretMethodCall(object, method, result);
                    return result;
                }
            }
            //bestStudent.getGPA() - the objectName is a local or a member
            else if(locals.containsKey(mc.objectName.get())) {
                //get refIDT - use findVariable()
                InterpreterDataType refIDT = new ReferenceIDT();
                refIDT = findVariable(mc.objectName.get(), locals,object);
                MethodDeclarationNode method = getMethodFromObject(object.get(), mc, result);
                interpretMethodCall(((ReferenceIDT)refIDT).refersTo, method, result);
                return result;
            }

        }

        //return the list that it returns
        throw new RuntimeException("Match not found"); //return result;
    }

    /*

        //call getParameters()
        getParameters(object, locals, mc);
        ClassNode classNode = new ClassNode();
        ObjectIDT objIDT = new ObjectIDT(classNode);

        //find the method
        getMethodFromObject(objIDT, mc, getParameters(object, locals, mc));



    ReferenceIDT refIDT = new ReferenceIDT();

        for(var m : object.astNode.methods) {//for method in currentClass.methods list
            if(m.isShared){
                if(mc.objectName.isPresent()){
                    if(doesMatch(m, mc, parameters)){
                        return m;
                    }
                }
            }//if not shared - use referenceIDT
            else if(m.isPrivate){
                if(doesMatch(m, mc, parameters)){
                    return m;
                }
            }
            //if none, look thru current class in object class method list and find one that has the right parameters
            else {
                if (doesMatch(m, mc, parameters)){
                    return m;
                }
            }
        }
        */

    /**
     * Run a "prepared" method (found, parameters evaluated)
     * This is split from findMethodForMethodCallAndRunIt() because there are a few cases where we don't need to do the finding:
     * in start() and dealing with loops with iterator objects, for example.
     *
     * Check to see if "m" is a built-in. If so, call Execute() on it and return
     * Make local variables, per "m"
     * If the number of passed in values doesn't match m's "expectations", throw
     * Add the parameters by name to locals.
     * Call InterpretStatementBlock
     * Build the return list - find the names from "m", then get the values for those names and add them to the list.
     * @param object - The object this method is being called on (might be empty for shared)
     * @param m - Which method is being called
     * @param values - The values to be passed in
     * @return the returned values from the method
     */
    private List<InterpreterDataType> interpretMethodCall(Optional<ObjectIDT> object, MethodDeclarationNode m, List<InterpreterDataType> values) {
        var retVal = new LinkedList<InterpreterDataType>();
        //Check to see if "m" is a built-in. If so, call Execute() on it and return
        if(m instanceof BuiltInMethodDeclarationNode){
            BuiltInMethodDeclarationNode builtIn = (BuiltInMethodDeclarationNode)m;
            return builtIn.Execute(values);
        }
        //make local variables per "m"
        HashMap<String, InterpreterDataType> localVar = new HashMap<>();
        for(var item : m.locals){
            localVar.put(item.name, instantiate(item.type));
        }

        if(!(m.locals.size() == localVar.size())){
            throw new RuntimeException("Number of passed in values does not match number of local values");
        }
        //add the parameters by name to locals
        for(var value : m.parameters){
            localVar.put(value.name, instantiate(value.type));
        }

        //Build the return list - find the names from "m", then get the values for those names and add them to the list.
        for(var names : m.returns){
            retVal.add(instantiate(names.type));
            localVar.put(names.name, instantiate(names.type));
        }

        //call InterpretStatementBlock
        interpretStatementBlock(object, m.statements, localVar);

        return retVal;
    }

    //              Running Constructors

    /**
     * This is a special case of the code for methods. Just different enough to make it worthwhile to split it out.
     *
     * Call GetParameters() to populate a list of IDT's
     * Call GetClassByName() to find the class for the constructor
     * If we didn't find the class, throw an exception
     * Find a constructor that is a good match - use DoesConstructorMatch()
     * Call InterpretConstructorCall() on the good match
     * @param callerObj - the object that we are inside when we called the constructor
     * @param locals - the current local variables (used to fill parameters)
     * @param mc  - the method call for this construction
     * @param newOne - the object that we just created that we are calling the constructor for
     */
    private void findConstructorAndRunIt(Optional<ObjectIDT> callerObj, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc, ObjectIDT newOne) {
        //getParameters(callerObj, locals, mc);
        List<InterpreterDataType> params = getParameters(callerObj, locals, mc);

        //find the class for the constructor
        if(!(getClassByName(newOne.astNode.name)).isPresent()){
            throw new RuntimeException("Class not found");
        }

        for(var member : newOne.astNode.members){
            newOne.members.put(member.declaration.name, instantiate(member.declaration.type));
            locals.put(member.declaration.name, instantiate(member.declaration.type));
        }

        for(var construct : newOne.astNode.constructors){
            if(doesConstructorMatch(construct, mc, params)){
                interpretConstructorCall(newOne, construct, params);
            }
        }

    }

    /**
     * Similar to interpretMethodCall, but "just different enough" - for example, constructors don't return anything.
     *
     * Creates local variables (as defined by the ConstructorNode), calls Instantiate() to do the creation
     * Checks to ensure that the right number of parameters were passed in, if not throw.
     * Adds the parameters (with the names from the ConstructorNode) to the locals.
     * Calls InterpretStatementBlock
     * @param object - the object that we allocated
     * @param c - which constructor is being called
     * @param values - the parameter values being passed to the constructor
     */
    private void interpretConstructorCall(ObjectIDT object, ConstructorNode c, List<InterpreterDataType> values) {
        //Creates local variables, calls instantiate() to do the creation
        HashMap<String, InterpreterDataType> localVar = new HashMap<>();
        for(var locals : c.locals){
            localVar.put(locals.name, instantiate(locals.type));
        }
        //Checks to ensure that the right number of parameters were passed in, if not throw
        if(!(c.parameters.size() == localVar.size())){
            throw new RuntimeException("Number of passed in values does not match number of local values");
        }
        //Adds the parameters (with the names from the ConstructorNode) to the locals
        for(var value : c.parameters){
            localVar.put(value.name, instantiate(value.type));
        }
        //call interpretStatementBlock()
        interpretStatementBlock(Optional.ofNullable(object), c.statements, localVar);
    }

    //              Running Instructions

    /**
     * Given a block (which could be from a method or an "if" or "loop" block, run each statement.)
     * Blocks, by definition, do every statement, so iterating over the statements makes sense.
     *
     * For each statement in statements:
     * check the type:
     *      For AssignmentNode, FindVariable() to get the target. Evaluate() the expression. Call Assign() on the target with the result of Evaluate()
     *      For MethodCallStatementNode, call doMethodCall(). Loop over the returned values and copy the into our local variables
     *      For LoopNode - there are 2 kinds.
     *          Setup:
     *          If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface)
     *              Find the "getNext()" method; throw an exception if there isn't one
     *          Loop:
     *          While we are not done:
     *              if this is a boolean loop, Evaluate() to get true or false.
     *              if this is an iterator, call "getNext()" - it has 2 return values. The first is a boolean (was there another?), the second is a value
     *              If the loop has an assignment variable, populate it: for boolean loops, the true/false. For iterators, the "second value"
     *              If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
     *       For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
     * @param object - the object that this statement block belongs to (used to get member variables and any members without an object)
     * @param statements - the statements to run
     * @param locals - the local variables
     */
    private void interpretStatementBlock(Optional<ObjectIDT> object, List<StatementNode> statements, HashMap<String, InterpreterDataType> locals) {
        for(var statement : statements) {
            //AssignmentNode
            if(statement instanceof AssignmentNode a){
                InterpreterDataType target = findVariable(a.target.name, locals, object);
                target.Assign(evaluate(locals, object, a.expression));
            }

            //MethodCallStatementNode
            if (statement instanceof MethodCallStatementNode) {
                MethodCallStatementNode mc = (MethodCallStatementNode)statement;
                //call doMethodCall()
                findMethodForMethodCallAndRunIt(object, locals, mc);
                //Loop over the returned values and copy the into our local variables
                for(var returns : mc.returnValues){
                    locals.put(returns.name, instantiate(returns.name));
                }

            }
            //LoopNode
            /*
            else if(statement instanceof LoopNode){
                LoopNode loopNode = (LoopNode)statement;

                ClassNode classNode = new ClassNode();
                ObjectIDT objectIDT = new ObjectIDT(classNode);


                //Find the "getNext()" method; throw an exception if there isn't one
                //evaluate(locals, object, loopNode.expression);

                //If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface)
                //make iterator instance : if boolean, use get next
                if(loopNode.equals(objectIDT.astNode.interfaces)){
                    //getNext should return boolean
                    if(!objectIDT.astNode.interfaces.iterator().hasNext()){
                        throw new RuntimeException("Required hasNext() method");
                    }
                    while(loopNode.equals(objectIDT.astNode.interfaces.iterator().next())){

                    }
                }

                //use while(!done) call recursively interpretStatementBlock
            }*/
            //If
            else if (statement instanceof IfNode) {
                //For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements.
                IfNode ifNode = (IfNode)statement;
                //HashMap<String, InterpreterDataType>, Optional<ObjectIDT>, ExpressionNode
                evaluate(locals, object, ifNode.condition);
                if(ifNode.condition != null) {
                    interpretStatementBlock(object, statements, locals);
                }
                //If not AND there is an else, InterpretStatementBlock on the else body
                else if(ifNode == null && ifNode.elseStatement != null) {
                    ElseNode elseNode = (ElseNode)statement;
                    interpretStatementBlock(object, statements, locals);
                }
            }
        }
    }

    /**
     *  evaluate() processes everything that is an expression - math, variables, boolean expressions.
     *  There is a good bit of recursion in here, since math and comparisons have left and right sides that need to be evaluated.
     *
     * See the How To Write an Interpreter document for examples
     * For each possible ExpressionNode, do the work to resolve it:
     * BooleanLiteralNode - create a new BooleanLiteralNode with the same value
     *      - Same for all of the basic data types
     * BooleanOpNode - Evaluate() left and right, then perform either and/or on the results.
     * CompareNode - Evaluate() both sides. Do good comparison for each data type
     * MathOpNode - Evaluate() both sides. If they are both numbers, do the math using the built-in operators. Also handle String + String as concatenation (like Java)
     * MethodCallExpression - call doMethodCall() and return the first value
     * VariableReferenceNode - call findVariable()
     * @param locals the local variables
     * @param object - the current object we are running
     * @param expression - some expression to evaluate
     * @return a value
     */
    private InterpreterDataType evaluate(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, ExpressionNode expression) {
        //Basic Data Types Literal Node
        if(expression instanceof BooleanLiteralNode) {
            BooleanLiteralNode bL = (BooleanLiteralNode)expression;
            return new BooleanIDT(bL.value);
        }
        else if(expression instanceof StringLiteralNode) {
            StringLiteralNode sL = (StringLiteralNode)expression;
            return new StringIDT(sL.value);
        }
        else if(expression instanceof NumericLiteralNode) {
            NumericLiteralNode nL = (NumericLiteralNode)expression;
            return new NumberIDT(nL.value);
        }
        else if(expression instanceof CharLiteralNode) {
            CharLiteralNode cL = (CharLiteralNode)expression;
            return new CharIDT(cL.value);
        }
        //BooleanOpNode - Evaluate() left and right                     --might not need to implement this (did not use and/or in parser)

        //CompareNode - fix the return statements
        else if(expression instanceof CompareNode){
            CompareNode c = (CompareNode)expression;
            InterpreterDataType l = evaluate(locals, object, c.left);
            InterpreterDataType r = evaluate(locals, object, c.right);

            boolean result;
            //Do good comparison for each data type
            //ex: n >= 15 , 7 < 9 , foo > fie(?)

            //Numbers
            if((l instanceof NumberIDT left) && (r instanceof NumberIDT right)){
                switch(c.op){
                    case lt:
                        result = left.Value < right.Value;
                        break;
                    case le:
                        result = left.Value <= right.Value;
                        break;
                    case gt:
                        result = left.Value > right.Value;
                        break;
                    case ge:
                        result = left.Value >= right.Value;
                        break;
                    case eq:
                        result = left.Value == right.Value;
                        break;
                    case ne:
                        result = left.Value != right.Value;
                        break;
                    default:
                        throw new RuntimeException("Unknown compare operation");
                }
                return new BooleanIDT(result);
            }
            //Boolean
            if((l instanceof BooleanIDT left) && (r instanceof BooleanIDT right)){
                switch(c.op){
                    case eq:
                        result = left.Value == right.Value;
                        break;
                    case ne:
                        result = left.Value != right.Value;
                        break;
                    default:
                        throw new RuntimeException("Unknown compare operation");
                }
                return new BooleanIDT(result);
            }
            //String
            // <,>,<=,>= cannot be used with strings
            if((l instanceof StringIDT left) && (r instanceof StringIDT right)){
                String leftString = left.Value;
                String rightString = right.Value;
                switch(c.op){
                    case lt:
                        result = leftString.compareTo(rightString) < 0; //autofilled. is this correct?
                        break;
                    case le:
                        result = leftString.compareTo(rightString) <= 0;
                        break;
                    case gt:
                        result = leftString.compareTo(rightString) > 0;
                        break;
                    case ge:
                        result = leftString.compareTo(rightString) >= 0;
                        break;
                    case eq:
                        result = leftString.compareTo(rightString) == 0;
                        break;
                    case ne:
                        result = !leftString.equals(rightString); //would this yield the intended result?
                        break;
                    default:
                        throw new RuntimeException("Unknown compare operation");
                }
                return new BooleanIDT(result);
            }
            //Char
            if((l instanceof CharIDT left) && (r instanceof CharIDT right)){
                char leftChar = left.Value;
                char rightChar = right.Value;
                switch(c.op){
                    case lt:
                        //result = left.Value < right.Value;
                        result = leftChar < rightChar;
                        break;
                    case le:
                        result = leftChar <= rightChar;
                        break;
                    case gt:
                        result = leftChar > rightChar;
                        break;
                    case ge:
                        result = leftChar >= rightChar;
                        break;
                    case eq:
                        result = leftChar == rightChar;
                        break;
                    case ne:
                        result = leftChar != rightChar;
                        break;
                    default:
                        throw new RuntimeException("Unknown compare operation");
                }
                return new BooleanIDT(result);
            }
        }
        //MathOpNode - Evaluate() both sides.
        else if(expression instanceof MathOpNode) {
            MathOpNode m = (MathOpNode)expression;
            InterpreterDataType l = evaluate(locals, object, m.left);
            InterpreterDataType r = evaluate(locals, object, m.right);

            //Also handle String + String as concatenation (like Java) - concatenation uses addition operator but isn't adding numbers
            //handle this case (before numbers) that doesn't involve numbers
            if(m.op == MathOpNode.MathOperations.add && l instanceof StringIDT || r instanceof StringIDT) {
                return new StringIDT(l.toString() + r.toString());
            }

            //If they are both numbers, do the math using the built-in operators.
            if((l instanceof NumberIDT left) && (r instanceof NumberIDT right)){//changed from && to ||
                float result; //ex: result = 7.05
                switch(m.op){
                    case add:
                        result = left.Value + right.Value;
                        break;
                    case subtract:
                        result = left.Value - right.Value;
                        break;
                    case multiply:
                        result = left.Value * right.Value;
                        break;
                    case divide:
                        result = left.Value / right.Value;
                        break;
                    case modulo:
                        result = left.Value % right.Value;
                        break;
                    default:
                        throw new RuntimeException("Unknown math operation");
                }
                return new NumberIDT(result);
            }
        }
        //MethodCallExpression - call doMethodCall() and return the first value
        else if(expression instanceof MethodCallExpressionNode){
            MethodDeclarationNode m = (MethodDeclarationNode)expression;
            LinkedList <InterpreterDataType> values = new LinkedList<>();
            for(var rv : locals.values()) {
                values.push(rv);
            }
            return interpretMethodCall(object, m, values).getFirst();
        }

        //VariableReferenceNode - call findVariable()
        else if(expression instanceof VariableReferenceNode){
            VariableReferenceNode vr = (VariableReferenceNode)expression;
            return findVariable(vr.name, locals, object);
        }

        //NewNode
        else if(expression instanceof NewNode){
            NewNode newNode = (NewNode)expression;
            Optional<ClassNode> classNode = getClassByName(newNode.className);
            ObjectIDT objIDT = new ObjectIDT(classNode.get());

            //make new MethodCallStatementNode and fill all the parts in
            MethodCallStatementNode mcs = new MethodCallStatementNode();
            mcs.objectName = Optional.of(newNode.className);
            mcs.parameters = newNode.parameters;
            mcs.methodName = "";
            //use findConstructorAndRunIt
            findConstructorAndRunIt(object, locals, mcs, objIDT);
            //return objIDT (everything in it is changed because it is a reference, not the value)
            return objIDT;

        }

        throw new IllegalArgumentException(); //throws if all else fails/isn't returned
    }

    //              Utility Methods

    /**
     * Used when trying to find a match to a method call. Given a method declaration, does it match this method call?
     * We double-check with the parameters, too, although in theory JUST checking the declaration to the call should be enough.
     *
     * Match names, parameter counts (both declared count vs method call and declared count vs value list), return counts.
     * If all of those match, consider the types (use TypeMatchToIDT).
     * If everything is OK, return true, else return false.
     * Note - if m is a built-in and isVariadic is true, skip all of the parameter validation.
     * @param m - the method declaration we are considering
     * @param mc - the method call we are trying to match
     * @param parameters - the parameter values for this method call
     * @return does this method match the method call?
     */
    private boolean doesMatch(MethodDeclarationNode m, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {

        //if m is variadic, skip the parameter validation - use BuiltInMethodDeclaration
        if(m instanceof BuiltInMethodDeclarationNode builtIn){
            if(builtIn.isVariadic){
                return true;
            }
        }

        //match names
        if(m.name.equals(mc.methodName)) {
            //match parameter counts
            if(!(parameters.size() == mc.parameters.size())) {
                return false;
            }
            if(!(parameters.size() == m.parameters.size())) {
                return false;
            }
            if(!(mc.returnValues.size() == m.returns.size())) {
                return false;
            }

            //if all of those match, consider the types (use TypeMatchToIDT).
            for(int match = 0; match < parameters.size(); match++) {
                String type = m.parameters.get(match).type; //has a VariableDeclarationNode which has the .type field
                InterpreterDataType parameterType = parameters.get(match);

                if(!type.equals(parameterType.toString())) { //if the types don't match (mc and m should match so this is fine(?)
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Very similar to DoesMatch() except simpler - there are no return values, the name will always match.
     * @param c - a particular constructor
     * @param mc - the method call
     * @param parameters - the parameter values
     * @return does this constructor match the method call?
     */
    private boolean doesConstructorMatch(ConstructorNode c, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        //c and mc - compare types
        //then typeMatchToIDT to parameters
        if(!(c.parameters.size() == mc.parameters.size())) {
            return false;
        }
        if(!(parameters.size() == mc.parameters.size())) {
            return false;
        }

        for(int match = 0; match < parameters.size(); match++) {
            String type = c.parameters.get(match).type;
            InterpreterDataType parameterType = parameters.get(match);
            if(!type.equals(parameterType.toString())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Used when we call a method to get the list of values for the parameters.
     *
     * for each parameter in the method call, call Evaluate() on the parameter to get an IDT and add it to a list
     * @param object - the current object
     * @param locals - the local variables
     * @param mc - a method call
     * @return the list of method values
     */
    private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String,InterpreterDataType> locals, MethodCallStatementNode mc) {
        LinkedList <InterpreterDataType> methodValues = new LinkedList<>();
        for(var parameter : mc.parameters) {
            methodValues.add(evaluate(locals, object, parameter));
        }
        return methodValues; //return null;
    }

    /**
     * Used when we have an IDT and we want to see if it matches a type definition
     * Commonly, when someone is making a function call - do the parameter values match the method declaration?
     *
     * If the IDT is a simple type (boolean, number, etc) - does the string type match the name of that IDT ("boolean", etc)
     * If the IDT is an object, check to see if the name matches OR the class has an interface that matches
     * If the IDT is a reference, check the inner (refered to) type
     * @param type the name of a data type (parameter to a method)
     * @param idt the IDT someone is trying to pass to this method
     * @return is this OK?
     */
    private boolean typeMatchToIDT(String type, InterpreterDataType idt) {

        if(type == "boolean" && idt instanceof BooleanIDT){
            return true;
        }
        if(type == "string" && idt instanceof StringIDT){
            return true;
        }
        if(type == "character" && idt instanceof CharIDT){//or "char"
            return true;
        }
        if(type == "number" && idt instanceof NumberIDT){
            return true;
        }
        if(idt instanceof ObjectIDT object){
            if(type.equals(object.astNode.name) || object.astNode.interfaces.contains(type)){
                return true;
            }
        }
        if(idt instanceof ReferenceIDT reference){
            if(reference.refersTo.equals(type)){
                return true;
            }
        }

        throw new RuntimeException("Unable to resolve type " + type);
    }

    /**
     * Find a method in an object that is the right match for a method call (same name, parameters match, etc. Uses doesMatch() to do most of the work)
     *
     * Given a method call, we want to loop over the methods for that class, looking for a method that matches (use DoesMatch) or throw
     * @param object - an object that we want to find a method on
     * @param mc - the method call
     * @param parameters - the parameter value list
     * @return a method or throws an exception
     */
    private MethodDeclarationNode getMethodFromObject(ObjectIDT object, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        for(var m : object.astNode.methods){
            if(doesMatch(m, mc, parameters)){
                return m;
            }
        }

        throw new RuntimeException("Unable to resolve method call " + mc);
    }

    /**
     * Find a class, given the name. Just loops over the TranNode's classes member, matching by name.
     *
     * Loop over each class in the top node, comparing names to find a match.
     * @param name Name of the class to find
     * @return either a class node or empty if that class doesn't exist
     */
    private Optional<ClassNode> getClassByName(String name) {
        ClassNode classNode = new ClassNode();
        for (var c : top.Classes) {
            if (c.name.equals(name)) {
                classNode = c;
                return Optional.of(classNode);
            }

        }
        return Optional.empty();
    }

    /**
     * Given an execution environment (the current object, the current local variables), find a variable by name.
     *
     * @param name  - the variable that we are looking for
     * @param locals - the current method's local variables
     * @param object - the current object (so we can find members)
     * @return the IDT that we are looking for or throw an exception
     */
    private InterpreterDataType findVariable(String name, HashMap<String,InterpreterDataType> locals, Optional<ObjectIDT> object) {
        ClassNode classNode = new ClassNode();
        ObjectIDT objectIDT = new ObjectIDT(classNode);
        if(locals.containsKey(name)){
            return locals.get(name);
        }
        else if(object.isPresent()){
            objectIDT = object.get();
            if(objectIDT.members.containsKey(name)){
                return objectIDT.members.get(name);
            }
        }

        throw new RuntimeException("Unable to find variable " + name);
    }

    /**
     * Given a string (the type name), make an IDT for it.
     *
     * @param type The name of the type (string, number, boolean, character). Defaults to ReferenceIDT if not one of those.
     * @return an IDT with default values (0 for number, "" for string, false for boolean, ' ' for character)
     */
    private InterpreterDataType instantiate(String type) {
        //instantiate - creating an object from a class
       switch(type){
           case "string":
               return new StringIDT("");
           case "number":
               return new NumberIDT(0);
           case "boolean":
               return new BooleanIDT(false);
           case "char":
               return new CharIDT(' ');
           default:
               return new ReferenceIDT();
       }
    }
}
