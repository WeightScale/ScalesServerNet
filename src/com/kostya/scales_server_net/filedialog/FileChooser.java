/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.kostya.scales_server_net.filedialog;

import android.content.Context;
import android.widget.LinearLayout;

/**
 * This interface defines all the methods that a file chooser must implement, in order to being able to make use of the class FileChooserUtils.
 */
interface FileChooser {

	/**
	 * Gets the root of the layout 'file_chooser.xml'.
	 * 
	 * @return A linear layout.
	 */
	LinearLayout getRootLayout();
	
	/**
	 * Set the name of the current folder.
	 * 
	 * @param name The current folder's name.
	 */
	void setCurrentFolderName(String name);
	
	/**
	 * Returns the current context of the file chooser.
	 * 
	 * @return The current context.
	 */
	Context getContext();
}
