/*
 * @(#)Compiler.java                       
 * 
 * Revisions and updates (c) 2022-2024 Sandy Brownlee. alexander.brownlee@stir.ac.uk
 * 
 * Original release:
 *
 * Copyright (C) 1999, 2003 D.A. Watt and D.F. Brown
 * Dept. of Computing Science, University of Glasgow, Glasgow G12 8QQ Scotland
 * and School of Computer and Math Sciences, The Robert Gordon University,
 * St. Andrew Street, Aberdeen AB25 1HG, Scotland.
 * All rights reserved.
 *
 * This software is provided free for educational use only. It may
 * not be used for commercial purposes without the prior written permission
 * of the authors.
 */

 package triangle;

 import triangle.abstractSyntaxTrees.Program;
 import triangle.abstractSyntaxTrees.visitors.SummaryVisitor; // Import the SummaryVisitor
 import triangle.codeGenerator.Emitter;
 import triangle.codeGenerator.Encoder;
 import triangle.contextualAnalyzer.Checker;
 import triangle.optimiser.ConstantFolder;
 import triangle.syntacticAnalyzer.Parser;
 import triangle.syntacticAnalyzer.Scanner;
 import triangle.syntacticAnalyzer.SourceFile;
 import triangle.treeDrawer.Drawer;
 
 /**
  * The main driver class for the Triangle compiler.
  *
  * @version 2.1 7 Oct 2003
  * @author Deryck F. Brown
  */
 public class Compiler {
 
	 /** The filename for the object program, normally obj.tam. */
	 static String objectName = "obj.tam";
	 
	 static boolean showTree = false;
	 static boolean folding = false;
	 static boolean showTreeAfter = false; // New option for showing tree after folding
	 static boolean showStats = false; // New variable to control stats display
 
	 private static Scanner scanner;
	 private static Parser parser;
	 private static Checker checker;
	 private static Encoder encoder;
	 private static Emitter emitter;
	 private static ErrorReporter reporter;
	 private static Drawer drawer;
 
	 /** The AST representing the source program. */
	 private static Program theAST;
 
	 /**
	  * Compile the source program to TAM machine code.
	  *
	  * @param sourceName   the name of the file containing the source program.
	  * @param objectName   the name of the file containing the object program.
	  * @param showingAST   true iff the AST is to be displayed after contextual
	  *                     analysis
	  * @param showingTable true iff the object description details are to be
	  *                     displayed during code generation (not currently
	  *                     implemented).
	  * @return true iff the source program is free of compile-time errors, otherwise
	  *         false.
	  */
	 static boolean compileProgram(String sourceName, String objectName, boolean showingAST, boolean showingTable) {
 
		 System.out.println("********** " + "Triangle Compiler (Java Version 2.1)" + " **********");
 
		 System.out.println("Syntactic Analysis ...");
		 SourceFile source = SourceFile.ofPath(sourceName);
 
		 if (source == null) {
			 System.out.println("Can't access source file " + sourceName);
			 System.exit(1);
		 }
 
		 scanner = new Scanner(source);
		 reporter = new ErrorReporter(false);
		 parser = new Parser(scanner, reporter);
		 checker = new Checker(reporter);
		 emitter = new Emitter(reporter);
		 encoder = new Encoder(emitter, reporter);
		 drawer = new Drawer();
 
		 theAST = parser.parseProgram(); // 1st pass
		 if (reporter.getNumErrors() == 0) {
			 System.out.println("Contextual Analysis ...");
			 checker.check(theAST); // 2nd pass
			 if (showingAST) {
				 drawer.draw(theAST);
			 }
			 if (folding) {
				 theAST.visit(new ConstantFolder());
			 }
			 
			 // Integrate SummaryVisitor here if showStats is true
			 if (showStats) {
				 SummaryVisitor summaryVisitor = new SummaryVisitor();
				 theAST.visit(summaryVisitor, null); // Visit the AST to count nodes
				 summaryVisitor.getSummary(); // Print the summary
			 }
 
			 if (reporter.getNumErrors() == 0) {
				 System.out.println("Code Generation ...");
				 encoder.encodeRun(theAST, showingTable); // 3rd pass
			 }
		 }
 
		 boolean successful = (reporter.getNumErrors() == 0);
		 if (successful) {
			 emitter.saveObjectProgram(objectName);
			 System.out.println("Compilation was successful.");
			 if (showTreeAfter) {
				 drawer.draw(theAST); // Show the tree after folding if the option is set
			 }
		 } else {
  System.out.println("Compilation failed with " + reporter.getNumErrors() + " errors.");
		 }
		 return successful;
	 }
 
	 /**
	  * Parse command line arguments.
	  *
	  * @param args the command line arguments.
	  */
	 private static void parseCommandLineArguments(String[] args) {
		 for (String arg : args) {
			 if (arg.startsWith("-o=")) {
				 objectName = arg.substring(3);
			 } else if (arg.equals("tree")) {
				 showTree = true;
			 } else if (arg.equals("folding")) {
				 folding = true;
			 } else if (arg.equals("showTreeAfter")) {
				 showTreeAfter = true;
			 } else if (arg.equals("showStats")) { // New option for showing stats
				 showStats = true; // You need to declare this variable
			 }
		 }
	 }
 
	 /**
	  * The main method for the Triangle compiler.
	  *
	  * @param args command line arguments.
	  */
	 public static void main(String[] args) {
		 parseCommandLineArguments(args);
		 compileProgram("source.tri", objectName, showTree, false);
	 }
 }