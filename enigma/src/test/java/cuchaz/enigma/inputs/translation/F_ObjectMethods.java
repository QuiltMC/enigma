/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *	 Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.inputs.translation;

@SuppressWarnings("FinalizeCalledExplicitly")
public class F_ObjectMethods {
	public void callEmAll()
		throws Throwable {
		this.clone();
		this.equals(this);
		this.finalize();
		this.getClass();
		this.hashCode();
		this.notify();
		this.notifyAll();
		this.toString();
		this.wait();
		this.wait(0);
		this.wait(0, 0);
	}
}
