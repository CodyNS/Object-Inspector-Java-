import java.util.*;
import java.lang.reflect.*;
import java.lang.IllegalAccessException;


class ClassInspector
{
	InspectorTools t = new InspectorTools(); 

	void printClassInforForObject(Object obj)
	{	
		Class classObj = obj.getClass();
		t.print("         Object's declaring class:  " + classObj.getName());
		t.print("    Object's immediate superclass:  " + superClassName(classObj));

		Class[] interfaces = classObj.getInterfaces();
		String interfacesSt = "";
		for(int i = 0; i < interfaces.length; i++)
			interfacesSt += interfaces[i].getName() + (i == interfaces.length-1 ? "" : ", ");
		if (interfacesSt == "") interfacesSt = "(none)";
		t.print(" Interfaces this class implements:  " + interfacesSt + "\n");
	}
	String superClassName(Class classObj)
	{
		if (classObj.getSuperclass() == null)
			return "";
		return classObj.getSuperclass().getName();
	}
}


class MethodInspector
{
	InspectorTools t = new InspectorTools(); 

	void printMethodsForObject(Object obj, Set<Class> classesInObjHrchy)
	{
		t.print(); t.print(" ----  METHODS (entire hierarchy)  -------------------------------");
		Method[] methods = getAllMethodsForObject(classesInObjHrchy);
		if (methods.length == 0)  t.print(" (none)");
		else for (Method m : methods) printDataForMethod(m);
	}
	Method[] getAllMethodsForObject(Set<Class> classesInObjHrchy)
	{
		Method[] methods = new Method[]{};
		for (Class c : classesInObjHrchy)
			methods = t.combineArrays(methods, c.getDeclaredMethods());
		return methods;
	}
	void printDataForMethod(Method m)
	{
		t.print(
			t.modifiersForItem(m) + 
			t.simplifyTypeName(m.getReturnType()) + " " + 
			m.getName() + "(" + t.parametersForItem(m) + ")" + methodExceptionDetails(m) 
		);
	}
	String methodExceptionDetails(Method m)
	{
		Class[] exceptions = m.getExceptionTypes();
		if (exceptions.length == 0) 
			return "";
		String exceptionSt = " throws ";
		for(int i = 0; i < exceptions.length; i++)
			exceptionSt += (exceptions[i].getName() + (i == exceptions.length-1 ? "" : ", "));
		return exceptionSt;
	}
}


class ConstructorInspector
{
	InspectorTools t = new InspectorTools(); 

	void printConstructorsForObject(Object obj, Set<Class> classesInObjHrchy)
	{
		t.print(); t.print(" ----  CONSTRUCTORS (entire hierarchy)  --------------------------");
		Constructor[] cons = getAllConstructorsForObject(classesInObjHrchy);
		if (cons.length == 0)  t.print(" (none)");
		else for (Constructor c : cons) printConstructorData(c);
	}
	Constructor[] getAllConstructorsForObject(Set<Class> classesInObjHrchy)
	{
		Constructor[] cons = new Constructor[]{};
		for (Class c : classesInObjHrchy)
			cons = t.combineArrays(cons, c.getDeclaredConstructors());
		return cons;
	}
	void printConstructorData(Constructor c)
	{
		t.print(
			t.modifiersForItem(c) +
			t.simplifyTypeName(c.getDeclaringClass()) + "(" + t.parametersForItem(c) + ")"
		);
	}
}


class FieldInspector
{
	InspectorTools t = new InspectorTools(); 

	void printFieldsForObject(Object obj, Set<Class> classesInObjHrchy, List<Field> objectFields)
	{
		t.print(); t.print(" ----  FIELDS (entire hierarchy)  --------------------------------");
		Field[] fields = getAllFieldsForObject(classesInObjHrchy);
		if (fields.length == 0)  t.print(" (none)");
		else for (Field f : fields) printDataForField(f, obj, objectFields);
	}
	Field[] getAllFieldsForObject(Set<Class> classesInObjHrchy)
	{
		Field[] fields = new Field[]{};
		for (Class c : classesInObjHrchy)
			fields = t.combineArrays(fields, c.getDeclaredFields());
		return fields;
	}
	void printDataForField(Field f, Object obj, List<Field> objectFields)
	{
		f.setAccessible(true);
		t.prnt(Inspector.INDENT.repeat(Inspector.indentMultiple) + t.modifiersForItem(f) + t.simplifyTypeName(f.getType()) + " " + f.getName());
		if (f.getType().isArray()) {
			t.prnt(arrayFieldValues(f, obj, objectFields) + "\n");
			return;
		}
		if ( ! f.getType().isPrimitive() && getFieldValue(f, obj) != null )
			objectFields.add(f); // these will be inspected later

		t.prnt(" = " + simpleFieldValue(f, obj) + "\n");
	}
	Object getFieldValue(Field f, Object obj)
	{
		try { return f.get(obj); } 
			catch(IllegalAccessException e){return "IllegalAccessException thrown :(";}
	}
	String simpleFieldValue(Field f, Object obj)
	{
		return f.getType() == String.class ? "\"" + getFieldValue(f, obj) + "\"" : 
											 "" + getFieldValue(f, obj);
	}
	String arrayFieldValues(Field f, Object obj, List<Field> objectFields)
	{
		String arraySt = " = {";
		Object o = getFieldValue(f, obj);
		if ( ! o.getClass().getComponentType().isPrimitive() )
			objectFields.add(f); // inspect this later
		String quoteForWhenItsAStr = o.getClass().getComponentType() == String.class ? "\"" : "";
		int length = Array.getLength(o);
		for (int i = 0; i < length; i++)
			arraySt += quoteForWhenItsAStr + Array.get(o, i) + quoteForWhenItsAStr + (i < length-1 ? ", " : "");
		return " (length: " + length + ")" + arraySt + "}";
	}
}


class InspectorTools
{
	String parametersForItem(Member m)
	{
		Class[] params = {};
		if (m instanceof Constructor) params = ((Constructor)m).getParameterTypes();
		else if (m instanceof Method) params = ((Method)m).getParameterTypes();
		else {
			print("\n\nERROR: invalid parameter type passed to parametersForItem(<Member>) method\n\n");
			System.exit(0);
		}
		String paramSt = "";
		for(int i = 0; i < params.length; i++)
			paramSt += (simplifyTypeName(params[i]) + (i == params.length-1 ? "" : ", "));
		return paramSt;
	}
	String modifiersForItem(Member m)
	{
		String modifierSt = Modifier.toString(m.getModifiers());
		return " " + modifierSt + (modifierSt == "" ? "" : " ");
	}
	String simplifyTypeName(Class dataType)
	{
		if (dataType.isPrimitive())
			return dataType.getName();
		if (dataType.isArray())
			return simplifyArrayTypeName(dataType);
		return simplifiedClassName(dataType);
	}
	String simplifiedClassName(Class dataType) // strips off the fully-qualified (leading) part of class name and returns String version of that
	{
		String s = dataType.getName().substring(dataType.getName().lastIndexOf(".")+1);
		if (s.contains("[L"))
			s = s.substring(s.lastIndexOf("[L")+2);
		if (s.charAt(s.length()-1) == ';') // if array type
			return s.substring(0, s.length()-1);
		return s;
	}
	String simplifyArrayTypeName(Class dataType)
	{
		String typeSt = dataType.getName();
		int numDimensions = 0;
		for (int i = 0; i < typeSt.length(); i++){
			if (typeSt.charAt(i) == '[')  numDimensions++;
		}
		String arrayTypeSt = "" + (dataType.getComponentType().isPrimitive() ? dataType.getComponentType() : simplifiedClassName(dataType));
		arrayTypeSt += "[]".repeat(numDimensions);
		return arrayTypeSt;
	}
	void getAllClassesInHierarchyForClass(Class c, Set<Class> classesInObjHrchy)
	{
		if (c == null || classHasAlreadyBeenTraversed(c, classesInObjHrchy))
			return;
		// traverse up the inheritence hierarchy one level and repeat
		getAllClassesInHierarchyForClass(c.getSuperclass(), classesInObjHrchy);
		// get the interfaces:
		for (Class i : c.getInterfaces())
			getAllClassesInHierarchyForClass(i, classesInObjHrchy);
	}
	boolean classHasAlreadyBeenTraversed(Class c, Set<Class> classesInObjHrchy)
	{
		return ! classesInObjHrchy.add(c); // hashset .add() returns false if item is already present
	}
	boolean objectIsNull(Object obj)
	{
		if (obj == null) { 
			print(" (null object: nothing to inspect)\n");
			return true;
		}
		return false;
	}

	// a few basic helpers ---------------
	void print(String s){System.out.println(Inspector.INDENT.repeat(Inspector.indentMultiple) + s);}
	void print(){System.out.println();}
	void prnt(String s){System.out.print(s);} // no newline at end
	<T> T[] combineArrays(T[] first, T[] second)
	{
		T[] combined = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, combined, first.length, second.length);
		return combined;
	}
	String numberExtensionFor(int i)
	{
		String numStr = "" + i;
		char digitOfInterest = numStr.charAt(numStr.length()-1);
		switch (digitOfInterest){
			case '1': return "st";
			case '2': return "nd";
			case '3': return "rd";
			default: return "th";
		}
	}
}


public class Inspector
{
	InspectorTools t = new InspectorTools();
	public static final String INDENT = "        ";
	static int indentMultiple = 0;

	ClassInspector cI = new ClassInspector();
	MethodInspector mI = new MethodInspector();
	ConstructorInspector consI = new ConstructorInspector();
	FieldInspector fI = new FieldInspector();

	public Inspector(){
		printBanner();
	}
	public void printBanner()
	{
		t.print("\n\n==============================  OBJECT INSPECTOR  ==============================\n\n");
	}

	void inspect(Object obj, boolean recursive)
	{
		if (t.objectIsNull(obj))
			return;

		Set<Class> classesInObjHrchy = new HashSet<Class>();
		t.getAllClassesInHierarchyForClass(obj.getClass(), classesInObjHrchy);
		List<Field> objectFields = new ArrayList<Field>();

		t.print();
		cI.printClassInforForObject(obj);
		mI.printMethodsForObject(obj, classesInObjHrchy);
		consI.printConstructorsForObject(obj, classesInObjHrchy);
		fI.printFieldsForObject(obj, classesInObjHrchy, objectFields);
		t.print("\n");

		if (recursive)
			inspectObjectFields(obj, objectFields, recursive);
	}

	void inspectObjectFields(Object obj, List<Field> objectFields, boolean recursive)
	{
		if (objectFields.size() > 0){
			t.print(" >>>>>>> INSPECTING ^ THIS OBJECT'S FIELDS THAT ARE OBJECTS THEMSELVES:\n");
			
			indentMultiple++;
		}
		for (Field f : objectFields) {
			t.print(" Inspecting Field:  " + f.getName());
			t.print(" Reference value =  " + fI.getFieldValue(f, obj) + "\n");
			if (f.getType().isArray()) {
				Object[] array = (Object[])fI.getFieldValue(f, obj);
				for(int i = 0; i < array.length; i++){
					t.print(" Inspecting Field \"" + f.getName() + "\"'s " + i + 
						  t.numberExtensionFor(i) + " element:");
					
					inspect(array[i], recursive);
				}
			}
			else
				inspect(fI.getFieldValue(f, obj), recursive);
			t.print(" - - - - - - - - - - - - - -\n");
		}
		if (objectFields.size() > 0){
			indentMultiple--;
			t.print(" <<<<<<<<\n");
		}
	}

	public static void main (String[] arg){ 
		System.out.println();
	}
}