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
 * Description of a parameter annotation as described in the JVM specifications.
 * 
 * This interface may be implemented by clients. 
 *  
 * @since 3.1
 */
public interface IParameterAnnotation {

	/**
	 * Answer back the number of annotations as described in the JVM specifications.
	 * 
	 * @return the number of annotations
	 */
	int getAnnotationsNumber();

	/**
	 * Answer back the annotations as described in the JVM specifications.
	 * Answers an empty collection if none.
	 * 
	 * @return the annotations
	 */
	IAnnotation[] getAnnotations();
}