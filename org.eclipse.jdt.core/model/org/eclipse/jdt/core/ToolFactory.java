/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.util.ClassFileBytesDisassembler;
import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.util.ClassFileReader;
import org.eclipse.jdt.internal.core.util.Disassembler;
import org.eclipse.jdt.internal.core.util.PublicScanner;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;

/**
 * Factory for creating various compiler tools, such as scanners, parsers and compilers.
 * <p>
 *  This class provides static methods only; it is not intended to be instantiated or subclassed by clients.
 * </p>
 * 
 * @since 2.0
 */
public class ToolFactory {

	/**
	 * Create an instance of a code formatter. A code formatter implementation can be contributed via the 
	 * extension point "org.eclipse.jdt.core.codeFormatter". If unable to find a registered extension, the factory 
	 * will default to using the default code formatter.
	 * 
	 * @return an instance of a code formatter
	 * @see ICodeFormatter
	 * @see ToolFactory#createDefaultCodeFormatter(Map)
	 * @deprecated - should use #createCodeFormatter(Map) instead. Extension point is discontinued
	 */
	public static ICodeFormatter createCodeFormatter(){
		
			Plugin jdtCorePlugin = JavaCore.getPlugin();
			if (jdtCorePlugin == null) return null;
		
			IExtensionPoint extension = jdtCorePlugin.getDescriptor().getExtensionPoint(JavaModelManager.FORMATTER_EXTPOINT_ID);
			if (extension != null) {
				IExtension[] extensions =  extension.getExtensions();
				for(int i = 0; i < extensions.length; i++){
					IConfigurationElement [] configElements = extensions[i].getConfigurationElements();
					for(int j = 0; j < configElements.length; j++){
						try {
							Object execExt = configElements[j].createExecutableExtension("class"); //$NON-NLS-1$
							if (execExt instanceof ICodeFormatter){
								// use first contribution found
								return (ICodeFormatter)execExt;
							}
						} catch(CoreException e){
							// unable to instantiate extension, will answer default formatter instead
						}
					}
				}	
			}
		// no proper contribution found, use default formatter			
		return createDefaultCodeFormatter(null);
	}

	/**
	 * Create an instance of the built-in code formatter. 
	 * @param options - the options map to use for formatting with the default code formatter. Recognized options
	 * 	are documented on <code>JavaCore#getDefaultOptions()</code>. If set to <code>null</code>, then use 
	 * 	the current settings from <code>JavaCore#getOptions</code>.
	 * @return an instance of the built-in code formatter
	 * @see CodeFormatter
	 * @see JavaCore#getOptions()
	 * @since 3.0
	 */
	public static CodeFormatter createCodeFormatter(Map options){
		if (options == null) options = JavaCore.getOptions();
		return new DefaultCodeFormatter(options);
	}

	/**
	 * Create a classfile bytecode disassembler, able to produce a String representation of a given classfile.
	 * 
	 * @return a classfile bytecode disassembler
	 * @see ClassFileBytesDisassembler
	 * @since 2.1
	 */
	public static ClassFileBytesDisassembler createDefaultClassFileBytesDisassembler(){
		return new Disassembler();
	}
	
	/**
	 * Create a classfile bytecode disassembler, able to produce a String representation of a given classfile.
	 * 
	 * @return a classfile bytecode disassembler
	 * @see org.eclipse.jdt.core.util.IClassFileDisassembler
	 * @deprecated - should use factory method creating ClassFileBytesDisassembler instead 
	 */
	public static org.eclipse.jdt.core.util.IClassFileDisassembler createDefaultClassFileDisassembler(){
		class DeprecatedDisassembler extends Disassembler implements org.eclipse.jdt.core.util.IClassFileDisassembler {
			// for backward compatibility, defines a disassembler which implements IClassFileDisassembler
		}
		return new DeprecatedDisassembler();
	}
	
	/**
	 * Create a classfile reader onto a classfile Java element.
	 * Create a default classfile reader, able to expose the internal representation of a given classfile
	 * according to the decoding flag used to initialize the reader.
	 * Answer null if the file named fileName doesn't represent a valid .class file.
	 * 
	 * The decoding flags are described in IClassFileReader.
	 * 
	 * @param classfile the classfile element to introspect
	 * @param decodingFlag the flag used to decode the class file reader.
	 * @return a default classfile reader
	 * 
	 * @see IClassFileReader
	 */
	public static IClassFileReader createDefaultClassFileReader(IClassFile classfile, int decodingFlag){

		IPackageFragmentRoot root = (IPackageFragmentRoot) classfile.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		if (root != null){
			try {
				if (root instanceof JarPackageFragmentRoot) {
					String archiveName = null;
					ZipFile jar = null;
					try {
						jar = ((JarPackageFragmentRoot)root).getJar();
						archiveName = jar.getName();
					} finally {
						JavaModelManager.getJavaModelManager().closeZipFile(jar);
					}
					PackageFragment packageFragment = (PackageFragment) classfile.getParent();
					String classFileName = classfile.getElementName();
					String entryName = org.eclipse.jdt.internal.core.util.Util.concatWith(packageFragment.names, classFileName, '/');
					return createDefaultClassFileReader(archiveName, entryName, decodingFlag);
				} else {
					InputStream in = null;
					try {
						in = ((IFile) classfile.getResource()).getContents();
						return createDefaultClassFileReader(in, decodingFlag);
					} finally {
						if (in != null)
							try {
								in.close();
							} catch (IOException e) {
								// ignore
							}
					}
				}
			} catch(CoreException e){
				// unable to read
			}
		}
		return null;
	}

	/**
	 * Create a default classfile reader, able to expose the internal representation of a given classfile
	 * according to the decoding flag used to initialize the reader.
	 * Answer null if the input stream contents cannot be retrieved
	 * 
	 * The decoding flags are described in IClassFileReader.
	 * 
	 * @param stream the given input stream to read
	 * @param decodingFlag the flag used to decode the class file reader.
	 * @return a default classfile reader
	 * 
	 * @see IClassFileReader
	 * @since 3.2
	 */
	public static IClassFileReader createDefaultClassFileReader(InputStream stream, int decodingFlag) {
		try {
			return new ClassFileReader(Util.getInputStreamAsByteArray(stream, -1), decodingFlag);
		} catch(ClassFormatException e) {
			return null;
		} catch(IOException e) {
			return null;
		}
	}
	
	/**
	 * Create a default classfile reader, able to expose the internal representation of a given classfile
	 * according to the decoding flag used to initialize the reader.
	 * Answer null if the file named fileName doesn't represent a valid .class file.
	 * The fileName has to be an absolute OS path to the given .class file.
	 * 
	 * The decoding flags are described in IClassFileReader.
	 * 
	 * @param fileName the name of the file to be read
	 * @param decodingFlag the flag used to decode the class file reader.
	 * @return a default classfile reader
	 * 
	 * @see IClassFileReader
	 */
	public static IClassFileReader createDefaultClassFileReader(String fileName, int decodingFlag){
		try {
			return new ClassFileReader(Util.getFileByteContent(new File(fileName)), decodingFlag);
		} catch(ClassFormatException e) {
			return null;
		} catch(IOException e) {
			return null;
		}
	}

	/**
	 * Create a default classfile reader, able to expose the internal representation of a given classfile
	 * according to the decoding flag used to initialize the reader.
	 * Answer null if the file named zipFileName doesn't represent a valid zip file or if the zipEntryName
	 * is not a valid entry name for the specified zip file or if the bytes don't represent a valid
	 * .class file according to the JVM specifications.
	 * 
	 * The decoding flags are described in IClassFileReader.
	 * 
	 * @param zipFileName the name of the zip file
	 * @param zipEntryName the name of the entry in the zip file to be read
	 * @param decodingFlag the flag used to decode the class file reader.
	 * @return a default classfile reader
	 * @see IClassFileReader
	 */
	public static IClassFileReader createDefaultClassFileReader(String zipFileName, String zipEntryName, int decodingFlag){
		ZipFile zipFile = null;
		try {
			if (JavaModelManager.ZIP_ACCESS_VERBOSE) {
				System.out.println("(" + Thread.currentThread() + ") [ToolFactory.createDefaultClassFileReader()] Creating ZipFile on " + zipFileName); //$NON-NLS-1$	//$NON-NLS-2$
			}
			zipFile = new ZipFile(zipFileName);
			ZipEntry zipEntry = zipFile.getEntry(zipEntryName);
			if (zipEntry == null) {
				return null;
			}
			if (!zipEntryName.toLowerCase().endsWith(SuffixConstants.SUFFIX_STRING_class)) {
				return null;
			}
			byte classFileBytes[] = Util.getZipEntryByteContent(zipEntry, zipFile);
			return new ClassFileReader(classFileBytes, decodingFlag);
		} catch(ClassFormatException e) {
			return null;
		} catch(IOException e) {
			return null;
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch(IOException e) {
					// ignore
				}
			}
		}
	}
	
	/**
	 * Create an instance of the built-in code formatter. A code formatter implementation can be contributed via the 
	 * extension point "org.eclipse.jdt.core.codeFormatter". If unable to find a registered extension, the factory will 
	 * default to using the default code formatter.
	 * 
	 * @param options - the options map to use for formatting with the default code formatter. Recognized options
	 * 	are documented on <code>JavaCore#getDefaultOptions()</code>. If set to <code>null</code>, then use 
	 * 	the current settings from <code>JavaCore#getOptions</code>.
	 * @return an instance of the built-in code formatter
	 * @see ICodeFormatter
	 * @see ToolFactory#createCodeFormatter()
	 * @see JavaCore#getOptions()
	 * @deprecated - use #createCodeFormatter(Map) instead
	 */
	public static ICodeFormatter createDefaultCodeFormatter(Map options){
		if (options == null) options = JavaCore.getOptions();
		return new org.eclipse.jdt.internal.formatter.old.CodeFormatter(options);
	}
	
	/**
	 * Create a scanner, indicating the level of detail requested for tokenizing. The scanner can then be
	 * used to tokenize some source in a Java aware way.
	 * Here is a typical scanning loop:
	 * 
	 * <code>
	 * <pre>
	 *   IScanner scanner = ToolFactory.createScanner(false, false, false, false);
	 *   scanner.setSource("int i = 0;".toCharArray());
	 *   while (true) {
	 *     int token = scanner.getNextToken();
	 *     if (token == ITerminalSymbols.TokenNameEOF) break;
	 *     System.out.println(token + " : " + new String(scanner.getCurrentTokenSource()));
	 *   }
	 * </pre>
	 * </code>
	 * 
	 * <p>
	 * The returned scanner will tolerate unterminated line comments (missing line separator). It can be made stricter
	 * by using API with extra boolean parameter (<code>strictCommentMode</code>).
	 * <p>
	 * @param tokenizeComments if set to <code>false</code>, comments will be silently consumed
	 * @param tokenizeWhiteSpace if set to <code>false</code>, white spaces will be silently consumed,
	 * @param assertMode if set to <code>false</code>, occurrences of 'assert' will be reported as identifiers
	 * (<code>ITerminalSymbols#TokenNameIdentifier</code>), whereas if set to <code>true</code>, it
	 * would report assert keywords (<code>ITerminalSymbols#TokenNameassert</code>). Java 1.4 has introduced
	 * a new 'assert' keyword.
	 * @param recordLineSeparator if set to <code>true</code>, the scanner will record positions of encountered line 
	 * separator ends. In case of multi-character line separators, the last character position is considered. These positions
	 * can then be extracted using <code>IScanner#getLineEnds</code>. Only non-unicode escape sequences are 
	 * considered as valid line separators.
  	 * @return a scanner
	 * @see org.eclipse.jdt.core.compiler.IScanner
	 */
	public static IScanner createScanner(boolean tokenizeComments, boolean tokenizeWhiteSpace, boolean assertMode, boolean recordLineSeparator){

		PublicScanner scanner = new PublicScanner(tokenizeComments, tokenizeWhiteSpace, false/*nls*/, assertMode ? ClassFileConstants.JDK1_4 : ClassFileConstants.JDK1_3/*sourceLevel*/, null/*taskTags*/, null/*taskPriorities*/, true/*taskCaseSensitive*/);
		scanner.recordLineSeparator = recordLineSeparator;
		return scanner;
	}
	
	/**
	 * Create a scanner, indicating the level of detail requested for tokenizing. The scanner can then be
	 * used to tokenize some source in a Java aware way.
	 * Here is a typical scanning loop:
	 * 
	 * <code>
	 * <pre>
	 *   IScanner scanner = ToolFactory.createScanner(false, false, false, false);
	 *   scanner.setSource("int i = 0;".toCharArray());
	 *   while (true) {
	 *     int token = scanner.getNextToken();
	 *     if (token == ITerminalSymbols.TokenNameEOF) break;
	 *     System.out.println(token + " : " + new String(scanner.getCurrentTokenSource()));
	 *   }
	 * </pre>
	 * </code>
	 * 
	 * <p>
	 * The returned scanner will tolerate unterminated line comments (missing line separator). It can be made stricter
	 * by using API with extra boolean parameter (<code>strictCommentMode</code>).
	 * <p>
	 * @param tokenizeComments if set to <code>false</code>, comments will be silently consumed
	 * @param tokenizeWhiteSpace if set to <code>false</code>, white spaces will be silently consumed,
	 * @param recordLineSeparator if set to <code>true</code>, the scanner will record positions of encountered line 
	 * separator ends. In case of multi-character line separators, the last character position is considered. These positions
	 * can then be extracted using <code>IScanner#getLineEnds</code>. Only non-unicode escape sequences are 
	 * considered as valid line separators.
	 * @param sourceLevel if set to <code>&quot;1.3&quot;</code> or <code>null</code>, occurrences of 'assert' will be reported as identifiers
	 * (<code>ITerminalSymbols#TokenNameIdentifier</code>), whereas if set to <code>&quot;1.4&quot;</code>, it
	 * would report assert keywords (<code>ITerminalSymbols#TokenNameassert</code>). Java 1.4 has introduced
	 * a new 'assert' keyword.
  	 * @return a scanner
	 * @see org.eclipse.jdt.core.compiler.IScanner
     * @since 3.0
	 */
	public static IScanner createScanner(boolean tokenizeComments, boolean tokenizeWhiteSpace, boolean recordLineSeparator, String sourceLevel) {
		PublicScanner scanner = null;
		long level = CompilerOptions.versionToJdkLevel(sourceLevel);
		if (level == 0) level = ClassFileConstants.JDK1_3; // fault-tolerance
		scanner = new PublicScanner(tokenizeComments, tokenizeWhiteSpace, false/*nls*/,level /*sourceLevel*/, null/*taskTags*/, null/*taskPriorities*/, true/*taskCaseSensitive*/);
		scanner.recordLineSeparator = recordLineSeparator;
		return scanner;
	}

	/**
	 * Create a scanner, indicating the level of detail requested for tokenizing. The scanner can then be
	 * used to tokenize some source in a Java aware way.
	 * Here is a typical scanning loop:
	 * 
	 * <code>
	 * <pre>
	 *   IScanner scanner = ToolFactory.createScanner(false, false, false, false);
	 *   scanner.setSource("int i = 0;".toCharArray());
	 *   while (true) {
	 *     int token = scanner.getNextToken();
	 *     if (token == ITerminalSymbols.TokenNameEOF) break;
	 *     System.out.println(token + " : " + new String(scanner.getCurrentTokenSource()));
	 *   }
	 * </pre>
	 * </code>
	 * 
	 * <p>
	 * The returned scanner will tolerate unterminated line comments (missing line separator). It can be made stricter
	 * by using API with extra boolean parameter (<code>strictCommentMode</code>).
	 * <p>
	 * @param tokenizeComments if set to <code>false</code>, comments will be silently consumed
	 * @param tokenizeWhiteSpace if set to <code>false</code>, white spaces will be silently consumed,
	 * @param recordLineSeparator if set to <code>true</code>, the scanner will record positions of encountered line 
	 * separator ends. In case of multi-character line separators, the last character position is considered. These positions
	 * can then be extracted using <code>IScanner#getLineEnds</code>. Only non-unicode escape sequences are 
	 * considered as valid line separators.
	 * @param sourceLevel if set to <code>&quot;1.3&quot;</code> or <code>null</code>, occurrences of 'assert' will be reported as identifiers
	 * (<code>ITerminalSymbols#TokenNameIdentifier</code>), whereas if set to <code>&quot;1.4&quot;</code>, it
	 * would report assert keywords (<code>ITerminalSymbols#TokenNameassert</code>). Java 1.4 has introduced
	 * a new 'assert' keyword.
	 * @param complianceLevel This is used to support the Unicode 4.0 character sets. if set to 1.5 or above,
	 * the Unicode 4.0 is supporte, otherwise Unicode 3.0 is supported.
  	 * @return a scanner
	 * @see org.eclipse.jdt.core.compiler.IScanner
	 *
     * @since 3.1
	 */
	public static IScanner createScanner(boolean tokenizeComments, boolean tokenizeWhiteSpace, boolean recordLineSeparator, String sourceLevel, String complianceLevel) {
		PublicScanner scanner = null;
		long sourceLevelValue = CompilerOptions.versionToJdkLevel(sourceLevel);
		if (sourceLevelValue == 0) sourceLevelValue = ClassFileConstants.JDK1_3; // fault-tolerance
		long complianceLevelValue = CompilerOptions.versionToJdkLevel(complianceLevel);
		if (complianceLevelValue == 0) complianceLevelValue = ClassFileConstants.JDK1_3; // fault-tolerance
		scanner = new PublicScanner(tokenizeComments, tokenizeWhiteSpace, false/*nls*/,sourceLevelValue /*sourceLevel*/, complianceLevelValue, null/*taskTags*/, null/*taskPriorities*/, true/*taskCaseSensitive*/);
		scanner.recordLineSeparator = recordLineSeparator;
		return scanner;
	}
}
