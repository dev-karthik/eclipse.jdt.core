/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.util;

/**
 * Description of attribute names as described in the JVM specifications.
 * 
 * This interface is not intended to be implemented by clients.
 * 
 * @since 2.0
 */
public interface IAttributeNamesConstants {
	char[] SYNTHETIC = new char[] {'S', 'y', 'n', 't', 'h', 'e', 't', 'i', 'c'};
	char[] CONSTANT_VALUE = new char[] {'C', 'o', 'n', 's', 't', 'a', 'n', 't', 'V', 'a', 'l', 'u', 'e'};
	char[] LINE_NUMBER= new char[] {'L', 'i', 'n', 'e', 'N', 'u', 'm', 'b', 'e', 'r', 'T', 'a', 'b', 'l', 'e'};
	char[] LOCAL_VARIABLE = new char[] {'L', 'o', 'c', 'a', 'l', 'V', 'a', 'r', 'i', 'a', 'b', 'l', 'e', 'T', 'a', 'b', 'l', 'e'};
	char[] INNER_CLASSES = new char[] {'I', 'n', 'n', 'e', 'r', 'C', 'l', 'a', 's', 's', 'e', 's'};
	char[] CODE = new char[] {'C', 'o', 'd', 'e'};
	char[] EXCEPTIONS = new char[] {'E', 'x', 'c', 'e', 'p', 't', 'i', 'o', 'n', 's'};
	char[] SOURCE = new char[] {'S', 'o', 'u', 'r', 'c', 'e', 'F', 'i', 'l', 'e'};
	char[] DEPRECATED = new char[] {'D', 'e', 'p', 'r', 'e', 'c', 'a', 't', 'e', 'd'};
	/**
	 * @since 3.1
	 */
	char[] SIGNATURE = new char[] {'S', 'i', 'g', 'n', 'a', 't', 'u', 'r', 'e'};
	/**
	 * @since 3.1
	 */
	char[] ENCLOSING_METHOD = new char[] {'E', 'n', 'c', 'l', 'o', 's', 'i', 'n', 'g', 'M', 'e', 't', 'h', 'o', 'd'};
	/**
	 * @since 3.1
	 */
	char[] LOCAL_VARIABLE_TYPE_TABLE = new char[] {'L', 'o', 'c', 'a', 'l', 'V', 'a', 'r', 'i', 'a', 'b', 'l', 'e', 'T', 'y', 'p', 'e', 'T', 'a', 'b', 'l', 'e'};
	/**
	 * @since 3.1
	 */
	char[] RUNTIME_VISIBLE_ANNOTATIONS= "RuntimeVisibleAnnotations".toCharArray(); //$NON-NLS-1$
	/**
	 * @since 3.1
	 */
	char[] RUNTIME_INVISIBLE_ANNOTATIONS= "RuntimeInvisibleAnnotations".toCharArray();//$NON-NLS-1$
	/**
	 * @since 3.1
	 */
	char[] RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS= "RuntimeVisibleParameterAnnotations".toCharArray();//$NON-NLS-1$
	/**
	 * @since 3.1
	 */
	char[] RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS= "RuntimeInvisibleParameterAnnotations".toCharArray();//$NON-NLS-1$
	/**
	 * @since 3.1
	 */
	char[] ANNOTATION_DEFAULT = "AnnotationDefault".toCharArray();//$NON-NLS-1$
}
